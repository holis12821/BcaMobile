package id.bca.bcamobile.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.transfer.toTransferUiState
import id.bca.bcamobile.ui.screen.transfer.toPinUiState
import id.bca.bcamobile.ui.screen.transfer.toBuktiUiState
import id.bca.bcamobile.ui.screen.transfer.toAntarRekeningUiState
import id.bca.bcamobile.ui.screen.transfer.TransferFlowViewModel
import id.bca.bcamobile.ui.screen.transfer.TransferFlowEvent
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiScreen
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.screen.akun.AkunEvent
import id.bca.bcamobile.ui.screen.akun.AkunScreen
import id.bca.bcamobile.ui.screen.akun.AkunViewModel
import id.bca.bcamobile.ui.screen.akun.AkunUiState
import id.bca.bcamobile.ui.screen.home.BerandaViewModel
import id.bca.bcamobile.ui.screen.home.HomeScreen
import id.bca.bcamobile.ui.screen.home.BerandaUiState
import id.bca.bcamobile.ui.screen.home.QuickAction
import id.bca.bcamobile.ui.screen.mutasi.MutasiScreen
import id.bca.bcamobile.ui.screen.mutasi.MutasiViewModel
import id.bca.bcamobile.ui.screen.transfer.RecentTransferItem
import id.bca.bcamobile.ui.screen.transfer.TransferAntarRekeningScreen
import id.bca.bcamobile.ui.screen.transfer.TransferAntarRekeningUiState
import id.bca.bcamobile.ui.screen.transfer.TransferScreen
import id.bca.bcamobile.ui.screen.transfer.TransferType
import id.bca.bcamobile.ui.screen.transfer.TransferUiState
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.Spacing

// ── Tab Definition ──────────────────────────────────────────────────────

enum class MainTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    BERANDA(R.string.nav_beranda, Icons.Default.Home),
    MUTASI(R.string.nav_mutasi, Icons.Default.DateRange),
    RIWAYAT(R.string.nav_riwayat, Icons.Default.Refresh),
    AKUN(R.string.nav_akun, Icons.Default.AccountCircle),
}

private const val LEFT_TAB_COUNT = 2

// ── Graph ───────────────────────────────────────────────────────────────

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    onLogout: () -> Unit,
) {
    navigation<GraphMain>(startDestination = Home) {

        composable<Home> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                val viewModel: BerandaViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                HomeScreen(
                    state = state,
                    onToggleBalance = viewModel::onToggleBalance,
                    onIsiSaldo = {},
                    onMutasi = { navController.navigate(Mutasi) },
                    onQuickAction = { action ->
                        when (action) {
                            QuickAction.TRANSFER -> navController.navigate(Transfer)
                            QuickAction.E_WALLET -> navController.navigate(EWalletPilih)
                            else -> {}
                        }
                    },
                    onPromoClick = {},
                    onLihatSemuaPromo = {},
                    onNotificationClick = {},
                    onProfileClick = { navController.navigate(Akun) },
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Mutasi> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                val viewModel: MutasiViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                MutasiScreen(
                    state = state,
                    onAccountClick = {},
                    onPeriodSelected = viewModel::onPeriodSelected,
                    onCustomDateClick = { navController.navigate(RentangWaktu) },
                    onTransactionClick = {},
                    onLoadMore = viewModel::onLoadMore,
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Riwayat> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.navigation_history))
                }
            }
        }

        composable<Akun> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                val viewModel: AkunViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                // Navigasi keluar dikendalikan SessionState, bukan dipanggil layar.
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            AkunEvent.LoggedOut -> onLogout()
                        }
                    }
                }

                AkunScreen(
                    state = state,
                    onNotificationClick = {},
                    onProfileClick = {},
                    onLihatProfilClick = {},
                    onMenuItemClick = {},
                    onBiometricToggle = viewModel::onBiometricToggle,
                    onKeluarClick = viewModel::onLogout,
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Transfer> {
            val viewModel = transferViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            TransferScreen(
                state = state.toTransferUiState(),
                onBackClick = { navController.popBackStack() },
                onToggleBalance = viewModel::onToggleBalance,
                onCopyAccount = {},
                onSearchChange = viewModel::onSearchChange,
                onTransferTypeClick = { type ->
                    when (type) {
                        TransferType.ANTAR_REKENING -> navController.navigate(TransferAntarRekening)
                        else -> {}
                    }
                },
                onRecentTransferClick = { recent ->
                    viewModel.onRecentSelected(recent.id)
                    navController.navigate(TransferAntarRekening)
                },
                onLihatSemua = {},
                onNotificationClick = {},
                onProfileClick = {},
            )
        }

        composable<TransferAntarRekening> {
            val viewModel = transferViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            // Lanjut menunggu jawaban inquiry, bukan langsung berpindah layar.
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is TransferFlowEvent.InquiryReady) {
                        navController.navigate(TransferPin)
                    }
                }
            }

            TransferAntarRekeningScreen(
                state = state.toAntarRekeningUiState(),
                onBackClick = { navController.popBackStack() },
                onSourceAccountClick = {},
                onDestinationAccountChange = viewModel::onDestinationChange,
                onContactsClick = {},
                onAmountChange = viewModel::onAmountChange,
                onNotesChange = viewModel::onNotesChange,
                onLanjutClick = viewModel::onContinue,
            )
        }

        composable<TransferPin> {
            val viewModel = transferViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is TransferFlowEvent.TransferSucceeded) {
                        navController.navigate(TransferBukti) {
                            popUpTo<TransferPin> { inclusive = true }
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

        composable<TransferBukti> {
            val viewModel = transferViewModel(navController, it)
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

        composable<RentangWaktu> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.navigation_date_range))
            }
        }

        eWalletGraph(navController)
    }
}

// ── Main Scaffold (tab screens only) ────────────────────────────────────

@Composable
private fun MainScaffold(
    navController: NavHostController,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomBar(
                currentDestination = currentDestination,
                onTabSelected = { tab ->
                    val route: Any = when (tab) {
                        MainTab.BERANDA -> Home
                        MainTab.MUTASI -> Mutasi
                        MainTab.RIWAYAT -> Riwayat
                        MainTab.AKUN -> Akun
                    }
                    navController.navigate(route) {
                        popUpTo<Home> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onScanClick = onScanClick,
            )
        },
        modifier = modifier,
        content = content,
    )
}

// ── Bottom Bar with FAB ─────────────────────────────────────────────────

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (MainTab) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s6),
    ) {
        NavigationBar(
            containerColor = AppColor.Neutral100,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MainTab.entries.forEachIndexed { index, tab ->
                if (index == LEFT_TAB_COUNT) {
                    Spacer(Modifier.weight(1f))
                }

                val isSelected = currentDestination?.let { dest ->
                    when (tab) {
                        MainTab.BERANDA -> dest.hasRoute<Home>()
                        MainTab.MUTASI -> dest.hasRoute<Mutasi>()
                        MainTab.RIWAYAT -> dest.hasRoute<Riwayat>()
                        MainTab.AKUN -> dest.hasRoute<Akun>()
                    }
                } ?: false

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = AppColor.Neutral100,
                shape = AppShape.Full,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_scan),
                    contentDescription = stringResource(R.string.cd_scan_qr),
                )
            }
            Text(
                text = stringResource(R.string.nav_qris),
                modifier = Modifier.padding(top = Spacing.s1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Satu ViewModel untuk empat layar transfer, di-scope ke entri route [Transfer]
 * supaya inquiry dan PIN tidak hidup lebih lama dari alurnya.
 */
@Composable
private fun transferViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): TransferFlowViewModel {
    // Dikunci ke entri layar ini sendiri, bukan currentBackStackEntry: saat flow
    // dipop, layar yang sedang keluar masih ter-compose selama animasi transisi.
    // Dengan key lama, remember dievaluasi ulang di saat itu dan
    // getBackStackEntry(Transfer) melempar IllegalArgumentException.
    val owner = remember(entry) {
        runCatching { navController.getBackStackEntry(Transfer) }.getOrDefault(entry)
    }
    return hiltViewModel(owner)
}
