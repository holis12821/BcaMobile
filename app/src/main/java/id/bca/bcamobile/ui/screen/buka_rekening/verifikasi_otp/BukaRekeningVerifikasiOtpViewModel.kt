package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.toErrorText
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class BukaRekeningVerifikasiOtpViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    init {
        // Jendela hitung mundur dibuka dari `otp_expires_at` yang dicatat langkah
        // sebelumnya. Dihitung di sini, bukan di layar Data Pribadi, supaya
        // tickernya mati bersama layar ini.
        beginOtpWindow(store.current.otpExpiresAt)
    }

    fun onEvent(event: VerifikasiOtpEvent) {
        when (event) {
            is VerifikasiOtpEvent.OtpCodeChanged -> onOtpCodeChanged(event.code)
            VerifikasiOtpEvent.OtpSubmitted -> verifyOtp()
            VerifikasiOtpEvent.OtpResendRequested -> resendOtp()
            VerifikasiOtpEvent.OtpCodeCleared -> store.update { it.copy(otpCode = "") }
        }
    }

    private fun onOtpCodeChanged(code: String) {
        // Papan ketik dan autofill SMS bisa membawa spasi atau teks lain.
        val digits = code.filter(Char::isDigit).take(OTP_LENGTH)
        store.update { it.copy(otpCode = digits, error = null) }
    }

    /**
     * Request ditahan kalau kodenya belum enam digit: server menjawab
     * `VALIDATION_ERROR`, dan itu kesalahan client, bukan kesalahan nasabah.
     */
    private fun verifyOtp() {
        val current = store.current
        val code = current.otpCode
        if (current.isOtpInputBlocked || code.length != OTP_LENGTH) return

        launchWithLoading {
            when (val result = repository.verifyOtp(code)) {
                is DataResult.Success -> {
                    stopOtpCountdown()
                    store.update {
                        it.copy(isOtpVerified = true, otpCode = "", otpCountdownSeconds = 0)
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(result.value.currentStep))
                }
                is DataResult.Failure -> handleVerifyOtpFailure(result.error)
            }
        }
    }

    private fun resendOtp() {
        val current = store.current
        // Kirim ulang bukan jalan memutar blokir, dan jatahnya hanya 3 per jam.
        if (current.isOtpInputBlocked || current.isOtpResendBlocked) return
        if (current.otpCountdownSeconds > 0) return

        launchWithLoading {
            when (val result = repository.resendOtp()) {
                is DataResult.Success -> {
                    val challenge = result.value
                    // Kode lama mati seketika; field input ikut dikosongkan.
                    store.update {
                        it.copy(
                            otpSentTo = challenge.otpSentTo,
                            otpExpiresAt = challenge.otpExpiresAt,
                            otpCode = "",
                        )
                    }
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
                store.update { it.copy(otpCode = "", error = error.toErrorText()) }
                beginCountdown(OTP_TTL_SECONDS)
            }

            // Blokir 30 menit setelah 5 kali gagal: input dan kirim ulang mati.
            error is ApiFailure.RateLimited && error.isOtpBlocked -> {
                store.update {
                    it.copy(otpCode = "", isOtpInputBlocked = true, error = error.toErrorText())
                }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // Kode salah, jatah masih ada: hitung mundur tetap jalan.
            error.isBusinessCode(CODE_OTP_INVALID) ->
                store.update { it.copy(otpCode = "", error = error.toErrorText()) }

            else -> handleFailure(error)
        }
    }

    private suspend fun handleResendOtpFailure(error: ApiFailure) {
        when {
            error is ApiFailure.RateLimited && error.isOtpBlocked -> {
                store.update {
                    it.copy(otpCode = "", isOtpInputBlocked = true, error = error.toErrorText())
                }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // Kuota kirim ulang habis. Input tetap aktif: kode terakhir masih sah.
            error is ApiFailure.RateLimited -> {
                store.update { it.copy(isOtpResendBlocked = true, error = error.toErrorText()) }
                beginCountdown(error.retryAfterSeconds.orRetryFallback())
            }

            // SMS gagal berangkat, tapi kodenya tetap terbit dan sah. Tawarkan
            // kirim ulang alih-alih mengusir nasabah ke awal flow.
            error.isBusinessCode(CODE_OTP_DELIVERY_FAILED) -> {
                store.update { it.copy(error = error.toErrorText()) }
                beginCountdown(0)
            }

            else -> handleFailure(error)
        }
    }

    // -- Hitung mundur ---------------------------------------------------------

    private var otpTickerJob: Job? = null

    /**
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
        store.update { it.copy(otpCountdownSeconds = seconds) }

        if (seconds <= 0) {
            releaseOtpBlocks()
            return
        }

        otpTickerJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(TICK_MS)
                left -= 1
                store.update { it.copy(otpCountdownSeconds = left) }
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
        store.update { it.copy(isOtpInputBlocked = false, isOtpResendBlocked = false) }
    }

    private fun Int?.orRetryFallback(): Int = this?.takeIf { it > 0 } ?: RETRY_FALLBACK_SECONDS

    /** Sisa detik sampai [this] (RFC3339 UTC), atau null bila tidak terbaca. */
    private fun String.secondsFromNow(): Int? = runCatching {
        Duration.between(Instant.now(), Instant.parse(this)).seconds.toInt()
    }.getOrNull()

    private fun ApiFailure.isBusinessCode(code: String): Boolean =
        this is ApiFailure.Business && this.code == code

    private companion object {
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
