package id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.FlashMode

data class BukaRekeningKameraFotoUiState(
    val isAutoCaptureEnabled: Boolean = true,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isDetecting: Boolean = true,
)

fun BukaRekeningFlowState.toKameraFotoUiState(): BukaRekeningKameraFotoUiState =
    BukaRekeningKameraFotoUiState(
        isAutoCaptureEnabled = isAutoCaptureEnabled,
        flashMode = flashMode,
        isDetecting = isProcessingPhoto || isAutoCaptureEnabled,
    )
