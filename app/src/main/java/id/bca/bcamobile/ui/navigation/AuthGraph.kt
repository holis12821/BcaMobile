package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.faceid.FaceIdScreen
import id.bca.bcamobile.ui.screen.faceid.FaceIdUiState
import id.bca.bcamobile.ui.screen.finger_print.TouchIdScreen
import id.bca.bcamobile.ui.screen.finger_print.TouchIdUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesViewModel
import id.bca.bcamobile.ui.screen.login.LoginScreen
import id.bca.bcamobile.ui.screen.login.LoginUiState

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthenticated: () -> Unit,
) {
    navigation<GraphAuth>(startDestination = Login) {

        composable<Login> {
            LoginScreen(
                state = LoginUiState(),
                onMbcaLoginClick = { navController.navigate(KodeAkses) },
                onFaceIdClick = { navController.navigate(FaceId) },
                onFingerprintClick = { navController.navigate(TouchId) },
                onBukaRekeningClick = { navController.navigate(BukaRekening) },
                onGantiKodeAksesClick = { navController.navigate(GantiKodeAkses) },
                onInfoBcaClick = {},
                onFlazzClick = {},
                onKlikBcaClick = {},
                onRetry = {},
            )
        }

        dialog<KodeAkses>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val viewModel: KodeAksesViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            KodeAksesScreen(
                state = state,
                onDigitClick = viewModel::onDigitClick,
                onDeleteClick = viewModel::onDeleteClick,
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = {
                    if (viewModel.submit()) {
                        onAuthenticated()
                    } else {
                        viewModel.showError()
                    }
                },
                onForgotClick = {},
            )
        }

        composable<FaceId> {
            FaceIdScreen(
                state = FaceIdUiState(),
                onBackClick = { navController.popBackStack() },
                onRetryClick = {},
                onCancelClick = { navController.popBackStack() },
            )
        }

        composable<TouchId> {
            TouchIdScreen(
                state = TouchIdUiState(),
                onFingerprintPress = {},
                onUseAccessCode = {
                    navController.navigate(KodeAkses) {
                        popUpTo<Login> { inclusive = false }
                    }
                },
                onRetryClick = {},
            )
        }

        composable<BukaRekening> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.navigation_open_account))
            }
        }

        composable<GantiKodeAkses> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.navigation_change_access_code))
            }
        }
    }
}