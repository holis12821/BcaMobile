package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.maskedPhoneOrNull
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
        nomorTersamar = otpSentTo.ifBlank { maskedFormPhone(dataPribadi?.nomorHp) },
        kode = otpCode,
        detikTersisa = otpCountdownSeconds,
        isLoading = isLoading,
        isInputDiblokir = isOtpInputBlocked,
        isKirimUlangDiblokir = isOtpResendBlocked,
        error = error?.resolve(),
    )

/**
 * Nomor tujuan OTP saat server tidak mengirim `otp_sent_to`.
 *
 * `personal-data` yang dijawab `OTP_DELIVERY_FAILED` berstatus 503 tanpa `data`, jadi
 * tidak ada `otp_sent_to` walau langkahnya sudah maju ke OTP. Nomor yang ditampilkan
 * diambil dari form Data Pribadi — nomor yang sama yang dikirim ke server sebagai
 * tujuan OTP, bukan tebakan. Hanya kalau form itu pun tidak terbaca (mis. proses mati
 * lalu draf dilanjutkan) layar jatuh ke keterangan umum.
 */
@Composable
private fun maskedFormPhone(nomorHp: String?): String {
    val masked = nomorHp?.maskedPhoneOrNull()
        ?: return stringResource(R.string.buka_rekening_otp_nomor_tidak_tersedia)
    return stringResource(
        R.string.buka_rekening_otp_nomor_mask_format,
        masked.prefix,
        masked.suffix,
    )
}
