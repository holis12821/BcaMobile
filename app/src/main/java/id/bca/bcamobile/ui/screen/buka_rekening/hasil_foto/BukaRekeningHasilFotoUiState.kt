package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.INDONESIAN
import id.bca.bcamobile.ui.screen.buka_rekening.common.formatAccuracy
import id.bca.bcamobile.ui.screen.buka_rekening.common.groupNik
import id.bca.bcamobile.ui.screen.buka_rekening.common.joinNonBlank
import id.bca.bcamobile.ui.screen.buka_rekening.common.toKtpDate

data class BukaRekeningHasilFotoUiState(
    val isFotoValid: Boolean = true,
    val resolusiInfo: String = "",
    val ocrAccuracy: String = "",
    val nik: String = "",
    val namaLengkap: String = "",
    val tempatTanggalLahir: String = "",
    val alamat: String = "",
    val agama: String = "",
    val statusPerkawinan: String = "",
)

@Composable
fun BukaRekeningFlowState.toHasilFotoUiState(): BukaRekeningHasilFotoUiState {
    val data = ktpData ?: return BukaRekeningHasilFotoUiState(isFotoValid = false)
    val cornersVisible = ocr?.allCornersVisible ?: true
    val sudut = stringResource(
        if (cornersVisible) {
            R.string.buka_rekening_hasil_foto_sudut_presisi
        } else {
            R.string.buka_rekening_hasil_foto_sudut_terpotong
        },
    )
    return BukaRekeningHasilFotoUiState(
        isFotoValid = !needsRetake && (ocr == null || ocr.dukcapilMatch) &&
            ocr?.glareDetected != true && cornersVisible,
        resolusiInfo = if (captureResolution.isBlank()) {
            ""
        } else {
            stringResource(
                R.string.buka_rekening_hasil_foto_resolusi_format,
                captureResolution,
                sudut,
            )
        },
        ocrAccuracy = stringResource(
            R.string.buka_rekening_hasil_foto_akurasi_format,
            formatAccuracy(ktpAccuracy),
        ),
        nik = data.nik.groupNik(),
        namaLengkap = data.namaLengkap,
        tempatTanggalLahir = joinNonBlank(
            data.tempatLahir.uppercase(INDONESIAN),
            data.tanggalLahir.toKtpDate(),
        ),
        alamat = joinNonBlank(
            data.alamat,
            data.rtRw,
            data.kelurahan,
            data.kecamatan,
            data.kota,
        ).uppercase(INDONESIAN),
        agama = data.agama.uppercase(INDONESIAN),
        statusPerkawinan = data.statusPerkawinan.uppercase(INDONESIAN),
    )
}
