package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiScreen
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiUiState
import id.bca.bcamobile.ui.screen.ewallet.ConfirmEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.ConfirmEWalletUiState
import id.bca.bcamobile.ui.screen.ewallet.TopUpEWalletScreen
import id.bca.bcamobile.ui.screen.ewallet.TopUpEWalletUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesViewModel

fun NavGraphBuilder.eWalletGraph(navController: NavHostController) {
    navigation<GraphEWallet>(startDestination = EWalletPilih) {

        composable<EWalletPilih> {
            TopUpEWalletScreen(
                state = remember { TopUpEWalletUiState() },
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
                state = remember { ConfirmEWalletUiState() },
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(EWalletPin) },
                onRetry = {},
            )
        }

        composable<EWalletPin> {
            val viewModel: KodeAksesViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(R.string.navigation_input_transaction_pin),
                        onBackClick = { navController.popBackStack() },
                    )
                },
            ) { innerPadding ->
                KodeAksesScreen(
                    state = state,
                    onDigitClick = viewModel::onDigitClick,
                    onDeleteClick = viewModel::onDeleteClick,
                    onCancelClick = { navController.popBackStack() },
                    onSubmitClick = {
                        if (viewModel.submit()) {
                            navController.navigate(EWalletBukti) {
                                popUpTo<GraphEWallet> { inclusive = true }
                            }
                        } else {
                            viewModel.showError()
                        }
                    },
                    onForgotClick = {},
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<EWalletBukti> {
            BuktiTransaksiScreen(
                state = remember {
                    BuktiTransaksiUiState(
                        tanggal = "24 Okt 2023, 14:30 WIB",
                        noReferensi = "TRX9876543210",
                        sumberRekening = "Tahapan BCA (1234 5678)",
                        namaPengirim = "Budi Santoso",
                        nomorTujuan = "GoPay - 0812 3456 7890",
                        namaTujuan = "Top Up e-Wallet",
                        jenisTransaksi = "Top Up e-Wallet",
                        nominal = "Rp 100.000",
                        biayaAdmin = "Rp 1.000",
                        total = "Rp 101.000",
                    )
                },
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