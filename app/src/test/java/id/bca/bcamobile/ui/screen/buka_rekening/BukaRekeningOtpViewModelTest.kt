package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiForm
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.BukaRekeningDataPribadiViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.DataPribadiEvent
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.BukaRekeningVerifikasiOtpViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.VerifikasiOtpEvent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Perilaku layar OTP di `BukaRekeningVerifikasiOtpViewModel`.
 *
 * Aturannya datang dari skill `frontend-otp-verification` §4, §6, dan §8 —
 * sebagian besar soal apa yang **tidak** boleh terjadi: request dengan kode
 * belum lengkap, `resend-otp` setelah `OTP_EXPIRED`, hitung mundur palsu, dan
 * navigasi maju berdasarkan tebakan client.
 *
 * Sejak tiap layar punya ViewModel sendiri, yang diuji di sini dua ViewModel
 * yang berbagi satu [BukaRekeningSessionStore]: layar Data Pribadi menerbitkan
 * OTP dan mencatat `otp_expires_at`, layar OTP yang menghitung mundurnya.
 * Jalur itu sengaja dilewati apa adanya, bukan dipintas dengan menyuntik state —
 * pembagian tugas antar-ViewModel justru bagian yang mudah rusak.
 *
 * Waktu dijalankan virtual (`StandardTestDispatcher`), jadi hitung mundur lima
 * menit dan blokir tiga puluh menit ikut teruji tanpa menunggu nyata.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BukaRekeningOtpViewModelTest {

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
            val store = BukaRekeningSessionStore()
            val effects = collectEffects(store)

            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(OTP_TTL_SECONDS))

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
    fun `personal data OTP_DELIVERY_FAILED tetap maju ke step dari server`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            val effects = collectEffects(store)
            store.update { it.copy(sessionId = SESSION_ID, ocr = ocrResult()) }

            // 503 dengan kode ini bukan penolakan: data pribadi sudah tersimpan dan server
            // sudah pindah ke OTP_VERIFY, hanya SMS-nya yang gagal berangkat. Tanpa ini
            // nasabah tertahan di layar Data Pribadi sementara server sudah maju, dan tap
            // "Lanjut" berikutnya cuma menghasilkan ONBOARDING_INVALID_STEP.
            repo.personalDataResult = DataResult.Failure(
                ApiFailure.Business("OTP_DELIVERY_FAILED", ""),
            )
            repo.getSessionResult = DataResult.Success(session(OnboardingStep.OTP_VERIFY))

            val dataPribadi = BukaRekeningDataPribadiViewModel(repo, store)
            dataPribadi.onEvent(DataPribadiEvent.ScreenShown)
            store.update { it.copy(dataPribadi = completedForm(store)) }
            dataPribadi.onEvent(DataPribadiEvent.PersonalDataSubmitted)
            runCurrent()

            // Step-nya ditanyakan ke server, bukan ditebak lokal.
            assertEquals(1, repo.getSessionCount)
            assertEquals(
                BukaRekeningSideEffect.AdvanceTo(OnboardingStep.OTP_VERIFY),
                effects.last(),
            )

            // Begitu sampai di layar OTP, kirim ulang harus langsung tersedia: itu satu-satunya
            // jalan keluar nasabah kalau SMS tidak pernah datang.
            val otp = BukaRekeningVerifikasiOtpViewModel(repo, store)
            val state = otp.state.value
            assertFalse(state.isOtpResendBlocked)
            assertFalse(state.isOtpInputBlocked)
            assertEquals(0, state.otpCountdownSeconds)
            assertEquals(
                ErrorText.Res(R.string.buka_rekening_error_otp_delivery_failed),
                state.error,
            )
        }

    @Test
    fun `otp_expires_at kosong tidak memunculkan hitung mundur palsu`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)

        // Terjadi saat proses aplikasi mati: deadline OTP tidak bisa ditarik ulang
        // dari server, jadi layar menawarkan kirim ulang alih-alih angka tebakan.
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = null)

        assertEquals(0, viewModel.state.value.otpCountdownSeconds)
    }

    @Test
    fun `deadline jauh di masa depan dibatasi ke plafon jendela`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)

        // Jam perangkat yang bergeser satu jam tidak boleh mematikan tombol kirim
        // ulang selamanya; hitung mundur hanya tampilan.
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(3_600))

        assertEquals(OTP_MAX_WINDOW_SECONDS, viewModel.state.value.otpCountdownSeconds)
    }

    @Test
    fun `jendela sepuluh menit dari server tidak dipotong`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)

        // Model Twilio Verify yang aktif di dev memakai jendela 10 menit, bukan 5
        // (skill `frontend-otp-verification` §6.5a). Plafon yang terlalu rendah membuat
        // hitung mundur selesai saat kodenya sebenarnya masih berlaku.
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(600))

        assertTrue(
            "hitung mundur = ${viewModel.state.value.otpCountdownSeconds}",
            viewModel.state.value.otpCountdownSeconds in 595..OTP_MAX_WINDOW_SECONDS,
        )
    }

    @Test
    fun `data pribadi belum lengkap tidak pernah memanggil personal-data`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            val effects = collectEffects(store)
            store.update { it.copy(sessionId = SESSION_ID, ocr = ocrResult()) }

            // Hanya prefill OCR: kode pos, nomor HP, dan email tidak ada di e-KTP.
            val dataPribadi = BukaRekeningDataPribadiViewModel(repo, store)
            dataPribadi.onEvent(DataPribadiEvent.ScreenShown)
            dataPribadi.onEvent(DataPribadiEvent.PersonalDataSubmitted)
            runCurrent()

            val state = store.current
            assertTrue(state.showDataPribadiErrors)
            assertEquals(ErrorText.Res(R.string.buka_rekening_dp_error_form), state.error)
            // Request yang pasti ditolak server tetap memakan kuota rate limit sesi.
            assertTrue(effects.isEmpty())
            assertEquals("", state.otpSentTo)
        }

    // -- Verifikasi ------------------------------------------------------------

    @Test
    fun `verifikasi sukses pindah ke step dari response bukan konstanta client`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            val effects = collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

            // Sengaja bukan BIOMETRIC: kalau client menebak, test ini gagal.
            repo.verifyOtpResult = DataResult.Success(
                OtpVerification(verified = true, currentStep = OnboardingStep.VIDEO_CALL),
            )
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
    fun `digit keenam mengirim sendiri tanpa menunggu tombol`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))
        repo.verifyOtpResult = DataResult.Success(
            OtpVerification(verified = true, currentStep = OnboardingStep.BIOMETRIC),
        )

        // Lima digit belum memicu apa pun.
        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged("84729"))
        runCurrent()
        assertTrue(repo.verifyOtpCodes.isEmpty())

        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged("847291"))
        runCurrent()

        assertEquals(listOf("847291"), repo.verifyOtpCodes)
    }

    @Test
    fun `tombol verifikasi tidak mengirim ulang kode yang sudah berangkat`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))
            repo.verifyOtpResult = DataResult.Success(
                OtpVerification(verified = true, currentStep = OnboardingStep.BIOMETRIC),
            )

            // Digit keenam dan tekanan tombol bisa datang dalam satu frame; satu
            // kode tidak boleh memotong dua jatah percobaan.
            viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged("847291"))
            viewModel.onEvent(VerifikasiOtpEvent.OtpSubmitted)
            runCurrent()

            assertEquals(listOf("847291"), repo.verifyOtpCodes)
        }

    @Test
    fun `kode kurang dari enam digit tidak pernah dikirim`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

        // Server menjawabnya VALIDATION_ERROR, dan itu bug client — bukan
        // kesalahan nasabah yang perlu ditampilkan.
        submitCode(viewModel, "8472")

        assertTrue(repo.verifyOtpCodes.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `karakter bukan angka dibuang dari input`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

        // Autofill SMS dan papan ketik bisa membawa spasi atau teks lain.
        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged(" 84 72a91x "))
        runCurrent()

        assertEquals("847291", viewModel.state.value.otpCode)
    }

    @Test
    fun `OTP_INVALID mengosongkan input tapi hitung mundur tetap jalan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))
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
    fun `OTP_EXPIRED membuka kirim ulang tanpa memanggilnya sendiri`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(30))

            // Response `OTP_EXPIRED` tidak membawa `otp_expires_at`, jadi tidak ada cara
            // tahu apakah server sudah menerbitkan OTP baru sendiri. Kode yang lama pasti
            // mati, jadi nasabah wajib bisa minta yang baru: hitung mundur dinolkan dan
            // tombol kirim ulang hidup. Yang tetap dilarang adalah memanggil resend
            // otomatis — itu memotong kuota tanpa diminta.
            repo.verifyOtpResult = DataResult.Failure(ApiFailure.Business("OTP_EXPIRED", ""))
            submitCode(viewModel, "111111")

            val state = viewModel.state.value
            assertEquals(0, state.otpCountdownSeconds)
            assertEquals("", state.otpCode)
            assertFalse(state.isOtpInputBlocked)
            assertFalse(state.isOtpResendBlocked)
            assertEquals(0, repo.resendOtpCount)
            assertEquals(ErrorText.Res(R.string.buka_rekening_error_otp_expired), state.error)
        }

    @Test
    fun `OTP_BLOCKED mematikan input dan kirim ulang sampai hitung mundur habis`() =
        runTest(dispatcher) {
            val repo = FakeOnboardingRepository()
            val store = BukaRekeningSessionStore()
            collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

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
            viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested)
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
            val store = BukaRekeningSessionStore()
            collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

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
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        // Hitung mundur sudah habis, jadi tombol kirim ulang boleh dipakai.
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = null)

        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged("111"))
        repo.resendOtpResult = DataResult.Success(
            OtpChallenge(otpSentTo = "0812****9999", otpExpiresAt = expiresIn(OTP_TTL_SECONDS)),
        )
        viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested)
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
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

        viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested)
        runCurrent()

        assertEquals(0, repo.resendOtpCount)
    }

    @Test
    fun `kuota kirim ulang habis hanya mematikan tombolnya`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = null)

        repo.resendOtpResult = DataResult.Failure(
            ApiFailure.RateLimited(retryAfterSeconds = 900, message = "Batas kirim ulang tercapai."),
        )
        viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested)
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
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = null)

        repo.resendOtpResult = DataResult.Failure(
            ApiFailure.Business("OTP_DELIVERY_FAILED", ""),
        )
        viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested)
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
            val store = BukaRekeningSessionStore()
            val effects = collectEffects(store)
            val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

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
        val store = BukaRekeningSessionStore()
        val effects = collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

        repo.verifyOtpResult = DataResult.Failure(ApiFailure.SessionNotFound)
        submitCode(viewModel, "847291")

        assertEquals(1, repo.clearLocalSessionCount)
        assertEquals(BukaRekeningSideEffect.RestartFlow, effects.last())
        assertEquals("", viewModel.state.value.otpCode)
        // Sesi yang hangus membuang seluruh PII, termasuk isian data pribadi.
        assertNull(viewModel.state.value.dataPribadi)
    }

    @Test
    fun `kode OTP dibuang saat layar ditinggalkan`() = runTest(dispatcher) {
        val repo = FakeOnboardingRepository()
        val store = BukaRekeningSessionStore()
        collectEffects(store)
        val viewModel = arriveAtOtp(repo, store, otpExpiresAt = expiresIn(120))

        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged("847291"))
        runCurrent()
        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeCleared)
        runCurrent()

        assertEquals("", viewModel.state.value.otpCode)
    }

    // -- Perkakas --------------------------------------------------------------

    /**
     * Side effect seluruh flow lewat satu channel di store, dan channel hanya
     * mengantar ke satu penerima — jadi test mengumpulkannya sekali di sini,
     * bukan per ViewModel.
     */
    private fun TestScope.collectEffects(
        store: BukaRekeningSessionStore,
    ): List<BukaRekeningSideEffect> {
        val effects = mutableListOf<BukaRekeningSideEffect>()
        backgroundScope.launch { store.sideEffect.collect { effects += it } }
        runCurrent()
        return effects
    }

    /**
     * Menerbitkan OTP lewat layar Data Pribadi, lalu membuka layar OTP —
     * urutan yang sama dengan aplikasi, termasuk siapa yang mencatat
     * `otp_expires_at` dan siapa yang menghitungnya.
     */
    private fun TestScope.arriveAtOtp(
        repo: FakeOnboardingRepository,
        store: BukaRekeningSessionStore,
        otpExpiresAt: String?,
    ): BukaRekeningVerifikasiOtpViewModel {
        store.update { it.copy(sessionId = SESSION_ID, ocr = ocrResult()) }

        repo.personalDataResult = DataResult.Success(
            PersonalDataResult(
                personalDataId = "pd_x1y2z3",
                otpSentTo = MASKED_PHONE,
                otpExpiresAt = otpExpiresAt,
                currentStep = OnboardingStep.OTP_VERIFY,
            ),
        )

        val dataPribadi = BukaRekeningDataPribadiViewModel(repo, store)
        dataPribadi.onEvent(DataPribadiEvent.ScreenShown)
        store.update { it.copy(dataPribadi = completedForm(store)) }
        dataPribadi.onEvent(DataPribadiEvent.PersonalDataSubmitted)
        runCurrent()

        // Jendela hitung mundur dibuka di `init`, jadi ViewModel-nya baru dibuat
        // setelah `otp_expires_at` tercatat di store — sama seperti saat layar
        // OTP masuk komposisi.
        return BukaRekeningVerifikasiOtpViewModel(repo, store)
    }

    /** Prefill OCR ditambah tiga isian yang tidak ada di e-KTP. */
    private fun completedForm(store: BukaRekeningSessionStore): DataPribadiForm =
        (store.current.dataPribadi ?: DataPribadiForm()).copy(
            kodePos = "12190",
            nomorHp = "081234568889",
            email = "m.ardan@example.com",
        )

    private fun TestScope.submitCode(
        viewModel: BukaRekeningVerifikasiOtpViewModel,
        code: String,
    ) {
        viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged(code))
        viewModel.onEvent(VerifikasiOtpEvent.OtpSubmitted)
        runCurrent()
    }

    private companion object {
        const val SESSION_ID = "onb_9f8e7d6c5b4a"
        const val MASKED_PHONE = "0812****8889"
        const val OTP_TTL_SECONDS = 300

        /** Plafon hitung mundur di ViewModel; mengikuti jendela terpanjang dari server. */
        const val OTP_MAX_WINDOW_SECONDS = 600
        const val RETRY_FALLBACK_SECONDS = 60
        const val BLOCK_SECONDS = 1_800

        fun expiresIn(seconds: Int): String =
            Instant.now().plusSeconds(seconds.toLong()).toString()

        fun session(step: OnboardingStep) = OnboardingSession(
            sessionId = SESSION_ID,
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
            dukcapilChecked = true,
            sharpness = "HIGH",
            glareDetected = false,
            allCornersVisible = true,
        )
    }
}
