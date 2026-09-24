package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiScreen
import id.bca.bcamobile.ui.screen.ewallet.ConfirmEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.EWalletFlowEvent
import id.bca.bcamobile.ui.screen.ewallet.EWalletFlowViewModel
import id.bca.bcamobile.ui.screen.ewallet.TopUpEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.toBuktiUiState
import id.bca.bcamobile.ui.screen.ewallet.toConfirmUiState
import id.bca.bcamobile.ui.screen.ewallet.toPinUiState
import id.bca.bcamobile.ui.screen.ewallet.toTopUpUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen

fun NavGraphBuilder.eWalletGraph(navController: NavHostController) {
    navigation<GraphEWallet>(startDestination = EWalletPilih) {

        composable<EWalletPilih> {
            val viewModel = eWalletViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            // Perpindahan ke konfirmasi menunggu jawaban inquiry, bukan klik tombol.
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is EWalletFlowEvent.InquiryReady) {
                        navController.navigate(EWalletNominal)
                    }
                }
            }

            TopUpEWalletScreen(
                state = state.toTopUpUiState(),
                onBackClick = { navController.popBackStack() },
                onAccountClick = {},
                onWalletSelected = viewModel::onProviderSelected,
                onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                onContactPickerClick = {},
                onPresetAmountSelected = viewModel::onPresetSelected,
                onManualAmountChanged = viewModel::onManualAmountChanged,
                onContinueClick = viewModel::onContinue,
                onRetry = viewModel::load,
            )
        }

        composable<EWalletNominal> {
            val viewModel = eWalletViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            ConfirmEWalletScreen(
                state = state.toConfirmUiState(),
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(EWalletPin) },
                onRetry = {},
            )
        }

        composable<EWalletPin> {
            val viewModel = eWalletViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is EWalletFlowEvent.TopUpSucceeded) {
                        navController.navigate(EWalletBukti) {
                            popUpTo<EWalletPin> { inclusive = true }
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(R.string.navigation_input_transaction_pin),
                        onBackClick = { navController.popBackStack() },
                    )
                },
            ) { innerPadding ->
                KodeAksesScreen(
                    state = state.toPinUiState(),
                    onDigitClick = viewModel::onPinDigit,
                    onDeleteClick = viewModel::onPinDelete,
                    onCancelClick = { navController.popBackStack() },
                    onSubmitClick = viewModel::submitPin,
                    onForgotClick = {},
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<EWalletBukti> {
            val viewModel = eWalletViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            BuktiTransaksiScreen(
                state = state.toBuktiUiState(),
                onBackClick = {
                    navController.navigate(Home) {
                        popUpTo<GraphMain> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBagikanClick = {},
                onSimpanClick = {},
                onRetry = {},
            )
        }
    }
}

/**
 * Satu ViewModel untuk empat layar, di-scope ke entri graph e-wallet.
 * Instance ikut dibuang saat graph lepas dari back stack, sehingga PIN dan
 * data inquiry tidak tertinggal di memory.
 */
@Composable
private fun eWalletViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): EWalletFlowViewModel {
    // Dikunci ke entri layar ini sendiri, bukan currentBackStackEntry: saat flow
    // dipop, layar yang sedang keluar masih ter-compose selama animasi transisi.
    // Dengan key lama, remember dievaluasi ulang di saat itu dan
    // getBackStackEntry(GraphEWallet) melempar IllegalArgumentException.
    val owner = remember(entry) {
        runCatching { navController.getBackStackEntry(GraphEWallet) }.getOrDefault(entry)
    }
    return hiltViewModel(owner)
}
