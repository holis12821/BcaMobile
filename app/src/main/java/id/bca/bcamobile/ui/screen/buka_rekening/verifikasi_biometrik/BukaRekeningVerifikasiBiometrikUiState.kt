package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import androidx.compose.runtime.Composable
import id.bca.bcamobile.core.liveness.LivenessDetector
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState

data class VerifikasiBiometrikUiState(
    val faceDetected: Boolean = false,
    val precisionPercent: Int = 98,
    val currentAction: Int = 2,
    val totalActions: Int = 3,
)

fun BukaRekeningFlowState.toVerifikasiBiometrikUiState(): VerifikasiBiometrikUiState =
    VerifikasiBiometrikUiState(
        faceDetected = liveness.faceDetected,
        precisionPercent = liveness.precisionPercent,
        currentAction = liveness.completedActions,
        totalActions = LivenessDetector.TOTAL_CHALLENGES,
    )
