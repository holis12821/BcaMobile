package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import android.graphics.Bitmap
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.liveness.LivenessProgress
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.AlamatKtp
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import id.bca.bcamobile.domain.onboarding.model.ProductType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel tunggal untuk seluruh flow buka rekening.
 *
 * Di-scope ke navigation graph buka rekening (`hiltViewModel` dengan
 * `navController.getBackStackEntry(BukaRekening)`), bukan ke masing-masing
 * screen — dua belas layar berbagi satu sesi, jadi state-nya harus satu.
 *
 * Pola: MVI. Satu pintu masuk [onEvent], satu [state] untuk UI, dan
 * [sideEffect] terpisah untuk navigasi dan pesan sekali pakai.
 *
 * Navigasi maju **selalu** mengikuti `current_step` yang dikirim server lewat
 * [BukaRekeningSideEffect.AdvanceTo], bukan hasil tebakan lokal.
 */
@HiltViewModel
class BukaRekeningFlowViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    private val textRecognizer: KtpTextRecognizer,
) : ViewModel() {

    private val _state = MutableStateFlow(BukaRekeningFlowState())
    val state: StateFlow<BukaRekeningFlowState> = _state.asStateFlow()

    private val _sideEffect = Channel<BukaRekeningSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<BukaRekeningSideEffect> = _sideEffect.receiveAsFlow()

    init {
        // Draft dari sesi sebelumnya (mis. proses sempat mati) ditandai supaya
        // UI bisa menawarkan "Lanjutkan pendaftaran?".
        _state.update { it.copy(hasResumableDraft = repository.savedSessionId() != null) }
    }

    fun onEvent(event: BukaRekeningEvent) {
        when (event) {
            is BukaRekeningEvent.ProductSelected ->
                _state.update { it.copy(selectedProductIndex = event.index, error = null) }

            is BukaRekeningEvent.CardTypeSelected ->
                _state.update { it.copy(selectedCardIndex = event.index, error = null) }

            BukaRekeningEvent.TncAccepted -> createSession()

            is BukaRekeningEvent.KtpPhotoCaptured -> processCapturedPhoto(event)

            BukaRekeningEvent.FlashModeToggled -> _state.update {
                it.copy(flashMode = it.flashMode.next())
            }

            is BukaRekeningEvent.AutoCaptureToggled -> _state.update {
                it.copy(isAutoCaptureEnabled = event.enabled)
            }

            BukaRekeningEvent.PhotoDiscarded -> discardPhoto()
            BukaRekeningEvent.OcrResultRequested -> loadOcrResult()
            BukaRekeningEvent.OcrConfirmed -> confirmOcr()

            is BukaRekeningEvent.GenderSelected ->
                _state.update { it.copy(jenisKelamin = event.value) }

            is BukaRekeningEvent.DomicileSameToggled ->
                _state.update { it.copy(alamatDomisiliSama = event.same) }

            BukaRekeningEvent.PersonalDataSubmitted -> savePersonalData()

            is BukaRekeningEvent.OtpCodeChanged -> onOtpCodeChanged(event.code)
            BukaRekeningEvent.OtpSubmitted -> verifyOtp()
            BukaRekeningEvent.OtpResendRequested -> resendOtp()
            BukaRekeningEvent.OtpCodeCleared -> _state.update { it.copy(otpCode = "") }

            is BukaRekeningEvent.BiometricCaptured -> uploadBiometric(event)

            BukaRekeningEvent.LivenessStarted -> _state.update {
                it.copy(isLivenessRunning = true, liveness = LivenessProgress())
            }

            is BukaRekeningEvent.LivenessProgressed -> _state.update {
                it.copy(liveness = event.progress)
            }

            BukaRekeningEvent.QueueJoinRequested -> joinQueue()
            is BukaRekeningEvent.VideoCallCompleted -> completeVideoCall(event.csName)

            is BukaRekeningEvent.AccessCodeChanged ->
                _state.update { it.copy(accessCode = event.value, error = null) }

            is BukaRekeningEvent.ConfirmAccessCodeChanged ->
                _state.update { it.copy(confirmAccessCode = event.value, error = null) }

            BukaRekeningEvent.AccessCodeVisibilityToggled ->
                _state.update { it.copy(isAccessCodeVisible = !it.isAccessCodeVisible) }

            BukaRekeningEvent.ConfirmAccessCodeVisibilityToggled ->
                _state.update { it.copy(isConfirmAccessCodeVisible = !it.isConfirmAccessCodeVisible) }

            is BukaRekeningEvent.PinChanged ->
                _state.update { it.copy(pin = event.value, error = null) }

            is BukaRekeningEvent.ConfirmPinChanged ->
                _state.update { it.copy(confirmPin = event.value, error = null) }

            BukaRekeningEvent.CredentialsSubmitted -> saveCredentials()

            is BukaRekeningEvent.AgreementToggled ->
                _state.update { it.copy(isAgreed = event.agreed, error = null) }

            BukaRekeningEvent.ApplicationSubmitted -> submitApplication()

            BukaRekeningEvent.DraftSaveRequested -> emitMessage(
                ErrorText.Res(R.string.buka_rekening_draft_saved),
            )

            BukaRekeningEvent.DraftResumeRequested -> resumeDraft()
            BukaRekeningEvent.FlowAbandoned -> abandonFlow()

            BukaRekeningEvent.ErrorDismissed -> _state.update { it.copy(error = null) }
        }
    }

    // -- Step 1-2: sesi --------------------------------------------------------

    private fun createSession() {
        val index = _state.value.selectedProductIndex ?: return
        val productType = ProductType.fromIndex(index) ?: return

        launchWithLoading {
            when (val result = repository.createSession(productType, TNC_VERSION)) {
                is DataResult.Success -> {
                    val session = result.value
                    _state.update {
                        it.copy(
                            sessionId = session.sessionId,
                            product = session.product,
                            currentStep = session.currentStep,
                            hasResumableDraft = false,
                        )
                    }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(session.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun resumeDraft() {
        val sessionId = repository.savedSessionId() ?: return
        launchWithLoading {
            when (val result = repository.getSession(sessionId)) {
                is DataResult.Success -> {
                    val session = result.value
                    _state.update {
                        it.copy(
                            sessionId = session.sessionId,
                            product = session.product,
                            currentStep = session.currentStep,
                            isOtpVerified = session.stepsCompleted.otpVerified,
                            isVideoCallVerified = session.stepsCompleted.videoCallVerified,
                            isCredentialsSaved = session.stepsCompleted.credentialsSet,
                            hasResumableDraft = false,
                        )
                    }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(session.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun abandonFlow() {
        viewModelScope.launch {
            repository.cancelSession()
            _state.value = BukaRekeningFlowState()
            _sideEffect.send(BukaRekeningSideEffect.ExitFlow)
        }
    }

    // -- Step 3-5: OCR ---------------------------------------------------------

    /**
     * Membaca e-KTP di perangkat lebih dulu.
     *
     * Hasilnya langsung tampil di layar pratinjau sementara foto belum diunggah,
     * dan foto yang terbaca buruk bisa ditolak di sini — menghemat kuota unggah
     * OCR yang dibatasi 10 kali per jam per sesi.
     */
    private fun processCapturedPhoto(event: BukaRekeningEvent.KtpPhotoCaptured) {
        // Foto lama diganti; berkasnya tidak boleh menumpuk di cache.
        CameraCapture.discard(_state.value.ktpPhoto)

        _state.update {
            it.copy(
                ktpPhoto = event.photo,
                captureResolution = event.resolution,
                ocr = null,
                localScan = null,
                isProcessingPhoto = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val bitmap = CameraCapture.decode(event.photo)
            val scan = bitmap?.let { textRecognizer.scan(it) }
            bitmap?.recycle()

            _state.update { it.copy(localScan = scan, isProcessingPhoto = false) }
            _sideEffect.send(BukaRekeningSideEffect.ShowCaptureResult)
        }
    }

    private fun discardPhoto() {
        CameraCapture.discard(_state.value.ktpPhoto)
        _state.update {
            it.copy(ktpPhoto = null, localScan = null, ocr = null, captureResolution = "")
        }
    }

    /** Unggah foto ke server untuk OCR resmi dan pencocokan Dukcapil. */
    private fun uploadKtpPhoto() {
        val current = _state.value
        val photo = current.ktpPhoto ?: run {
            _state.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_photo)) }
            return
        }

        launchWithLoading {
            val result = repository.uploadKtpPhoto(
                photo = photo,
                flashUsed = current.flashMode != FlashMode.OFF,
                autoCaptured = current.isAutoCaptureEnabled,
                resolution = current.captureResolution,
            )
            when (result) {
                is DataResult.Success -> {
                    applyOcr(result.value)
                    // Foto sudah di server; salinan lokalnya tidak boleh tertinggal.
                    CameraCapture.discard(photo)
                    _state.update { it.copy(ktpPhoto = null) }
                    _sideEffect.send(
                        BukaRekeningSideEffect.AdvanceTo(OnboardingStep.PERSONAL_DATA),
                    )
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun loadOcrResult() {
        launchWithLoading {
            when (val result = repository.getOcrResult()) {
                is DataResult.Success -> applyOcr(result.value)
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    /** Hasil OCR jadi sumber prefill form data pribadi, termasuk jenis kelamin. */
    private fun applyOcr(ocr: KtpOcrResult) {
        _state.update {
            it.copy(
                ocr = ocr,
                jenisKelamin = it.jenisKelamin ?: ocr.extracted.jenisKelamin.toJenisKelamin(),
            )
        }
    }

    /**
     * Tombol "Gunakan Foto".
     *
     * Kalau server sudah punya hasil OCR untuk sesi ini, langsung maju. Kalau
     * belum, fotonya diunggah sekarang.
     */
    private fun confirmOcr() {
        val current = _state.value
        when {
            current.ocr != null -> viewModelScope.launch {
                _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.PERSONAL_DATA))
            }
            current.ktpPhoto != null -> uploadKtpPhoto()
            else -> _state.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_ocr))
            }
        }
    }

    // -- Step 6: data pribadi dan OTP ------------------------------------------

    private fun savePersonalData() {
        val current = _state.value
        val ocr = current.ocr
        if (ocr == null) {
            _state.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_ocr)) }
            return
        }

        val data = ocr.toPersonalData(
            jenisKelamin = current.jenisKelamin,
            alamatDomisiliSama = current.alamatDomisiliSama,
        )

        launchWithLoading {
            when (val result = repository.savePersonalData(ocr.ocrId, data)) {
                is DataResult.Success -> {
                    val challenge = result.value
                    // OTP pembuka step ini menolkan penghitung gagal dan kuota
                    // kirim ulang di server, jadi blokir lokal ikut dilepas.
                    _state.update {
                        it.copy(
                            otpSentTo = challenge.otpSentTo,
                            otpCode = "",
                            isOtpInputBlocked = false,
                            isOtpResendBlocked = false,
                        )
                    }
                    beginOtpWindow(challenge.otpExpiresAt)
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(challenge.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun onOtpCodeChanged(code: String) {
        // Papan ketik dan autofill SMS bisa membawa spasi atau teks lain.
        val digits = code.filter(Char::isDigit).take(OTP_LENGTH)
        _state.update { it.copy(otpCode = digits, error = null) }
    }

    /**
     * Verifikasi kode OTP.
     *
     * Request ditahan kalau kodenya belum enam digit: server menjawab
     * `VALIDATION_ERROR`, dan itu kesalahan client, bukan kesalahan nasabah.
     */
    private fun verifyOtp() {
        val current = _state.value
        val code = current.otpCode
        if (current.isOtpInputBlocked || code.length != OTP_LENGTH) return

        launchWithLoading {
            when (val result = repository.verifyOtp(code)) {
                is DataResult.Success -> {
                    stopOtpCountdown()
                    _state.update {
                        it.copy(isOtpVerified = true, otpCode = "", otpCountdownSeconds = 0)
                    }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(result.value.currentStep))
                }
                is DataResult.Failure -> handleVerifyOtpFailure(result.error)
            }
        }
    }

    private fun resendOtp() {
        val current = _state.value
        // Kirim ulang bukan jalan memutar blokir, dan jatahnya hanya 3 per jam.
        if (current.isOtpInputBlocked || current.isOtpResendBlocked) return
        if (current.otpCountdownSeconds > 0) return

        launchWithLoading {
            when (val result = repository.resendOtp()) {
                is DataResult.Success -> {
                    val challenge = result.value
                    // Kode lama mati seketika; field input ikut dikosongkan.
                    _state.update { it.copy(otpSentTo = challenge.otpSentTo, otpCode = "") }
                    beginOtpWindow(challenge.otpExpiresAt)
                }
                is DataResult.Failure -> handleResendOtpFailure(result.error)
            }
        }
    }

    /**
     * Kegagalan `verify-otp` yang punya perilaku UI sendiri.
     *
     * Sisanya jatuh ke [handleFailure] — termasuk `ONBOARDING_INVALID_STEP` dan
     * sesi yang hilang.
     */
    private suspend fun handleVerifyOtpFailure(error: ApiFailure) {
        when {
            // Kegagalan ke-3 dijawab OTP_EXPIRED: server sudah menerbitkan dan
            // mengirim OTP baru. Jangan panggil resend — itu memotong kuota kirim
            // ulang tanpa perlu. Response ini tidak membawa otp_expires_at, jadi
            // jendela 5 menit dihitung dari saat jawaban diterima.
            error.isBusinessCode(CODE_OTP_EXPIRED) -> {
                _state.update { it.copy(otpCode = "", error = error.toErrorText()) }
                beginCountdown(OTP_TTL_SECONDS)
            }

            // Blokir 30 menit setelah 5 kali gagal: input dan kirim ulang mati.
            error is ApiFailure.RateLimited && error.isOtpBlocked -> {
                _state.update {
                    it.copy(otpCode = "", isOtpInputBlocked = true, error = error.toErrorText())
                }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // Kode salah, jatah masih ada: hitung mundur tetap jalan.
            error.isBusinessCode(CODE_OTP_INVALID) ->
                _state.update { it.copy(otpCode = "", error = error.toErrorText()) }

            else -> handleFailure(error)
        }
    }

    private suspend fun handleResendOtpFailure(error: ApiFailure) {
        when {
            error is ApiFailure.RateLimited && error.isOtpBlocked -> {
                _state.update {
                    it.copy(otpCode = "", isOtpInputBlocked = true, error = error.toErrorText())
                }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // Kuota kirim ulang habis. Input tetap aktif: kode terakhir masih sah.
            error is ApiFailure.RateLimited -> {
                _state.update { it.copy(isOtpResendBlocked = true, error = error.toErrorText()) }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // SMS gagal berangkat, tapi kodenya tetap terbit dan sah. Tawarkan
            // kirim ulang alih-alih mengusir nasabah ke awal flow.
            error.isBusinessCode(CODE_OTP_DELIVERY_FAILED) -> {
                _state.update { it.copy(error = error.toErrorText()) }
                beginCountdown(0)
            }

            else -> handleFailure(error)
        }
    }

    // -- Hitung mundur OTP -----------------------------------------------------

    private var otpTickerJob: Job? = null

    /**
     * Mulai hitung mundur sampai `otp_expires_at`.
     *
     * Jam perangkat bisa bergeser, dan angka ini hanya tampilan — deadline
     * sebenarnya ditegakkan server. Karena itu selisihnya dibatasi ke umur OTP:
     * jam yang meleset jauh tidak boleh mematikan tombol kirim ulang selamanya.
     * Nilai yang tidak terbaca berarti tanpa hitung mundur, bukan hitung mundur
     * palsu — kirim ulang langsung tersedia.
     */
    private fun beginOtpWindow(expiresAt: String?) {
        beginCountdown(expiresAt?.secondsFromNow()?.coerceIn(0, OTP_TTL_SECONDS) ?: 0)
    }

    private fun beginCountdown(seconds: Int) {
        otpTickerJob?.cancel()
        _state.update { it.copy(otpCountdownSeconds = seconds) }

        if (seconds <= 0) {
            releaseOtpBlocks()
            return
        }

        otpTickerJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(TICK_MS)
                left -= 1
                _state.update { it.copy(otpCountdownSeconds = left) }
            }
            // Hitung mundur habis berarti blokir dan kuota kirim ulang ikut lepas.
            releaseOtpBlocks()
        }
    }

    private fun stopOtpCountdown() {
        otpTickerJob?.cancel()
        otpTickerJob = null
    }

    private fun releaseOtpBlocks() {
        _state.update { it.copy(isOtpInputBlocked = false, isOtpResendBlocked = false) }
    }

    private fun Int?.orRetryFallback(): Int = this?.takeIf { it > 0 } ?: RETRY_FALLBACK_SECONDS

    /** Sisa detik sampai [this] (RFC3339 UTC), atau null bila tidak terbaca. */
    private fun String.secondsFromNow(): Int? = runCatching {
        Duration.between(Instant.now(), Instant.parse(this)).seconds.toInt()
    }.getOrNull()

    // -- Step 7: biometrik -----------------------------------------------------

    private fun uploadBiometric(event: BukaRekeningEvent.BiometricCaptured) {
        launchWithLoading {
            val result = repository.uploadBiometric(
                facePhoto = event.facePhoto,
                livenessFrames = event.livenessFrames,
                meta = event.meta,
            )

            // Wajah dan frame bukti tidak boleh menetap di perangkat, apa pun
            // hasil unggahannya.
            CameraCapture.discard(event.facePhoto)
            event.livenessFrames.forEach(CameraCapture::discard)

            when (result) {
                is DataResult.Success -> {
                    _state.update { it.copy(biometric = result.value, isLivenessRunning = false) }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.VIDEO_CALL))
                }
                is DataResult.Failure -> {
                    _state.update { it.copy(isLivenessRunning = false) }
                    handleFailure(result.error)
                }
            }
        }
    }

    // -- Step 8: video call ----------------------------------------------------

    private fun joinQueue() {
        launchWithLoading {
            when (val result = repository.joinVideoCallQueue()) {
                is DataResult.Success -> _state.update { it.copy(queue = result.value) }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    /**
     * Hasil verifikasi video call ditulis CS lewat endpoint sisi mereka; client
     * hanya menandai selesai lalu meminta server memastikan step berikutnya.
     */
    private fun completeVideoCall(csName: String) {
        _state.update { it.copy(isVideoCallVerified = true, videoCallCsName = csName) }
        val sessionId = _state.value.sessionId ?: return
        launchWithLoading {
            when (val result = repository.getSession(sessionId)) {
                is DataResult.Success -> {
                    _state.update { it.copy(currentStep = result.value.currentStep) }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(result.value.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    // -- Step 9: kredensial ----------------------------------------------------

    private fun saveCredentials() {
        val current = _state.value
        if (!current.isCredentialValid()) {
            _state.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_credential_invalid))
            }
            return
        }

        launchWithLoading {
            when (val result = repository.saveCredentials(current.accessCode, current.pin)) {
                is DataResult.Success -> {
                    // Kredensial tidak disimpan di state setelah terkirim.
                    _state.update {
                        it.copy(
                            isCredentialsSaved = true,
                            accessCode = "",
                            confirmAccessCode = "",
                            pin = "",
                            confirmPin = "",
                        )
                    }
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.REVIEW))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    // -- Step 10-11: submit ----------------------------------------------------

    private fun submitApplication() {
        if (!_state.value.isAgreed) {
            _state.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_agreement_required))
            }
            return
        }

        launchWithLoading {
            val result = repository.submitApplication(
                agreementVersion = AGREEMENT_VERSION,
                idempotencyKey = idempotencyKey(),
            )
            when (result) {
                is DataResult.Success -> {
                    _state.update { it.copy(account = result.value) }
                    repository.clearLocalSession()
                    _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.COMPLETED))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    // -- Infrastruktur ---------------------------------------------------------

    private fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                block()
            } finally {
                // Tanpa finally, satu exception di block() mengunci layar di
                // keadaan loading selamanya.
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleFailure(error: ApiFailure) {
        if (error.isFatalForSession) {
            repository.clearLocalSession()
            _state.value = BukaRekeningFlowState(error = error.toErrorText())
            _sideEffect.send(BukaRekeningSideEffect.RestartFlow)
            return
        }

        _state.update { it.copy(error = error.toErrorText()) }

        // Step tidak sinkron: server yang menentukan posisi sebenarnya.
        if (error is ApiFailure.InvalidStep) {
            val sessionId = _state.value.sessionId ?: return
            val result = repository.getSession(sessionId)
            if (result is DataResult.Success) {
                _state.update { it.copy(currentStep = result.value.currentStep) }
                _sideEffect.send(BukaRekeningSideEffect.AdvanceTo(result.value.currentStep))
            }
        }
    }

    private fun emitMessage(text: ErrorText) {
        viewModelScope.launch { _sideEffect.send(BukaRekeningSideEffect.ShowMessage(text)) }
    }

    private var cachedIdempotencyKey: String? = null

    /** Dibuat sekali lalu dipakai ulang, supaya retry submit tidak membuat rekening kedua. */
    private fun idempotencyKey(): String =
        cachedIdempotencyKey ?: java.util.UUID.randomUUID().toString()
            .also { cachedIdempotencyKey = it }

    private companion object {
        /** Versi S&K dan persetujuan yang sedang tayang; harus ikut berubah saat dokumen diperbarui. */
        const val TNC_VERSION = "2026-09-01"
        const val AGREEMENT_VERSION = "2026-09-01"

        /** Umur OTP menurut kontrak; dipakai saat response tidak membawa `otp_expires_at`. */
        const val OTP_TTL_SECONDS = 300
        /**
         * `retry_after_seconds` bisa 0 kalau server gagal membaca sisa waktu.
         * Nol akan menghidupkan tombol seketika dan langsung kena 429 lagi.
         */
        const val RETRY_FALLBACK_SECONDS = 60
        const val TICK_MS = 1_000L

        const val CODE_OTP_INVALID = "OTP_INVALID"
        const val CODE_OTP_EXPIRED = "OTP_EXPIRED"
        const val CODE_OTP_DELIVERY_FAILED = "OTP_DELIVERY_FAILED"
    }
}

private fun ApiFailure.isBusinessCode(code: String): Boolean =
    this is ApiFailure.Business && this.code == code

/** Urutan putar tombol flash: AUTO, ON, OFF, kembali ke AUTO. */
private fun FlashMode.next(): FlashMode = when (this) {
    FlashMode.AUTO -> FlashMode.ON
    FlashMode.ON -> FlashMode.OFF
    FlashMode.OFF -> FlashMode.AUTO
}
