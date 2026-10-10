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
import java.io.File

data class BukaRekeningHasilFotoUiState(
    /**
     * Foto e-KTP yang baru diambil, untuk ditampilkan di layar ini.
     *
     * Layar ini sebelumnya menampilkan field hasil ekstraksi **tanpa fotonya
     * sama sekali** — hanya ikon dari drawable. Dari sisi nasabah itu terbaca
     * sebagai "fotonya tidak terlampir", dan tidak ada cara memeriksa apakah
     * yang terbaca memang berasal dari kartu yang difoto.
     *
     * `null` saat melanjutkan draf: fotonya sudah di server dan salinan lokalnya
     * sudah dihapus, jadi pratinjaunya memang tidak ada.
     */
    val ktpPhoto: File? = null,
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
    val data = ktpData
        ?: return BukaRekeningHasilFotoUiState(ktpPhoto = ktpPhoto, isFotoValid = false)
    val cornersVisible = ocr?.allCornersVisible ?: true
    val sudut = stringResource(
        if (cornersVisible) {
            R.string.buka_rekening_hasil_foto_sudut_presisi
        } else {
            R.string.buka_rekening_hasil_foto_sudut_terpotong
        },
    )
    return BukaRekeningHasilFotoUiState(
        ktpPhoto = ktpPhoto,
        // Registri yang tidak dihubungi bukan alasan memblokir: server sudah
        // menolak kartu yang tidak lolos validasinya sendiri dengan OCR_NOT_KTP,
        // jadi hasil yang sampai ke sini sudah lulus. Yang memblokir hanyalah
        // pencocokan yang dijalankan DAN gagal.
        isFotoValid = !needsRetake &&
            !(ocr?.dukcapilChecked == true && !ocr.dukcapilMatch) &&
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
