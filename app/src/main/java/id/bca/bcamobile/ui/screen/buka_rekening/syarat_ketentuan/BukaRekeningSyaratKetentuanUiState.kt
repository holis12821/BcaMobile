package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

/**
 * Layar S&K memicu pembuatan sesi, jadi ia perlu tahu keadaan panggilan itu —
 * bukan sekadar menampilkan teks.
 */
data class BukaRekeningSyaratKetentuanUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Composable
fun BukaRekeningFlowState.toSyaratKetentuanUiState(): BukaRekeningSyaratKetentuanUiState =
    BukaRekeningSyaratKetentuanUiState(
        isLoading = isLoading,
        error = error?.resolve(),
    )
