package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.liveness.CaptureSlot
import id.bca.bcamobile.core.liveness.FaceSignals
import id.bca.bcamobile.core.liveness.LivenessAction
import id.bca.bcamobile.core.liveness.LivenessChallenge
import id.bca.bcamobile.core.liveness.LivenessFailure
import id.bca.bcamobile.core.liveness.LivenessFrame
import id.bca.bcamobile.core.liveness.LivenessPayload
import id.bca.bcamobile.core.liveness.LivenessPhase
import id.bca.bcamobile.core.liveness.LivenessStateMachine
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.security.DevicePublicKey
import id.bca.bcamobile.core.security.DeviceRiskSignals
import id.bca.bcamobile.core.security.LivenessAttestor
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.BukaRekeningVerifikasiBiometrikViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.VerifikasiBiometrikEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Perilaku pengiriman bukti liveness.
 *
 * Yang diuji terutama apa yang **tidak** boleh terjadi: maju ke langkah berikutnya
 * tanpa persetujuan server, mengirim payload tanpa tanda tangan, menghitung masa tunggu
 * sendiri, dan membiarkan piksel wajah hidup setelah unggahan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningLivenessViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeOnboardingRepository
    private lateinit var store: BukaRekeningSessionStore
    private lateinit var attestor: FakeAttestor

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeOnboardingRepository()
        store = BukaRekeningSessionStore()
        attestor = FakeAttestor()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `tantangan diminta dari server dan dipakai apa adanya`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        val viewModel = viewModel()

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()

        assertEquals(1, repository.livenessChallengeCount)
        assertEquals(
            listOf(LivenessAction.BLINK, LivenessAction.TURN_LEFT, LivenessAction.LOOK_UP),
            viewModel.machine.current.challenge?.actions,
        )
    }

    @Test
    fun `tantangan tanpa aksi yang dikenal tidak dipakai`() = runTest(dispatcher) {
        repository.livenessChallengeResult =
            DataResult.Success(challenge().copy(actions = emptyList()))
        val viewModel = viewModel()

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()

        assertEquals(null, viewModel.machine.current.challenge)
    }

    @Test
    fun `hanya persetujuan server yang memajukan langkah`() = runTest(dispatcher) {
        val effects = mutableListOf<BukaRekeningSideEffect>()
        backgroundScope.launch { store.sideEffect.collect { effects += it } }

        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))
        val viewModel = viewModel()

        submitOnce(viewModel)

        assertEquals(LivenessPhase.SUCCESS, viewModel.machine.current.phase)
        assertTrue(
            effects.any {
                it is BukaRekeningSideEffect.AdvanceTo && it.step == OnboardingStep.VIDEO_CALL
            },
        )
    }

    @Test
    fun `server menolak berarti gagal dan tidak ada langkah maju`() = runTest(dispatcher) {
        val effects = mutableListOf<BukaRekeningSideEffect>()
        backgroundScope.launch { store.sideEffect.collect { effects += it } }

        repository.livenessChallengeResult = DataResult.Success(challenge())
        // Perhatikan: panggilannya BERHASIL, tapi verdict-nya tidak lulus. Inilah
        // kasus yang dulu tidak pernah diperiksa — client menganggap 200 sebagai lulus.
        repository.submitLivenessResult = DataResult.Success(verdict(verified = false))
        val viewModel = viewModel()

        submitOnce(viewModel)

        assertEquals(LivenessPhase.FAILED, viewModel.machine.current.phase)
        assertFalse(effects.any { it is BukaRekeningSideEffect.AdvanceTo })
    }

    @Test
    fun `payload tanpa tanda tangan tidak pernah dikirim`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        attestor.signature = null
        val viewModel = viewModel()

        submitOnce(viewModel)

        assertTrue("tidak boleh ada pengiriman", repository.submittedLiveness.isEmpty())
        assertEquals(LivenessPhase.FAILED, viewModel.machine.current.phase)
    }

    @Test
    fun `payload yang ditandatangani menutupi isi setiap frame`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))
        val viewModel = viewModel()

        submitOnce(viewModel)

        val signed = attestor.signedPayloads.single()
        assertTrue(signed.startsWith(LivenessPayload.VERSION))
        assertTrue(signed.contains("nonce=nonce_1"))
        assertTrue(signed.contains("device_id=device_1"))
        // Digest, bukan hanya metadata: tanpa ini payload yang sah bisa dipasangkan
        // ulang dengan frame lain dan tanda tangannya tetap cocok.
        assertTrue(signed.contains(LivenessPayload.sha256Hex(byteArrayOf(9, 9, 9))))
    }

    @Test
    fun `token integritas diminta dengan nonce tantangan`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))
        val viewModel = viewModel()

        submitOnce(viewModel)

        assertEquals(listOf("nonce_1"), attestor.integrityNonces)
        assertEquals("integrity_token", repository.submittedLiveness.single().integrityToken)
    }

    @Test
    fun `frame ditimpa nol setelah pengiriman`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))
        val viewModel = viewModel()

        val frames = frames()
        submitOnce(viewModel, frames)

        frames.forEach {
            assertArrayEquals(
                "piksel wajah tidak boleh tertinggal di heap",
                ByteArray(it.jpeg.size),
                it.jpeg,
            )
        }
    }

    @Test
    fun `masa tunggu diambil dari server dan dihitung turun`() = runTest(dispatcher) {
        repository.livenessChallengeResult =
            DataResult.Failure(ApiFailure.RateLimited(retryAfterSeconds = 300))
        val viewModel = viewModel()

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()
        assertEquals(300, store.current.livenessCooldownSeconds)

        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(290, store.current.livenessCooldownSeconds)
    }

    @Test
    fun `liveness yang diblokir tidak meminta tantangan lagi`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Failure(
            ApiFailure.Business("LIVENESS_BLOCKED", ""),
        )
        val viewModel = viewModel()

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()
        assertTrue(store.current.isLivenessBlocked)

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()
        assertEquals("permintaan kedua harus ditahan", 1, repository.livenessChallengeCount)
    }

    /**
     * Percobaan kedua setelah gagal harus benar-benar terkirim.
     *
     * Regresi: `LivenessFaceAnalyzer.submitted` dulu hanya diset sekali seumur hidup
     * analyzer, dan analyzer hidup selama controller kamera tidak berubah — jadi
     * melewati percobaan ulang. Akibatnya percobaan kedua yang BERHASIL tidak pernah
     * dikirim, dan layar menggantung tanpa penjelasan. Test ini menjaga jalur itu dari
     * sisi ViewModel.
     */
    @Test
    fun `percobaan kedua setelah gagal tetap terkirim`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = false))
        val viewModel = viewModel()

        submitOnce(viewModel)
        assertEquals(1, repository.submittedLiveness.size)
        assertEquals(LivenessPhase.FAILED, viewModel.machine.current.phase)

        // Nonce baru untuk percobaan kedua, lalu seluruh siklus dijalankan ulang.
        repository.livenessChallengeResult =
            DataResult.Success(challenge().copy(challengeId = "chl_2", nonce = "nonce_2"))
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))

        viewModel.onEvent(VerifikasiBiometrikEvent.RetryRequested)
        runCurrent()
        submitOnce(viewModel)

        assertEquals("percobaan kedua harus ikut terkirim", 2, repository.submittedLiveness.size)
        assertEquals("nonce_2", repository.submittedLiveness.last().nonce)
        assertEquals(LivenessPhase.SUCCESS, viewModel.machine.current.phase)
        assertEquals(2, viewModel.machine.current.attempt)
    }

    /**
     * Frame langkah dikirim terurut indeks.
     *
     * Server memasangkan `liveness_frames[i]` dengan `step_meta[i]` lalu menuntut
     * `step.Index == i`. Urutan yang salah tetap lolos verifikasi tanda tangan —
     * payloadnya diurutkan indeks — lalu ditolak sebagai `step_index_mismatch`,
     * kegagalan yang sulit dilacak dari sisi client.
     */
    @Test
    fun `frame langkah dikirim terurut indeks`() = runTest(dispatcher) {
        repository.livenessChallengeResult = DataResult.Success(challenge())
        repository.submitLivenessResult = DataResult.Success(verdict(verified = true))
        val viewModel = viewModel()

        // Buffer sengaja diserahkan dalam urutan terbalik.
        submitOnce(viewModel, frames().reversed())

        val sent = repository.submittedLiveness.single().stepFrames
        assertEquals(listOf(0, 1, 2), sent.map { it.index })
    }

    /** Bukti yang tidak lengkap bukan penolakan server, dan tidak boleh dikirim. */
    @Test
    fun `bukti tidak lengkap tidak dikirim dan tidak disebut penolakan server`() =
        runTest(dispatcher) {
            repository.livenessChallengeResult = DataResult.Success(challenge())
            val viewModel = viewModel()

            submitOnce(viewModel, frames().filterNot { it.slot is CaptureSlot.Neutral })

            assertTrue(repository.submittedLiveness.isEmpty())
            assertEquals(LivenessPhase.FAILED, viewModel.machine.current.phase)
            assertEquals(
                LivenessFailure.EVIDENCE_INCOMPLETE,
                viewModel.machine.current.failure,
            )
        }

    // -- Pembantu --------------------------------------------------------------

    private fun viewModel() = BukaRekeningVerifikasiBiometrikViewModel(
        repository = repository,
        store = store,
        attestor = attestor,
    )

    /**
     * Menjalankan satu siklus penuh seperti di produksi.
     *
     * Mesin status benar-benar digerakkan sampai fase CAPTURE memakai frame sintetis,
     * bukan dilompati dengan langsung menembakkan `FramesReady`. Versi sebelumnya
     * melompatinya, dan akibatnya tiga test lolos hanya karena mesin status waktu itu
     * belum menolak jawaban server yang datang di fase yang salah. Jalur yang dipintas
     * adalah jalur yang tidak teruji.
     */
    private fun TestScope.submitOnce(
        viewModel: BukaRekeningVerifikasiBiometrikViewModel,
        frames: List<LivenessFrame> = frames(),
    ) {
        val machine = viewModel.machine

        viewModel.onEvent(VerifikasiBiometrikEvent.CameraReady)
        machine.onFrame(face(0))
        machine.onFrame(face(600))

        viewModel.onEvent(VerifikasiBiometrikEvent.ChallengeNeeded)
        runCurrent()

        // Tanpa tantangan yang sah, mesin tidak akan pernah sampai ke CAPTURE —
        // dan itu memang yang diuji sebagian test di berkas ini.
        if (machine.current.challenge == null) return

        machine.onFrame(face(700))

        var time = 800L
        for (action in machine.current.challenge!!.actions) {
            time = machine.perform(action, time)
            // Kembali ke netral di antara gerakan kepala.
            if (action != LivenessAction.BLINK) {
                machine.onFrame(face(time))
                time += 100L
            }
        }

        if (machine.current.phase == LivenessPhase.CAPTURE) {
            viewModel.onEvent(VerifikasiBiometrikEvent.FramesReady(frames))
            runCurrent()
        }
    }

    /** Menggerakkan satu aksi sampai terdeteksi, mengembalikan waktu berikutnya. */
    private fun LivenessStateMachine.perform(action: LivenessAction, startAt: Long): Long {
        var time = startAt
        when (action) {
            LivenessAction.BLINK -> {
                onFrame(face(time, eye = 0.95f)); time += 100L
                onFrame(face(time, eye = 0.05f)); time += 100L
                onFrame(face(time, eye = 0.95f)); time += 100L
            }
            else -> {
                val yaw = when (action) {
                    LivenessAction.TURN_LEFT -> 30f
                    LivenessAction.TURN_RIGHT -> -30f
                    else -> 0f
                }
                val pitch = when (action) {
                    LivenessAction.LOOK_UP -> 25f
                    LivenessAction.LOOK_DOWN -> -20f
                    else -> 0f
                }
                repeat(3) {
                    onFrame(face(time, yaw = yaw, pitch = pitch))
                    time += 100L
                }
            }
        }
        return time
    }

    private fun face(
        timestampMillis: Long,
        yaw: Float = 0f,
        pitch: Float = 0f,
        eye: Float = 0.95f,
    ) = FaceSignals(
        timestampMillis = timestampMillis,
        faceCount = 1,
        trackingId = 7,
        yawDegrees = yaw,
        pitchDegrees = pitch,
        rollDegrees = 0f,
        leftEyeOpenProbability = eye,
        rightEyeOpenProbability = eye,
        faceWidthRatio = 0.5f,
        faceCenterX = 0.5f,
        faceCenterY = 0.5f,
    )

    private fun challenge() = LivenessChallenge(
        challengeId = "chl_1",
        nonce = "nonce_1",
        actions = listOf(LivenessAction.BLINK, LivenessAction.TURN_LEFT, LivenessAction.LOOK_UP),
        expiresAtMillis = Long.MAX_VALUE,
    )

    private fun frames() = listOf(
        LivenessFrame(CaptureSlot.Neutral, byteArrayOf(1, 2, 3), 100),
        LivenessFrame(CaptureSlot.Step(0, LivenessAction.BLINK), byteArrayOf(9, 9, 9), 200),
        LivenessFrame(CaptureSlot.Step(1, LivenessAction.TURN_LEFT), byteArrayOf(4, 5), 300),
        LivenessFrame(CaptureSlot.Step(2, LivenessAction.LOOK_UP), byteArrayOf(6, 7), 400),
    )

    private fun verdict(verified: Boolean) = BiometricResult(
        biometricId = "bio_1",
        livenessVerified = verified,
        livenessScore = if (verified) 95.0 else 10.0,
        faceMatchWithKtp = verified,
        faceMatchScore = if (verified) 92.0 else 11.0,
    )

    private class FakeAttestor : LivenessAttestor {
        var signature: String? = "signature_base64"
        val signedPayloads = mutableListOf<String>()
        val integrityNonces = mutableListOf<String>()

        override val signatureAlgorithm = "EC-P256"

        override fun deviceId() = "device_1"

        override fun publicKey() = DevicePublicKey(keyId = "key_1", base64 = "pubkey_base64")

        override fun sign(payload: String): String? {
            signedPayloads += payload
            return signature
        }

        override suspend fun integrityToken(nonce: String): String? {
            integrityNonces += nonce
            return "integrity_token"
        }

        override fun riskSignals() = DeviceRiskSignals(
            isEmulatorLikely = false,
            hasRootArtifacts = false,
            isDebuggerAttached = false,
        )
    }
}
