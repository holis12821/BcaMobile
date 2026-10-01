package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Perilaku layar OTP di `BukaRekeningFlowViewModel`.
 *
 * Aturannya datang dari skill `frontend-otp-verification` §4, §6, dan §8 —
 * sebagian besar soal apa yang **tidak** boleh terjadi: request dengan kode
 * belum lengkap, `resend-otp` setelah `OTP_EXPIRED`, hitung mundur palsu, dan
 * navigasi maju berdasarkan tebakan client.
 *
 * Waktu dijalankan virtual (`StandardTestDispatcher`), jadi hitung mundur lima
 * menit dan blokir tiga puluh menit ikut teruji tanpa menunggu nyata.
 */
class BukaRekeningFlowViewModelOtpTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Kedatangan di layar OTP ----------------------------------------------

    @Test
    fun `personal data sukses memakai step dari server dan memulai hitung mundur`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)

            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(OTP_TTL_SECONDS))

            val state = viewModel.state.value
            assertEquals(MASKED_PHONE, state.otpSentTo)
            // Hitung mundur diambil dari `otp_expires_at`, bukan konstanta layar.
            assertTrue(
                "hitung mundur = ${state.otpCountdownSeconds}",
                state.otpCountdownSeconds in 295..OTP_TTL_SECONDS,
            )
            assertEquals(
                BukaRekeningSideEffect.AdvanceTo(OnboardingStep.OTP_VERIFY),
                effects.last(),
            )
        }

    @Test
    fun `otp_expires_at kosong tidak memunculkan hitung mundur palsu`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)

        // Terjadi saat proses aplikasi mati: deadline OTP tidak bisa ditarik ulang
        // dari server, jadi layar menawarkan kirim ulang alih-alih angka tebakan.
        arriveAtOtp(repo, viewModel, otpExpiresAt = null)

        assertEquals(0, viewModel.state.value.otpCountdownSeconds)
    }

    @Test
    fun `deadline jauh di masa depan dibatasi ke umur OTP`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)

        // Jam perangkat yang bergeser satu jam tidak boleh mematikan tombol kirim
        // ulang selamanya; hitung mundur hanya tampilan.
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(3_600))

        assertEquals(OTP_TTL_SECONDS, viewModel.state.value.otpCountdownSeconds)
    }

    // -- Verifikasi ------------------------------------------------------------

    @Test
    fun `verifikasi sukses pindah ke step dari response bukan konstanta client`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)
            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

            // Sengaja bukan BIOMETRIC: kalau client menebak, test ini gagal.
            repo.verifyOtpResult =
                DataResult.Success(OtpVerification(verified = true, currentStep = OnboardingStep.VIDEO_CALL))
            submitCode(viewModel, "847291")

            val state = viewModel.state.value
            assertTrue(state.isOtpVerified)
            assertEquals("", state.otpCode)
            assertEquals(listOf("847291"), repo.verifyOtpCodes)
            assertEquals(
                BukaRekeningSideEffect.AdvanceTo(OnboardingStep.VIDEO_CALL),
                effects.last(),
            )
        }

    @Test
    fun `kode kurang dari enam digit tidak pernah dikirim`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

        // Server menjawabnya VALIDATION_ERROR, dan itu bug client — bukan
        // kesalahan nasabah yang perlu ditampilkan.
        submitCode(viewModel, "8472")

        assertTrue(repo.verifyOtpCodes.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `karakter bukan angka dibuang dari input`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

        // Autofill SMS dan papan ketik bisa membawa spasi atau teks lain.
        viewModel.onEvent(BukaRekeningEvent.OtpCodeChanged(" 84 72a91x "))
        runCurrent()

        assertEquals("847291", viewModel.state.value.otpCode)
    }

    @Test
    fun `OTP_INVALID mengosongkan input tapi hitung mundur tetap jalan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))
        val countdownSebelum = viewModel.state.value.otpCountdownSeconds

        repo.verifyOtpResult = DataResult.Failure(
            ApiFailure.Business("OTP_INVALID", "Kode OTP tidak valid."),
        )
        submitCode(viewModel, "111111")

        val state = viewModel.state.value
        assertEquals("", state.otpCode)
        assertFalse(state.isOtpInputBlocked)
        assertEquals(countdownSebelum, state.otpCountdownSeconds)
        // Pesan yang tampil milik server, bukan teks cadangan kita.
        assertEquals(ErrorText.Raw("Kode OTP tidak valid."), state.error)
    }

    @Test
    fun `OTP_EXPIRED memulai jendela lima menit dan tidak memanggil kirim ulang`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            collectEffects(viewModel)
            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(30))

            // Gagal ke-3: server sudah menerbitkan dan mengirim OTP baru, tapi
            // response-nya tidak membawa otp_expires_at.
            repo.verifyOtpResult = DataResult.Failure(ApiFailure.Business("OTP_EXPIRED", ""))
            submitCode(viewModel, "111111")

            val state = viewModel.state.value
            assertEquals(OTP_TTL_SECONDS, state.otpCountdownSeconds)
            assertEquals("", state.otpCode)
            assertFalse(state.isOtpInputBlocked)
            // SMS-nya sudah berangkat; kirim ulang manual hanya memotong kuota.
            assertEquals(0, repo.resendOtpCount)
            assertEquals(ErrorText.Res(R.string.buka_rekening_error_otp_expired), state.error)
        }

    @Test
    fun `OTP_BLOCKED mematikan input dan kirim ulang sampai hitung mundur habis`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            collectEffects(viewModel)
            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

            repo.verifyOtpResult = DataResult.Failure(
                ApiFailure.RateLimited(
                    retryAfterSeconds = BLOCK_SECONDS,
                    isOtpBlocked = true,
                    message = "Terlalu banyak percobaan OTP.",
                ),
            )
            submitCode(viewModel, "111111")

            val blocked = viewModel.state.value
            assertTrue(blocked.isOtpInputBlocked)
            assertEquals(BLOCK_SECONDS, blocked.otpCountdownSeconds)

            // Selama terblokir, layar tidak boleh membebani endpoint lagi.
            submitCode(viewModel, "847291")
            viewModel.onEvent(BukaRekeningEvent.OtpResendRequested)
            runCurrent()
            assertEquals(listOf("111111"), repo.verifyOtpCodes)
            assertEquals(0, repo.resendOtpCount)

            advanceTimeBy((BLOCK_SECONDS + 1) * 1_000L)
            runCurrent()

            val released = viewModel.state.value
            assertFalse(released.isOtpInputBlocked)
            assertEquals(0, released.otpCountdownSeconds)
        }

    @Test
    fun `retry_after_seconds nol memakai fallback alih-alih menghidupkan tombol seketika`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            collectEffects(viewModel)
            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

            // Terjadi kalau server gagal membaca sisa waktu dari Redis.
            repo.verifyOtpResult = DataResult.Failure(
                ApiFailure.RateLimited(retryAfterSeconds = 0, isOtpBlocked = true),
            )
            submitCode(viewModel, "111111")

            assertEquals(RETRY_FALLBACK_SECONDS, viewModel.state.value.otpCountdownSeconds)
        }

    // -- Kirim ulang -----------------------------------------------------------

    @Test
    fun `kirim ulang sukses mengosongkan input dan memakai deadline baru`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        // Hitung mundur sudah habis, jadi tombol kirim ulang boleh dipakai.
        arriveAtOtp(repo, viewModel, otpExpiresAt = null)

        viewModel.onEvent(BukaRekeningEvent.OtpCodeChanged("111"))
        repo.resendOtpResult = DataResult.Success(
            OtpChallenge(otpSentTo = "0812****9999", otpExpiresAt = expiresIn(OTP_TTL_SECONDS)),
        )
        viewModel.onEvent(BukaRekeningEvent.OtpResendRequested)
        runCurrent()

        val state = viewModel.state.value
        assertEquals(1, repo.resendOtpCount)
        assertEquals("0812****9999", state.otpSentTo)
        // Kode lama langsung ditolak server, jadi field input ikut dibersihkan.
        assertEquals("", state.otpCode)
        assertTrue(state.otpCountdownSeconds in 295..OTP_TTL_SECONDS)
    }

    @Test
    fun `kirim ulang ditahan selama hitung mundur masih berjalan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

        viewModel.onEvent(BukaRekeningEvent.OtpResendRequested)
        runCurrent()

        assertEquals(0, repo.resendOtpCount)
    }

    @Test
    fun `kuota kirim ulang habis hanya mematikan tombolnya`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = null)

        repo.resendOtpResult = DataResult.Failure(
            ApiFailure.RateLimited(retryAfterSeconds = 900, message = "Batas kirim ulang tercapai."),
        )
        viewModel.onEvent(BukaRekeningEvent.OtpResendRequested)
        runCurrent()

        val state = viewModel.state.value
        assertTrue(state.isOtpResendBlocked)
        // Kode terakhir masih sah, jadi input tidak boleh ikut mati.
        assertFalse(state.isOtpInputBlocked)
        assertEquals(900, state.otpCountdownSeconds)
        assertEquals(ErrorText.Raw("Batas kirim ulang tercapai."), state.error)
    }

    @Test
    fun `OTP_DELIVERY_FAILED tetap menawarkan kirim ulang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = null)

        repo.resendOtpResult = DataResult.Failure(
            ApiFailure.Business("OTP_DELIVERY_FAILED", ""),
        )
        viewModel.onEvent(BukaRekeningEvent.OtpResendRequested)
        runCurrent()

        val state = viewModel.state.value
        // SMS gagal berangkat, tapi kodenya sah: jangan usir nasabah ke awal flow.
        assertFalse(state.isOtpResendBlocked)
        assertFalse(state.isOtpInputBlocked)
        assertEquals(0, state.otpCountdownSeconds)
        assertEquals(
            ErrorText.Res(R.string.buka_rekening_error_otp_delivery_failed),
            state.error,
        )
    }

    // -- Sesi dan kerahasiaan kode --------------------------------------------

    @Test
    fun `ONBOARDING_INVALID_STEP menarik ulang sesi dan pindah ke step sebenarnya`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)
            arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

            repo.verifyOtpResult = DataResult.Failure(ApiFailure.InvalidStep)
            repo.getSessionResult = DataResult.Success(session(OnboardingStep.BIOMETRIC))
            val sessionReadsSebelum = repo.getSessionCount
            submitCode(viewModel, "847291")

            assertEquals(sessionReadsSebelum + 1, repo.getSessionCount)
            assertEquals(
                BukaRekeningSideEffect.AdvanceTo(OnboardingStep.BIOMETRIC),
                effects.last(),
            )
        }

    @Test
    fun `sesi hilang membersihkan jejak lokal dan meminta mulai ulang`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        val effects = collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

        repo.verifyOtpResult = DataResult.Failure(ApiFailure.SessionNotFound)
        submitCode(viewModel, "847291")

        assertEquals(1, repo.clearLocalSessionCount)
        assertEquals(BukaRekeningSideEffect.RestartFlow, effects.last())
        assertEquals("", viewModel.state.value.otpCode)
    }

    @Test
    fun `kode OTP dibuang saat layar ditinggalkan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val viewModel = viewModel(repo)
        collectEffects(viewModel)
        arriveAtOtp(repo, viewModel, otpExpiresAt = expiresIn(120))

        viewModel.onEvent(BukaRekeningEvent.OtpCodeChanged("847291"))
        runCurrent()
        viewModel.onEvent(BukaRekeningEvent.OtpCodeCleared)
        runCurrent()

        assertEquals("", viewModel.state.value.otpCode)
    }

    // -- Perkakas --------------------------------------------------------------

    private fun viewModel(repo: FakeOnboardingRepository) =
        // KtpTextRecognizer membangun client ML Kit-nya saat dipakai, bukan saat
        // dibuat, jadi aman diinstansiasi di test JVM selama OCR tidak dijalankan.
        BukaRekeningFlowViewModel(repo, KtpTextRecognizer())

    private fun TestScope.collectEffects(
        viewModel: BukaRekeningFlowViewModel,
    ): List<BukaRekeningSideEffect> {
        val effects = mutableListOf<BukaRekeningSideEffect>()
        backgroundScope.launch { viewModel.sideEffect.collect { effects += it } }
        runCurrent()
        return effects
    }

    /** Menjalankan flow sampai layar OTP lewat jalur yang sama dengan aplikasi. */
    private fun TestScope.arriveAtOtp(
        repo: FakeOnboardingRepository,
        viewModel: BukaRekeningFlowViewModel,
        otpExpiresAt: String?,
    ) {
        repo.createSessionResult = DataResult.Success(session(OnboardingStep.OCR))
        viewModel.onEvent(BukaRekeningEvent.ProductSelected(0))
        viewModel.onEvent(BukaRekeningEvent.TncAccepted)
        runCurrent()

        repo.ocrResult = DataResult.Success(ocrResult())
        viewModel.onEvent(BukaRekeningEvent.OcrResultRequested)
        runCurrent()

        repo.personalDataResult = DataResult.Success(
            PersonalDataResult(
                personalDataId = "pd_x1y2z3",
                otpSentTo = MASKED_PHONE,
                otpExpiresAt = otpExpiresAt,
                currentStep = OnboardingStep.OTP_VERIFY,
            ),
        )
        viewModel.onEvent(BukaRekeningEvent.PersonalDataSubmitted)
        runCurrent()
    }

    private fun TestScope.submitCode(viewModel: BukaRekeningFlowViewModel, code: String) {
        viewModel.onEvent(BukaRekeningEvent.OtpCodeChanged(code))
        viewModel.onEvent(BukaRekeningEvent.OtpSubmitted)
        runCurrent()
    }

    private companion object {
        const val MASKED_PHONE = "0812****8889"
        const val OTP_TTL_SECONDS = 300
        const val RETRY_FALLBACK_SECONDS = 60
        const val BLOCK_SECONDS = 1_800

        fun expiresIn(seconds: Int): String =
            Instant.now().plusSeconds(seconds.toLong()).toString()

        fun session(step: OnboardingStep) = OnboardingSession(
            sessionId = "onb_9f8e7d6c5b4a",
            product = null,
            currentStep = step,
        )

        fun ocrResult() = KtpOcrResult(
            ocrId = "ocr_1",
            accuracyPercent = 98.0,
            extracted = KtpData(
                nik = "3174082104950001",
                namaLengkap = "MUHAMMAD ARDAN",
                tempatLahir = "JAKARTA",
                tanggalLahir = "1995-04-21",
                jenisKelamin = "LAKI_LAKI",
                alamat = "JL. KEBON JERUK NO. 12",
                rtRw = "003/005",
                kelurahan = "KEBON JERUK",
                kecamatan = "KEBON JERUK",
                kota = "JAKARTA BARAT",
                provinsi = "DKI JAKARTA",
                agama = "ISLAM",
                statusPerkawinan = "BELUM MENIKAH",
            ),
            dukcapilMatch = true,
            sharpness = "HIGH",
            glareDetected = false,
            allCornersVisible = true,
        )
    }
}
