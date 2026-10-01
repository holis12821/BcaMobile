package id.bca.bcamobile.ui.screen.buka_rekening.ringkasan

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.formatAmount
import id.bca.bcamobile.ui.screen.buka_rekening.common.groupNik
import id.bca.bcamobile.ui.screen.buka_rekening.common.joinNonBlank
import id.bca.bcamobile.ui.screen.buka_rekening.common.toDisplayDate

data class RingkasanUiState(
    // Pilihan Rekening
    val produkRekening: String = "",
    val isPalingPopuler: Boolean = false,
    val mataUang: String = "",
    val setoranAwalMinimum: String = "",
    val fasilitasDigital: String = "",
    // Data Nasabah
    val namaLengkap: String = "",
    val nik: String = "",
    val isNikVerified: Boolean = false,
    val tempatTanggalLahir: String = "",
    val alamatKtp: String = "",
    val pekerjaan: String = "",
    val nomorHandphone: String = "",
    val isOtpVerified: Boolean = false,
    val alamatEmail: String = "",
    // Status Verifikasi
    val isOcrVerified: Boolean = false,
    val isBiometrikVerified: Boolean = false,
    val isVideoCallVerified: Boolean = false,
    val videoCallCsName: String = "",
    // Persetujuan
    val isAgreed: Boolean = false,
)

@Composable
fun BukaRekeningFlowState.toRingkasanUiState(): RingkasanUiState {
    val data = ktpData
    return RingkasanUiState(
        produkRekening = product?.name.orEmpty(),
        isPalingPopuler = selectedProductIndex == POPULAR_PRODUCT_INDEX,
        mataUang = product?.currency.orEmpty(),
        setoranAwalMinimum = product?.minInitialDeposit?.let {
            stringResource(R.string.buka_rekening_ringkasan_setoran_format, formatAmount(it))
        }.orEmpty(),
        fasilitasDigital = product?.features?.joinToString(", ").orEmpty(),
        namaLengkap = data?.namaLengkap.orEmpty(),
        nik = data?.nik?.groupNik().orEmpty(),
        isNikVerified = ocr?.dukcapilMatch == true,
        tempatTanggalLahir = data?.let {
            joinNonBlank(it.tempatLahir, it.tanggalLahir.toDisplayDate())
        }.orEmpty(),
        alamatKtp = data?.let { joinNonBlank(it.alamat, it.kota) }.orEmpty(),
        pekerjaan = stringResource(R.string.buka_rekening_pekerjaan_karyawan_swasta),
        nomorHandphone = otpSentTo,
        isOtpVerified = isOtpVerified,
        alamatEmail = "",
        isOcrVerified = isOcrVerified,
        isBiometrikVerified = isBiometrikVerified,
        isVideoCallVerified = isVideoCallVerified,
        videoCallCsName = videoCallCsName,
        isAgreed = isAgreed,
    )
}

// -- Format --------------------------------------------------------------------

private const val POPULAR_PRODUCT_INDEX = 0
