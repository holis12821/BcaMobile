package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

data class BukaRekeningPilihJenisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val jenisRekeningList: List<JenisRekening> = emptyList(),
)

@Composable
fun BukaRekeningFlowState.toPilihJenisUiState(): BukaRekeningPilihJenisUiState =
    BukaRekeningPilihJenisUiState(
        isLoading = isLoading,
        error = error?.resolve(),
        jenisRekeningList = defaultJenisRekeningList(),
    )
