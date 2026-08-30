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
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.ewallet.ConfirmEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.ConfirmEWalletUiState
import id.bca.bcamobile.ui.screen.ewallet.TopUpEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.TopUpEWalletUiState

fun NavGraphBuilder.eWalletGraph(navController: NavHostController) {
    navigation<GraphEWallet>(startDestination = EWalletPilih) {

        composable<EWalletPilih> {
            TopUpEWalletScreen(
                state = TopUpEWalletUiState(),
                onBackClick = { navController.popBackStack() },
                onAccountClick = {},
                onWalletSelected = {},
                onPhoneNumberChanged = {},
                onContactPickerClick = {},
                onPresetAmountSelected = {},
                onManualAmountChanged = {},
                onContinueClick = { navController.navigate(EWalletNominal) },
                onRetry = {},
            )
        }

        composable<EWalletNominal> {
            ConfirmEWalletScreen(
                state = ConfirmEWalletUiState(),
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(EWalletPin) },
                onRetry = {},
            )
        }

        composable<EWalletPin> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.navigation_input_transaction_pin))
            }
        }

        composable<EWalletBukti> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.navigation_transaction_receipt))
            }
        }
    }
}