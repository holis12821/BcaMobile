package id.bca.bcamobile.core.liveness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan deteksi liveness diuji dengan angka sintetis — tanpa kamera, tanpa ML Kit.
 *
 * Itulah alasan [LivenessStateMachine] tidak menerima `Face` milik ML Kit: setiap
 * kasus di bawah adalah skenario yang mustahil diuji ulang dengan tangan di depan
 * kamera, termasuk kedipan di luar jendela 600 ms dan wajah yang ditukar di tengah
 * tantangan.
 */
class LivenessStateMachineTest {

    // -- Gerbang kualitas (mulai otomatis, tanpa tombol) -----------------------

    @Test
    fun `tantangan mulai sendiri setelah gerbang kualitas bertahan`() {
        val machine = LivenessStateMachine()
        machine.onCameraReady()

        machine.onFrame(face(0))
        val requested = machine.onFrame(face(600))

        assertTrue("nonce diminta setelah wajah siap", requested.needsNewChallenge)
        assertEquals(LivenessPhase.POSITIONING, requested.state.phase)

        machine.onChallengeIssued(challengeOf(LivenessAction.BLINK))
        val started = machine.onFrame(face(700))

        assertEquals(LivenessPhase.CHALLENGE, started.state.phase)
        assertEquals(CaptureSlot.Neutral, started.capture)
    }

    @Test
    fun `gerbang belum bertahan cukup lama belum meminta nonce`() {
        val machine = LivenessStateMachine()
        machine.onCameraReady()

        machine.onFrame(face(0))
        val early = machine.onFrame(face(400))

        assertFalse(early.needsNewChallenge)
        assertEquals(LivenessGuidance.NONE, early.state.guidance)
    }

    @Test
    fun `nonce hanya diminta sekali selama gerbang terpenuhi`() {
        val machine = LivenessStateMachine()
        machine.onCameraReady()
        machine.onFrame(face(0))

        assertTrue(machine.onFrame(face(600)).needsNewChallenge)
        assertFalse("permintaan kedua untuk gerbang yang sama", machine.onFrame(face(700)).needsNewChallenge)
    }

    @Test
    fun `panduan menyebut alasan gerbang tidak terpenuhi`() {
        val machine = LivenessStateMachine()
        machine.onCameraReady()

        assertEquals(LivenessGuidance.NO_FACE, machine.onFrame(face(0, faces = 0)).state.guidance)
        assertEquals(LivenessGuidance.MULTIPLE_FACES, machine.onFrame(face(0, faces = 2)).state.guidance)
        assertEquals(LivenessGuidance.MOVE_CLOSER, machine.onFrame(face(0, width = 0.2f)).state.guidance)
        assertEquals(LivenessGuidance.MOVE_AWAY, machine.onFrame(face(0, width = 0.9f)).state.guidance)
        assertEquals(LivenessGuidance.CENTER_FACE, machine.onFrame(face(0, cx = 0.85f)).state.guidance)
        assertEquals(LivenessGuidance.LOOK_STRAIGHT, machine.onFrame(face(0, yaw = 30f)).state.guidance)
        assertEquals(LivenessGuidance.OPEN_EYES, machine.onFrame(face(0, eye = 0.1f)).state.guidance)
        assertEquals(LivenessGuidance.NONE, machine.onFrame(face(0)).state.guidance)
    }

    @Test
    fun `gerbang terputus mengulang hitungan tahan`() {
        val machine = LivenessStateMachine()
        machine.onCameraReady()
        machine.onFrame(face(0))
        machine.onFrame(face(300, faces = 0))

        assertFalse("hitungan tahan harus mulai dari nol", machine.onFrame(face(600)).needsNewChallenge)
    }

    // -- Deteksi gerakan -------------------------------------------------------

    @Test
    fun `gerakan kepala butuh beberapa frame berurutan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val single = machine.onFrame(face(800, yaw = 30f))
        assertNull("satu frame tidak boleh meloloskan langkah", single.capture)

        machine.onFrame(face(900, yaw = 30f))
        val third = machine.onFrame(face(1000, yaw = 30f))

        assertEquals(CaptureSlot.Step(0, LivenessAction.TURN_LEFT), third.capture)
    }

    @Test
    fun `frame tidak berurutan mengulang hitungan tahan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        machine.onFrame(face(800, yaw = 30f))
        machine.onFrame(face(900, yaw = 30f))
        machine.onFrame(face(1000, yaw = 0f))
        machine.onFrame(face(1100, yaw = 30f))
        val second = machine.onFrame(face(1200, yaw = 30f))

        assertNull("hitungan tahan harus dimulai ulang", second.capture)
    }

    @Test
    fun `arah menoleh mengikuti kalibrasi tanda`() {
        val flipped = LivenessConfig(
            calibration = LivenessCalibration(yawSignForUserLeft = -1f),
        )
        val machine = LivenessStateMachine(flipped)
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        machine.onFrame(face(800, yaw = -30f))
        machine.onFrame(face(900, yaw = -30f))
        val done = machine.onFrame(face(1000, yaw = -30f))

        assertEquals(CaptureSlot.Step(0, LivenessAction.TURN_LEFT), done.capture)
    }

    @Test
    fun `menengadah dan menunduk dibedakan`() {
        val up = LivenessStateMachine()
        up.reachChallenge(challengeOf(LivenessAction.LOOK_UP))
        repeat(2) { up.onFrame(face(800 + it * 100L, pitch = 25f)) }
        assertEquals(
            CaptureSlot.Step(0, LivenessAction.LOOK_UP),
            up.onFrame(face(1000, pitch = 25f)).capture,
        )

        val down = LivenessStateMachine()
        down.reachChallenge(challengeOf(LivenessAction.LOOK_DOWN))
        repeat(2) { down.onFrame(face(800 + it * 100L, pitch = -20f)) }
        assertEquals(
            CaptureSlot.Step(0, LivenessAction.LOOK_DOWN),
            down.onFrame(face(1000, pitch = -20f)).capture,
        )
    }

    // -- Kedipan ---------------------------------------------------------------

    @Test
    fun `kedipan butuh urutan buka pejam buka`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK))

        machine.onFrame(face(800, eye = 0.95f))
        // Frame bukti diambil di sini, saat mata terpejam: frame inilah yang
        // membuktikan kedipan terjadi, dan satu-satunya yang jelas berbeda dari
        // frame netral.
        val closed = machine.onFrame(face(900, eye = 0.05f))
        assertEquals(CaptureSlot.Step(0, LivenessAction.BLINK), closed.capture)
        assertEquals(LivenessPhase.CHALLENGE, closed.state.phase)
        assertEquals(0, closed.state.completedSteps)

        val blinked = machine.onFrame(face(1000, eye = 0.95f))
        assertEquals(1, blinked.state.completedSteps)
        assertNull("frame bukti tidak diambil dua kali", blinked.capture)
    }

    @Test
    fun `mata dipejamkan terus tidak lolos lalu kehabisan waktu`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK))

        machine.onFrame(face(800, eye = 0.95f))
        var time = 900L
        while (time < 8_000L) {
            val update = machine.onFrame(face(time, eye = 0.02f))
            // Frame bukti boleh terambil — itu bukan kelulusan. Yang tidak boleh
            // terjadi adalah langkahnya maju tanpa mata terbuka lagi.
            assertEquals(
                "mata terpejam saja tidak boleh meloloskan kedipan",
                0,
                update.state.completedSteps,
            )
            time += 200L
        }

        val timedOut = machine.onFrame(face(9_000, eye = 0.02f))
        assertEquals(LivenessPhase.FAILED, timedOut.state.phase)
        assertEquals(LivenessFailure.STEP_TIMEOUT, timedOut.state.failure)
    }

    @Test
    fun `mata terbuka lagi di luar jendela tidak dihitung kedipan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK))

        machine.onFrame(face(800, eye = 0.95f))
        machine.onFrame(face(900, eye = 0.05f))
        val tooLate = machine.onFrame(face(1_700, eye = 0.95f))

        assertNull("jendela 600 ms sudah lewat", tooLate.capture)
    }

    @Test
    fun `kedipan tidak pernah dihitung sebagai gerakan salah`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        var time = 800L
        repeat(12) {
            machine.onFrame(face(time, eye = 0.95f))
            machine.onFrame(face(time + 60, eye = 0.02f))
            machine.onFrame(face(time + 120, eye = 0.95f))
            time += 200L
        }

        assertEquals(LivenessPhase.CHALLENGE, machine.current.phase)
        assertNull(machine.current.failure)
    }

    // -- Urutan, gerakan salah, dan kembali ke netral --------------------------

    @Test
    fun `urutan server diikuti dan langkah berikutnya belum aktif`() {
        val machine = LivenessStateMachine()
        val challenge = challengeOf(
            LivenessAction.LOOK_UP,
            LivenessAction.BLINK,
            LivenessAction.TURN_RIGHT,
        )
        machine.reachChallenge(challenge)

        assertEquals(LivenessAction.LOOK_UP, machine.current.currentAction)

        // Gerakan langkah ketiga dikerjakan lebih awal: tidak boleh memajukan apa pun.
        repeat(3) { machine.onFrame(face(800 + it * 100L, yaw = -30f)) }
        assertEquals(0, machine.current.completedSteps)
    }

    @Test
    fun `gerakan salah berulang menggagalkan tantangan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        // Tiga gerakan salah, masing-masing butuh holdFrames frame berurutan. Yang
        // diperiksa adalah update yang menggagalkan, bukan frame setelahnya: frame
        // sesudah FAILED tidak melakukan apa pun lagi.
        var time = 800L
        var last = machine.onFrame(face(time, yaw = -30f))
        while (last.state.phase == LivenessPhase.CHALLENGE && time < 7_000L) {
            time += 100L
            last = machine.onFrame(face(time, yaw = -30f))
        }

        assertEquals(LivenessPhase.FAILED, last.state.phase)
        assertEquals(LivenessFailure.WRONG_MOVE, last.state.failure)
        assertTrue("frame bukti harus dibuang", last.discardFrames)
    }

    @Test
    fun `kepala wajib kembali netral sebelum langkah berikutnya`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT, LivenessAction.LOOK_UP))

        repeat(3) { machine.onFrame(face(800 + it * 100L, yaw = 30f)) }
        assertTrue(machine.current.awaitingNeutral)
        assertNull("tidak ada aksi aktif sebelum netral", machine.current.currentAction)

        // Langsung menengadah dari posisi menoleh tidak boleh dihitung.
        repeat(3) { machine.onFrame(face(1_100 + it * 100L, yaw = 30f, pitch = 25f)) }
        assertEquals(1, machine.current.completedSteps)
        assertTrue(machine.current.awaitingNeutral)

        machine.onFrame(face(1_500))
        assertFalse(machine.current.awaitingNeutral)
        assertEquals(LivenessAction.LOOK_UP, machine.current.currentAction)

        repeat(2) { machine.onFrame(face(1_600 + it * 100L, pitch = 25f)) }
        val done = machine.onFrame(face(1_800, pitch = 25f))
        assertEquals(CaptureSlot.Step(1, LivenessAction.LOOK_UP), done.capture)
    }

    @Test
    fun `kedipan tidak menuntut kembali ke netral`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK, LivenessAction.TURN_LEFT))

        machine.onFrame(face(800, eye = 0.95f))
        machine.onFrame(face(900, eye = 0.05f))
        machine.onFrame(face(1_000, eye = 0.95f))

        assertFalse(machine.current.awaitingNeutral)
        assertEquals(LivenessAction.TURN_LEFT, machine.current.currentAction)
    }

    // -- Kesinambungan orang dan wajah hilang ----------------------------------

    @Test
    fun `wajah kedua membatalkan tantangan dan meminta nonce baru`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val aborted = machine.onFrame(face(800, faces = 2))

        assertEquals(LivenessPhase.POSITIONING, aborted.state.phase)
        assertEquals(LivenessGuidance.MULTIPLE_FACES, aborted.state.guidance)
        assertTrue(aborted.needsNewChallenge)
        assertTrue(aborted.discardFrames)
        assertNull("bukan kegagalan, hanya dibatalkan", aborted.state.failure)
    }

    @Test
    fun `trackingId berubah membatalkan tantangan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        repeat(2) { machine.onFrame(face(800 + it * 100L, yaw = 30f)) }
        val swapped = machine.onFrame(face(1_000, yaw = 30f, trackingId = 99))

        assertEquals(LivenessPhase.POSITIONING, swapped.state.phase)
        assertTrue(swapped.needsNewChallenge)
        assertTrue(swapped.discardFrames)
    }

    @Test
    fun `wajah hilang sebentar masih ditoleransi`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val blip = machine.onFrame(face(1_200, faces = 0))

        assertEquals(LivenessPhase.CHALLENGE, blip.state.phase)
        assertEquals(LivenessGuidance.NO_FACE, blip.state.guidance)
    }

    @Test
    fun `wajah hilang melewati tenggang membatalkan tantangan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val gone = machine.onFrame(face(1_800, faces = 0))

        assertEquals(LivenessPhase.POSITIONING, gone.state.phase)
        assertTrue(gone.needsNewChallenge)
        assertTrue(gone.discardFrames)
    }

    @Test
    fun `waktu berjalan tanpa frame tetap membatalkan tantangan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val ticked = machine.onTick(1_800)

        assertEquals(LivenessPhase.POSITIONING, ticked.state.phase)
        assertTrue(ticked.needsNewChallenge)
    }

    // -- Batas waktu dan kedaluwarsa -------------------------------------------

    @Test
    fun `langkah kehabisan waktu menggagalkan tantangan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))

        val timedOut = machine.onFrame(face(9_000))

        assertEquals(LivenessPhase.FAILED, timedOut.state.phase)
        assertEquals(LivenessFailure.STEP_TIMEOUT, timedOut.state.failure)
    }

    @Test
    fun `total waktu habis menggagalkan tantangan`() {
        // Batas per-langkah dilebarkan supaya yang diuji benar-benar batas total.
        val config = LivenessConfig(stepTimeoutMillis = 60_000L, totalTimeoutMillis = 5_000L)
        val machine = LivenessStateMachine(config)
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT), expiresAt = 120_000L)

        val timedOut = machine.onFrame(face(6_000))

        assertEquals(LivenessPhase.FAILED, timedOut.state.phase)
        assertEquals(LivenessFailure.TOTAL_TIMEOUT, timedOut.state.failure)
    }

    @Test
    fun `tantangan kedaluwarsa meminta nonce baru dan bukan kegagalan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT), expiresAt = 1_000L)

        val expired = machine.onFrame(face(1_500))

        assertEquals(LivenessPhase.POSITIONING, expired.state.phase)
        assertTrue(expired.needsNewChallenge)
        assertTrue(expired.discardFrames)
        assertNull(expired.state.failure)
    }

    // -- Keputusan akhir milik server ------------------------------------------

    @Test
    fun `seluruh langkah selesai berhenti di CAPTURE bukan SUCCESS`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK, LivenessAction.TURN_LEFT))

        machine.onFrame(face(800, eye = 0.95f))
        machine.onFrame(face(900, eye = 0.05f))
        machine.onFrame(face(1_000, eye = 0.95f))
        repeat(2) { machine.onFrame(face(1_100 + it * 100L, yaw = 30f)) }
        val last = machine.onFrame(face(1_300, yaw = 30f))

        assertEquals(LivenessPhase.CAPTURE, last.state.phase)
        assertEquals(2, last.state.completedSteps)
        assertEquals(CaptureSlot.Step(1, LivenessAction.TURN_LEFT), last.capture)
        assertNull(last.state.failure)
    }

    @Test
    fun `hasil akhir hanya datang dari server`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.BLINK))
        machine.onFrame(face(800, eye = 0.95f))
        machine.onFrame(face(900, eye = 0.05f))
        machine.onFrame(face(1_000, eye = 0.95f))

        assertEquals(LivenessPhase.CAPTURE, machine.current.phase)
        assertEquals(LivenessPhase.VERIFYING, machine.onSubmitted().state.phase)

        val rejected = machine.onServerVerdict(passed = false)
        assertEquals(LivenessPhase.FAILED, rejected.state.phase)
        assertEquals(LivenessFailure.SERVER_REJECTED, rejected.state.failure)
        assertTrue(rejected.discardFrames)
    }

    @Test
    fun `percobaan ulang meminta nonce baru dan menaikkan hitungan percobaan`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))
        machine.onFrame(face(9_000))

        val retried = machine.retry()

        assertEquals(LivenessPhase.POSITIONING, retried.state.phase)
        assertEquals(2, retried.state.attempt)
        assertTrue(retried.needsNewChallenge)
        assertTrue(retried.discardFrames)
        assertNull(retried.state.failure)
    }

    @Test
    fun `frame setelah gagal tidak memajukan apa pun`() {
        val machine = LivenessStateMachine()
        machine.reachChallenge(challengeOf(LivenessAction.TURN_LEFT))
        machine.onFrame(face(9_000))

        val ignored = machine.onFrame(face(9_100, yaw = 30f))

        assertEquals(LivenessPhase.FAILED, ignored.state.phase)
        assertNull(ignored.capture)
    }

    // -- Pembantu --------------------------------------------------------------

    private fun LivenessStateMachine.reachChallenge(
        challenge: LivenessChallenge,
        expiresAt: Long = 60_000L,
    ) {
        onCameraReady()
        onFrame(face(0))
        onFrame(face(600))
        onChallengeIssued(challenge.copy(expiresAtMillis = expiresAt))
        onFrame(face(700))
    }

    private fun challengeOf(vararg actions: LivenessAction) = LivenessChallenge(
        challengeId = "chl_test",
        nonce = "nonce_test",
        actions = actions.toList(),
        expiresAtMillis = 60_000L,
    )

    private fun face(
        timestampMillis: Long,
        yaw: Float = 0f,
        pitch: Float = 0f,
        roll: Float = 0f,
        eye: Float = 0.95f,
        width: Float = 0.5f,
        cx: Float = 0.5f,
        cy: Float = 0.5f,
        faces: Int = 1,
        trackingId: Int? = 7,
    ) = FaceSignals(
        timestampMillis = timestampMillis,
        faceCount = faces,
        trackingId = trackingId,
        yawDegrees = yaw,
        pitchDegrees = pitch,
        rollDegrees = roll,
        leftEyeOpenProbability = eye,
        rightEyeOpenProbability = eye,
        faceWidthRatio = width,
        faceCenterX = cx,
        faceCenterY = cy,
    )
}
