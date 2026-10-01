package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

/**
 * State layar verifikasi OTP.
 *
 * Kode OTP hidup di sini hanya selama layar tampil dan tidak pernah ditulis ke
 * disk — aturan PII #5 di `CLAUDE.md`.
 */
data class BukaRekeningOtpUiState(
    val nomorTersamar: String = "",
    val kode: String = "",
    val detikTersisa: Int = 0,
    val langkah: Int = 8,
    val totalLangkah: Int = 14,
    val isLoading: Boolean = false,
    /** `OTP_BLOCKED`: sesi diblokir, input dan kirim ulang sama-sama mati. */
    val isInputDiblokir: Boolean = false,
    /** Kuota kirim ulang habis: hanya tombolnya mati, kode terakhir masih sah. */
    val isKirimUlangDiblokir: Boolean = false,
    val error: String? = null,
) {
    val isInputAktif: Boolean get() = !isLoading && !isInputDiblokir

    val isKirimUlangAktif: Boolean
        get() = detikTersisa <= 0 && !isLoading && !isInputDiblokir && !isKirimUlangDiblokir

    /**
     * Kode yang belum enam digit tidak boleh dikirim: server menjawabnya
     * `VALIDATION_ERROR`, dan itu bukan kesalahan nasabah.
     */
    val isVerifikasiAktif: Boolean get() = kode.length == OTP_LENGTH && isInputAktif
}

@Composable
fun BukaRekeningFlowState.toOtpUiState(): BukaRekeningOtpUiState =
    BukaRekeningOtpUiState(
        nomorTersamar = otpSentTo,
        kode = otpCode,
        detikTersisa = otpCountdownSeconds,
        isLoading = isLoading,
        isInputDiblokir = isOtpInputBlocked,
        isKirimUlangDiblokir = isOtpResendBlocked,
        error = error?.resolve(),
    )
