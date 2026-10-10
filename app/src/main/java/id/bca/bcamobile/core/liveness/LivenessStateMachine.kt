package id.bca.bcamobile.core.liveness

import kotlin.math.hypot

/** Tahapan mesin status liveness, sesuai spesifikasi Phase 2. */
enum class LivenessPhase {
    IDLE,
    CAMERA_READY,
    POSITIONING,
    CHALLENGE,
    CAPTURE,
    VERIFYING,
    SUCCESS,
    FAILED,
}

/**
 * Alasan gerbang kualitas belum terpenuhi — satu-satunya isi panduan di layar.
 *
 * Dipakai supaya layar bisa memberi arahan konkret ("Dekatkan wajah") alih-alih
 * membiarkan pengguna menebak kenapa tantangan tidak mulai.
 */
enum class LivenessGuidance {
    NONE,
    NO_FACE,
    MULTIPLE_FACES,
    MOVE_CLOSER,
    MOVE_AWAY,
    CENTER_FACE,
    LOOK_STRAIGHT,
    OPEN_EYES,
}

/** Kenapa satu percobaan liveness berakhir gagal di sisi client. */
enum class LivenessFailure {
    STEP_TIMEOUT,
    TOTAL_TIMEOUT,
    WRONG_MOVE,
    /** Ditolak server — satu-satunya kegagalan yang menentukan hasil akhir. */
    SERVER_REJECTED,

    /**
     * Frame bukti tidak lengkap, jadi payload tidak pernah dikirim.
     *
     * Dibedakan dari [SERVER_REJECTED] dengan sengaja: keduanya tampak sama di layar,
     * tapi di jejak masalah yang satu menunjuk ke perangkat dan yang lain ke server.
     */
    EVIDENCE_INCOMPLETE,
}

/** Frame bukti mana yang harus disimpan analyzer dari frame yang baru dinilai. */
sealed interface CaptureSlot {
    /** Frame netral terbaik, pembanding untuk face match di server. */
    data object Neutral : CaptureSlot

    data class Step(val index: Int, val action: LivenessAction) : CaptureSlot
}

/**
 * Keadaan mesin status yang boleh dibaca layar.
 *
 * Tidak memuat satu pun nilai sudut atau probabilitas: angka mentah hanya hidup di
 * dalam mesin dan di overlay debug. State yang dipersistensi tidak boleh membawa
 * jejak biometrik (aturan PII #5).
 */
data class LivenessState(
    val phase: LivenessPhase = LivenessPhase.IDLE,
    val challenge: LivenessChallenge? = null,
    val stepIndex: Int = 0,
    val guidance: LivenessGuidance = LivenessGuidance.NO_FACE,
    /** true saat gerakan sudah selesai tapi kepala belum kembali menghadap lurus. */
    val awaitingNeutral: Boolean = false,
    val failure: LivenessFailure? = null,
    val attempt: Int = 1,
) {
    val currentAction: LivenessAction?
        get() = if (awaitingNeutral) null else challenge?.actions?.getOrNull(stepIndex)

    val completedSteps: Int get() = stepIndex
    val totalSteps: Int get() = challenge?.stepCount ?: 0
}

/**
 * Hasil satu masukan ke mesin status.
 *
 * [capture] dan [needsNewChallenge] sengaja bukan bagian dari [state]: keduanya
 * sekali-pakai, dan menaruhnya di state membuat pemanggil harus menebak apakah
 * sebuah permintaan sudah dikerjakan atau belum.
 */
data class LivenessUpdate(
    val state: LivenessState,
    val capture: CaptureSlot? = null,
    val needsNewChallenge: Boolean = false,
    /** Frame bukti yang sudah terkumpul tidak lagi sah dan harus dihapus dari memori. */
    val discardFrames: Boolean = false,
)

/**
 * Mesin status tantangan liveness aktif.
 *
 * Seluruhnya Kotlin murni: tidak ada tipe Android, tidak ada ML Kit, tidak ada jam
 * sistem — waktu selalu datang dari [FaceSignals.timestampMillis] atau [onTick].
 * Itu yang membuat setiap aturan deteksi bisa diuji dengan angka sintetis.
 *
 * Mesin ini **tidak memutuskan lulus**. Langkah terakhir hanya membawanya ke
 * [LivenessPhase.CAPTURE]; yang menentukan hasil adalah [onServerVerdict] setelah
 * server memverifikasi nonce, frame, dan pose-nya sendiri. Dulu keputusan itu ada
 * di sini (`isComplete = true` lalu langsung unggah), dan itulah lubang utamanya.
 */
class LivenessStateMachine(private val config: LivenessConfig = LivenessConfig()) {

    private var state = LivenessState()

    private var readySince: Long? = null
    private var challengeRequested = false
    private var lockedTrackingId: Int? = null
    private var lastFaceSeenAt: Long? = null
    private var challengeStartedAt: Long? = null
    private var stepStartedAt: Long? = null
    private var holdFrames = 0
    private var wrongHoldFrames = 0
    private var wrongMoves = 0

    // Pelacak kedipan, hidup per langkah BLINK.
    private var blinkSawOpen = false
    private var blinkCloseStartedAt: Long? = null
    private var blinkSawClosed = false

    /**
     * Frame bukti kedipan diambil saat mata **terpejam**, bukan saat terbuka lagi.
     *
     * Dua alasan. Pertama, frame bermata terbuka di akhir kedipan nyaris identik
     * dengan frame netral, dan pemeriksaan "frame harus berbeda" di server akan
     * menolak nasabah yang jujur. Kedua, frame terpejam adalah satu-satunya frame
     * yang membuktikan kedipan itu terjadi — server bisa mengukur sendiri bahwa
     * mata di frame itu tertutup.
     */
    private var blinkCaptured = false
    private var captureBlinkFrame = false

    val current: LivenessState get() = state

    /** Kamera sudah terikat dan frame mulai mengalir. */
    fun onCameraReady(): LivenessUpdate {
        state = LivenessState(phase = LivenessPhase.POSITIONING, attempt = state.attempt)
        resetAttemptScopedState()
        return LivenessUpdate(state)
    }

    /** Tantangan dari server sudah diterima; belum berarti tantangan dimulai. */
    fun onChallengeIssued(challenge: LivenessChallenge): LivenessUpdate {
        challengeRequested = false
        state = state.copy(challenge = challenge, stepIndex = 0, failure = null)
        return LivenessUpdate(state)
    }

    /** Permintaan tantangan gagal; gerbang kualitas boleh meminta ulang. */
    fun onChallengeRequestFailed(): LivenessUpdate {
        challengeRequested = false
        readySince = null
        return LivenessUpdate(state)
    }

    fun onFrame(signals: FaceSignals): LivenessUpdate = when (state.phase) {
        LivenessPhase.POSITIONING -> position(signals)
        LivenessPhase.CHALLENGE -> challenge(signals)
        else -> LivenessUpdate(state)
    }

    /**
     * Kemajuan waktu tanpa frame baru.
     *
     * Perlu terpisah dari [onFrame] karena aliran frame bisa berhenti sama sekali —
     * aplikasi ke latar, kamera dilepas pemilik lain — dan batas waktu tetap harus
     * berjalan supaya tantangan tidak menggantung dengan nonce yang masih hidup.
     */
    fun onTick(nowMillis: Long): LivenessUpdate {
        if (state.phase != LivenessPhase.CHALLENGE) return LivenessUpdate(state)
        expiryOrTimeout(nowMillis)?.let { return it }
        val lostSince = lastFaceSeenAt
        if (lostSince != null && nowMillis - lostSince > config.faceLostGraceMillis) {
            return abortChallenge(LivenessGuidance.NO_FACE)
        }
        return LivenessUpdate(state)
    }

    /** Frame bukti sudah dikirim; menunggu keputusan server. */
    fun onSubmitted(): LivenessUpdate {
        if (state.phase != LivenessPhase.CAPTURE) return LivenessUpdate(state)
        state = state.copy(phase = LivenessPhase.VERIFYING)
        return LivenessUpdate(state)
    }

    /**
     * Satu-satunya tempat hasil akhir ditetapkan, dan nilainya datang dari server.
     *
     * Hanya berlaku saat mesin memang sedang menunggu jawaban. Tanpa penjagaan ini,
     * response yang datang terlambat — setelah tantangan dibatalkan dan percobaan
     * baru dimulai — akan memaksa SUCCESS atau FAILED di tengah percobaan yang
     * sedang berjalan.
     */
    fun onServerVerdict(passed: Boolean): LivenessUpdate {
        if (state.phase != LivenessPhase.VERIFYING && state.phase != LivenessPhase.CAPTURE) {
            return LivenessUpdate(state)
        }
        state = state.copy(
            phase = if (passed) LivenessPhase.SUCCESS else LivenessPhase.FAILED,
            failure = if (passed) null else LivenessFailure.SERVER_REJECTED,
        )
        return LivenessUpdate(state, discardFrames = true)
    }

    /**
     * Kegagalan yang diputuskan client sebelum apa pun dikirim.
     *
     * Tidak boleh dipakai untuk hasil verifikasi — itu milik [onServerVerdict].
     */
    fun failLocally(failure: LivenessFailure): LivenessUpdate {
        state = state.copy(phase = LivenessPhase.FAILED, failure = failure)
        return LivenessUpdate(state, discardFrames = true)
    }

    /**
     * Percobaan ulang setelah gagal.
     *
     * Tidak ada batas percobaan di sini: jumlah kegagalan dan masa tunggunya dihitung
     * server (keputusan Q6), dan client hanya menampilkan sisa waktu yang dibalas API.
     * Batas lokal kedua hanya akan menyimpang dari hitungan yang sebenarnya berlaku.
     */
    fun retry(): LivenessUpdate {
        state = LivenessState(
            phase = LivenessPhase.POSITIONING,
            attempt = state.attempt + 1,
        )
        resetAttemptScopedState()
        // Permintaan sudah ditandai di sini supaya gerbang kualitas tidak meminta
        // nonce kedua untuk percobaan yang sama.
        challengeRequested = true
        return LivenessUpdate(state, needsNewChallenge = true, discardFrames = true)
    }

    // -- POSITIONING -----------------------------------------------------------

    private fun position(signals: FaceSignals): LivenessUpdate {
        val guidance = evaluateGate(signals)
        if (guidance != LivenessGuidance.NONE) {
            readySince = null
            state = state.copy(guidance = guidance)
            return LivenessUpdate(state)
        }

        val now = signals.timestampMillis
        val since = readySince ?: now.also { readySince = it }
        state = state.copy(guidance = LivenessGuidance.NONE)
        if (now - since < config.readyHoldMillis) return LivenessUpdate(state)

        val challenge = state.challenge
        if (challenge == null) {
            // Nonce baru diminta setelah wajah benar-benar siap, bukan saat layar
            // dibuka: TTL-nya 60 detik dan memintanya lebih awal membuang waktu itu
            // untuk memposisikan wajah.
            if (challengeRequested) return LivenessUpdate(state)
            challengeRequested = true
            return LivenessUpdate(state, needsNewChallenge = true)
        }

        if (now >= challenge.expiresAtMillis) {
            return requestFreshChallenge()
        }

        return enterChallenge(signals)
    }

    private fun evaluateGate(signals: FaceSignals): LivenessGuidance {
        if (signals.faceCount == 0) return LivenessGuidance.NO_FACE
        if (signals.faceCount > 1) return LivenessGuidance.MULTIPLE_FACES
        val face = signals.normalized(config.calibration)
        if (face.faceWidthRatio < config.minFaceWidthRatio) return LivenessGuidance.MOVE_CLOSER
        if (face.faceWidthRatio > config.maxFaceWidthRatio) return LivenessGuidance.MOVE_AWAY
        val offset = hypot(
            (face.faceCenterX - config.ovalCenterX).toDouble(),
            (face.faceCenterY - config.ovalCenterY).toDouble(),
        ).toFloat()
        if (offset > config.maxCenterOffsetRatio) return LivenessGuidance.CENTER_FACE
        if (!face.isNeutralPose(config)) return LivenessGuidance.LOOK_STRAIGHT
        if (!face.areEyesOpen(config)) return LivenessGuidance.OPEN_EYES
        return LivenessGuidance.NONE
    }

    private fun enterChallenge(signals: FaceSignals): LivenessUpdate {
        val now = signals.timestampMillis
        lockedTrackingId = signals.trackingId
        lastFaceSeenAt = now
        challengeStartedAt = now
        stepStartedAt = now
        resetStepTrackers()
        wrongMoves = 0
        state = state.copy(
            phase = LivenessPhase.CHALLENGE,
            stepIndex = 0,
            guidance = LivenessGuidance.NONE,
            awaitingNeutral = false,
            failure = null,
        )
        // Frame netral terbaik diambil tepat saat gerbang kualitas terpenuhi — itu
        // frame dengan pose paling lurus yang akan ada sepanjang sesi.
        return LivenessUpdate(state, capture = CaptureSlot.Neutral)
    }

    // -- CHALLENGE -------------------------------------------------------------

    private fun challenge(signals: FaceSignals): LivenessUpdate {
        val now = signals.timestampMillis
        val challenge = state.challenge ?: return abortChallenge(LivenessGuidance.NO_FACE)

        expiryOrTimeout(now)?.let { return it }

        if (signals.faceCount > 1) return abortChallenge(LivenessGuidance.MULTIPLE_FACES)

        if (signals.faceCount == 0) {
            val lost = lastFaceSeenAt
            if (lost != null && now - lost > config.faceLostGraceMillis) {
                return abortChallenge(LivenessGuidance.NO_FACE)
            }
            holdFrames = 0
            wrongHoldFrames = 0
            state = state.copy(guidance = LivenessGuidance.NO_FACE)
            return LivenessUpdate(state)
        }

        // Orang yang sama harus bertahan sepanjang tantangan. Tanpa trackingId,
        // wajah bisa ditukar di tengah jalan dan setiap langkah dikerjakan orang lain.
        val locked = lockedTrackingId
        if (locked != null && signals.trackingId != null && signals.trackingId != locked) {
            return abortChallenge(LivenessGuidance.NO_FACE)
        }

        lastFaceSeenAt = now
        val face = signals.normalized(config.calibration)

        if (state.awaitingNeutral) {
            if (!face.isNeutralPose(config)) {
                state = state.copy(guidance = LivenessGuidance.LOOK_STRAIGHT)
                return LivenessUpdate(state)
            }
            stepStartedAt = now
            resetStepTrackers()
            state = state.copy(awaitingNeutral = false, guidance = LivenessGuidance.NONE)
            return LivenessUpdate(state)
        }

        val expected = challenge.actions.getOrNull(state.stepIndex)
            ?: return completeChallenge()

        state = state.copy(guidance = LivenessGuidance.NONE)

        val satisfied = if (expected == LivenessAction.BLINK) {
            trackBlink(face, now)
        } else {
            trackHeadPose(face, expected)
        }

        val capture = when {
            // Frame mata-terpejam: diambil sekarang, bukan di akhir langkah.
            captureBlinkFrame -> CaptureSlot.Step(state.stepIndex, LivenessAction.BLINK)
            // Kedipan yang frame-nya sudah diambil tidak diambil dua kali.
            satisfied && !(expected == LivenessAction.BLINK && blinkCaptured) ->
                CaptureSlot.Step(state.stepIndex, expected)
            else -> null
        }
        captureBlinkFrame = false

        // Langkah yang selesai menang atas hitungan gerakan salah di frame yang sama.
        // Urutan sebaliknya membuat gerakan yang BERHASIL bisa hangus — misalnya
        // nasabah berkedip tepat saat kepalanya masih sedikit menoleh — dan itu
        // kegagalan yang tidak bisa dijelaskan kepada siapa pun.
        if (satisfied) return completeStep(expected, now, capture)

        wrongMove(face, expected)?.let { return it }

        return LivenessUpdate(state, capture = capture)
    }

    /**
     * Batas waktu yang berlaku di seluruh fase CHALLENGE.
     *
     * Kedaluwarsanya tantangan diperlakukan berbeda dari habisnya waktu: nonce mati
     * bukan kesalahan pengguna, jadi yang terjadi adalah meminta tantangan baru, bukan
     * menghitungnya sebagai satu kegagalan.
     */
    private fun expiryOrTimeout(now: Long): LivenessUpdate? {
        val challenge = state.challenge ?: return null
        if (now >= challenge.expiresAtMillis) return requestFreshChallenge()
        challengeStartedAt?.let {
            if (now - it > config.totalTimeoutMillis) return fail(LivenessFailure.TOTAL_TIMEOUT)
        }
        stepStartedAt?.let {
            if (now - it > config.stepTimeoutMillis) return fail(LivenessFailure.STEP_TIMEOUT)
        }
        return null
    }

    private fun trackHeadPose(face: NormalizedFace, expected: LivenessAction): Boolean {
        if (expected in face.satisfiedHeadPose(config)) {
            holdFrames++
            return holdFrames >= config.holdFrames
        }
        holdFrames = 0
        return false
    }

    /**
     * Kedipan sebagai urutan buka → pejam → buka di dalam satu jendela waktu.
     *
     * Implementasi sebelumnya menerima "pernah terpejam, lalu terbuka" tanpa batas
     * waktu, jadi foto bermata terpejam yang disusul foto bermata terbuka lolos.
     * Jendela waktu dihitung sejak mata **mulai** menutup, bukan sejak frame terbuka
     * pertama — keadaan terbuka adalah keadaan diam yang bisa berlangsung semenit.
     */
    private fun trackBlink(face: NormalizedFace, now: Long): Boolean {
        blinkCloseStartedAt?.let {
            if (now - it > config.blinkWindowMillis) {
                blinkCloseStartedAt = null
                blinkSawClosed = false
            }
        }

        val open = face.areEyesOpen(config)
        val closed = face.areEyesClosed(config)

        when {
            open -> {
                if (blinkSawClosed && blinkCloseStartedAt != null) return true
                blinkSawOpen = true
                blinkCloseStartedAt = null
                blinkSawClosed = false
            }

            closed -> if (blinkSawOpen) {
                if (blinkCloseStartedAt == null) blinkCloseStartedAt = now
                blinkSawClosed = true
                if (!blinkCaptured) {
                    blinkCaptured = true
                    captureBlinkFrame = true
                }
            }

            // Di antara terbuka dan terpejam: jendela sudah mulai berjalan, tapi
            // belum ada bukti mata benar-benar menutup.
            else -> if (blinkSawOpen && blinkCloseStartedAt == null) blinkCloseStartedAt = now
        }
        return false
    }

    /**
     * Gerakan yang bukan yang diminta.
     *
     * Kedipan tidak pernah dihitung salah: mata berkedip sendiri beberapa kali per
     * menit, dan menghitungnya sebagai gerakan salah akan menggagalkan semua orang.
     * Yang dihitung hanya pose kepala yang jelas berbeda dari yang diminta.
     */
    private fun wrongMove(face: NormalizedFace, expected: LivenessAction): LivenessUpdate? {
        val satisfied = face.satisfiedHeadPose(config)
        if (satisfied.isEmpty() || expected in satisfied) {
            wrongHoldFrames = 0
            return null
        }
        wrongHoldFrames++
        if (wrongHoldFrames < config.holdFrames) return null
        wrongHoldFrames = 0
        wrongMoves++
        return if (wrongMoves >= config.maxWrongMoves) fail(LivenessFailure.WRONG_MOVE) else null
    }

    private fun completeStep(
        action: LivenessAction,
        now: Long,
        capture: CaptureSlot?,
    ): LivenessUpdate {
        val nextIndex = state.stepIndex + 1
        val total = state.challenge?.stepCount ?: nextIndex

        if (nextIndex >= total) {
            state = state.copy(stepIndex = nextIndex, phase = LivenessPhase.CAPTURE)
            return LivenessUpdate(state, capture = capture)
        }

        // Jatah waktu langkah dimulai ulang di sini supaya kembali ke netral tidak
        // memakai sisa waktu gerakan yang baru saja selesai.
        stepStartedAt = now
        resetStepTrackers()
        state = state.copy(
            stepIndex = nextIndex,
            awaitingNeutral = action.isHeadPose,
            guidance = if (action.isHeadPose) LivenessGuidance.LOOK_STRAIGHT else LivenessGuidance.NONE,
        )
        return LivenessUpdate(state, capture = capture)
    }

    private fun completeChallenge(): LivenessUpdate {
        state = state.copy(phase = LivenessPhase.CAPTURE)
        return LivenessUpdate(state)
    }

    private fun abortChallenge(guidance: LivenessGuidance): LivenessUpdate {
        state = LivenessState(
            phase = LivenessPhase.POSITIONING,
            guidance = guidance,
            attempt = state.attempt,
        )
        resetAttemptScopedState()
        challengeRequested = true
        // Frame yang sudah terkumpul milik nonce yang kini dibatalkan; menyimpannya
        // hanya membuka peluang dipakai ulang di tantangan berikutnya.
        return LivenessUpdate(state, needsNewChallenge = true, discardFrames = true)
    }

    private fun requestFreshChallenge(): LivenessUpdate = abortChallenge(LivenessGuidance.NONE)

    private fun fail(failure: LivenessFailure): LivenessUpdate {
        state = state.copy(phase = LivenessPhase.FAILED, failure = failure)
        return LivenessUpdate(state, discardFrames = true)
    }

    private fun resetAttemptScopedState() {
        readySince = null
        challengeRequested = false
        lockedTrackingId = null
        lastFaceSeenAt = null
        challengeStartedAt = null
        stepStartedAt = null
        wrongMoves = 0
        resetStepTrackers()
    }

    private fun resetStepTrackers() {
        holdFrames = 0
        wrongHoldFrames = 0
        blinkSawOpen = false
        blinkCloseStartedAt = null
        blinkSawClosed = false
        blinkCaptured = false
        captureBlinkFrame = false
    }
}
