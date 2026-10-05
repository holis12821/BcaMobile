package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

import androidx.compose.runtime.Composable
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.TncDocument
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

/**
 * Keadaan muatan dokumen S&K, milik ViewModel layar ini sendiri.
 *
 * Sengaja **tidak** menumpang [BukaRekeningFlowState]: dokumen ini hanya hidup selama
 * layar S&K tampil dan tidak dibaca langkah mana pun sesudahnya, jadi menaruhnya di store
 * bersama hanya memperpanjang umurnya tanpa pembaca.
 */
data class TncLoadState(
    val document: TncDocument? = null,
    val isLoading: Boolean = false,
    val error: ErrorText? = null,
)

/**
 * Layar S&K memicu pembuatan sesi, jadi ia perlu tahu keadaan panggilan itu —
 * bukan sekadar menampilkan teks.
 *
 * Sejak teksnya datang dari server, layar punya tiga keadaan yang dulu tidak mungkin ada:
 * sedang memuat, gagal memuat, dan berhasil. Dua panggilan yang berbeda dipisah
 * penandanya ([isLoadingDocument] untuk `GET tnc`, [isCreatingSession] untuk
 * `POST sessions`) karena perilaku layarnya berbeda: yang pertama mengosongkan area pasal,
 * yang kedua hanya mematikan tombol.
 */
data class BukaRekeningSyaratKetentuanUiState(
    val document: TncDocument? = null,
    val isLoadingDocument: Boolean = false,
    val documentError: String? = null,
    val isCreatingSession: Boolean = false,
    val error: String? = null,
) {
    /**
     * Dokumen layak disetujui.
     *
     * `document != null` wajib ikut: tanpa itu sesi bisa lahir dengan
     * `accepted_tnc_version` kosong, dan backend menolaknya `400 VALIDATION_ERROR` —
     * error yang benar, tapi muncul di tempat yang membingungkan.
     *
     * `isActive` juga diperiksa karena dokumen yang sudah dicabut pasti ditolak `409`;
     * menawarkan tombolnya hanya mengantar nasabah ke kegagalan.
     */
    val isAgreeAllowed: Boolean
        get() = document != null && document.isActive && !isLoadingDocument && !isCreatingSession
}

@Composable
fun BukaRekeningFlowState.toSyaratKetentuanUiState(
    tnc: TncLoadState,
): BukaRekeningSyaratKetentuanUiState =
    BukaRekeningSyaratKetentuanUiState(
        document = tnc.document,
        isLoadingDocument = tnc.isLoading,
        documentError = tnc.error?.resolve(),
        isCreatingSession = isLoading,
        error = error?.resolve(),
    )
