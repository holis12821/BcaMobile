package id.bca.bcamobile.ui.screen.biometric

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.faceid.FaceIdStatus
import id.bca.bcamobile.ui.screen.faceid.FaceIdUiState
import id.bca.bcamobile.ui.screen.finger_print.TouchIdStatus
import id.bca.bcamobile.ui.screen.finger_print.TouchIdUiState

/**
 * Layar Face ID tidak punya status diam — begitu dibuka, pemindaian dianggap
 * sudah berjalan. Karena itu IDLE dipetakan ke SCANNING.
 */
@Composable
fun BiometricLoginState.toFaceIdUiState(): FaceIdUiState = FaceIdUiState(
    status = when (status) {
        BiometricLoginStatus.IDLE, BiometricLoginStatus.SCANNING -> FaceIdStatus.SCANNING
        BiometricLoginStatus.SUCCESS -> FaceIdStatus.SUCCESS
        BiometricLoginStatus.FAILED -> FaceIdStatus.FAILED
    },
)

@Composable
fun BiometricLoginState.toTouchIdUiState(): TouchIdUiState = TouchIdUiState(
    status = when (status) {
        BiometricLoginStatus.IDLE -> TouchIdStatus.IDLE
        BiometricLoginStatus.SCANNING -> TouchIdStatus.SCANNING
        BiometricLoginStatus.SUCCESS -> TouchIdStatus.SUCCESS
        BiometricLoginStatus.FAILED -> TouchIdStatus.FAILED
    },
)
