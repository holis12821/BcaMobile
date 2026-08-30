package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState
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
                onFaceIdClick = { /* BiometricPrompt — bukan route */ },
                onFingerprintClick = { /* BiometricPrompt — bukan route */ },
                onBukaRekeningClick = { navController.navigate(BukaRekening) },
                onGantiKodeAksesClick = { navController.navigate(GantiKodeAkses) },
                onInfoBcaClick = {},
                onFlazzClick = {},
                onKlikBcaClick = {},
                onRetry = {},
            )
        }

        dialog<KodeAkses> {
            KodeAksesScreen(
                state = KodeAksesUiState(),
                onDigitClick = {},
                onDeleteClick = {},
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = { onAuthenticated() },
                onForgotClick = {},
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