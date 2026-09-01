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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.home.BerandaScreen
import id.bca.bcamobile.ui.screen.home.BerandaUiState
import id.bca.bcamobile.ui.screen.home.QuickAction
import id.bca.bcamobile.ui.screen.mutasi.MutasiScreen
import id.bca.bcamobile.ui.screen.mutasi.MutasiUiState
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

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<GraphMain>(startDestination = Beranda) {

        composable<Beranda> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                BerandaScreen(
                    state = BerandaUiState(),
                    onToggleBalance = {},
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
                    onProfileClick = {},
                    onRetry = {},
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Mutasi> {
            MainScaffold(navController = navController, onScanClick = {}) { innerPadding ->
                MutasiScreen(
                    state = MutasiUiState(),
                    onAccountClick = {},
                    onPeriodSelected = {},
                    onCustomDateClick = { navController.navigate(RentangWaktu) },
                    onTransactionClick = {},
                    onLoadMore = {},
                    onRetry = {},
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.navigation_account))
                }
            }
        }

        composable<Transfer> {
            TransferScreen(
                state = TransferUiState(
                    recentTransfers = listOf(
                        RecentTransferItem("1", "Budi Santoso", "BCA - 0987 6543 21", 'B'),
                        RecentTransferItem("2", "PT. Aneka Tambang", "Mandiri - 123 456 789", 'A'),
                        RecentTransferItem("3", "Siti Aminah", "BNI - 555 444 333", 'S'),
                        RecentTransferItem("4", "Toko Buku Gramedia", "BCA - 111 222 333", 'T'),
                    ),
                ),
                onBackClick = { navController.popBackStack() },
                onToggleBalance = {},
                onCopyAccount = {},
                onSearchChange = {},
                onTransferTypeClick = { type ->
                    when (type) {
                        TransferType.ANTAR_REKENING -> navController.navigate(TransferAntarRekening)
                        else -> {}
                    }
                },
                onRecentTransferClick = {},
                onLihatSemua = {},
                onNotificationClick = {},
                onProfileClick = {},
            )
        }

        composable<TransferAntarRekening> {
            TransferAntarRekeningScreen(
                state = TransferAntarRekeningUiState(
                    currentStep = 1,
                    sourceAccountType = "Tahapan BCA",
                    sourceAccountNumber = "1234567890",
                    sourceBalance = "Rp 12.500.000",
                ),
                onBackClick = { navController.popBackStack() },
                onSourceAccountClick = {},
                onDestinationAccountChange = {},
                onContactsClick = {},
                onAmountChange = {},
                onNotesChange = {},
                onLanjutClick = {},
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
                        MainTab.BERANDA -> Beranda
                        MainTab.MUTASI -> Mutasi
                        MainTab.RIWAYAT -> Riwayat
                        MainTab.AKUN -> Akun
                    }
                    navController.navigate(route) {
                        popUpTo<Beranda> { saveState = true }
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
                        MainTab.BERANDA -> dest.hasRoute<Beranda>()
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