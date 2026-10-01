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
import androidx.compose.ui.platform.LocalContext
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
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import id.bca.bcamobile.ui.components.LocalSnackbarHostState
import id.bca.bcamobile.ui.screen.akun.AkunMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.bca.bcamobile.ui.screen.rentang.RentangPilihan
import id.bca.bcamobile.ui.screen.rentang.RentangWaktuScreen
import id.bca.bcamobile.ui.screen.rentang.RentangWaktuUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import id.bca.bcamobile.ui.screen.ubah_kode_akses.UbahKodeAksesScreen
import id.bca.bcamobile.ui.screen.ubah_kode_akses.UbahKodeAksesViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import id.bca.bcamobile.ui.screen.rekening.RekeningKartuScreen
import id.bca.bcamobile.ui.screen.rekening.RekeningKartuViewModel
import id.bca.bcamobile.ui.screen.limit.AturLimitEvent
import id.bca.bcamobile.ui.screen.limit.AturLimitScreen
import id.bca.bcamobile.ui.screen.limit.AturLimitViewModel
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiEvent
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiScreen
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiViewModel
import id.bca.bcamobile.ui.screen.notifikasi.NotifikasiScreen
import id.bca.bcamobile.ui.screen.notifikasi.NotifikasiViewModel
import id.bca.bcamobile.ui.screen.riwayat.RiwayatScreen
import id.bca.bcamobile.ui.screen.riwayat.RiwayatPeriod
import id.bca.bcamobile.ui.screen.riwayat.RiwayatViewModel
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.screen.akun.AkunEvent
import id.bca.bcamobile.ui.screen.akun.AkunScreen
import id.bca.bcamobile.ui.screen.akun.AkunViewModel
import id.bca.bcamobile.ui.screen.akun.AksiKartuDialog
import id.bca.bcamobile.ui.screen.bantuan.HubungiCsScreen
import id.bca.bcamobile.ui.screen.bantuan.HubungiCsViewModel
import id.bca.bcamobile.ui.screen.bantuan.PusatBantuanScreen
import id.bca.bcamobile.ui.screen.bantuan.PusatBantuanViewModel
import id.bca.bcamobile.ui.screen.bantuan.dial
import id.bca.bcamobile.ui.screen.bantuan.openLink
import id.bca.bcamobile.ui.screen.bantuan.openWhatsApp
import id.bca.bcamobile.ui.screen.bantuan.sendEmail
import id.bca.bcamobile.ui.screen.akun.AkunUiState
import id.bca.bcamobile.ui.screen.home.BerandaViewModel
import id.bca.bcamobile.ui.screen.home.HomeScreen
import id.bca.bcamobile.ui.screen.home.BerandaUiState
import id.bca.bcamobile.ui.screen.home.QuickAction
import id.bca.bcamobile.ui.screen.mutasi.MutasiPeriod
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
            MainScaffold(navController = navController, onScanClick = { navController.navigate(GraphQris) }) { innerPadding ->
                val viewModel: BerandaViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                HomeScreen(
                    state = state,
                    onToggleBalance = viewModel::onToggleBalance,
                    onIsiSaldo = {},
                    onMutasi = { navController.navigate(Mutasi) },
                    onQuickAction = { action ->
                        when (action) {
                            // m-Info = informasi rekening: saldo per rekening.
                            QuickAction.M_INFO -> navController.navigate(RekeningKartu)
                            QuickAction.TRANSFER -> navController.navigate(Transfer)
                            QuickAction.E_WALLET -> navController.navigate(EWalletPilih)
                            else -> {}
                        }
                    },
                    onPromoClick = {},
                    onLihatSemuaPromo = {},
                    onNotificationClick = { navController.navigate(Notifikasi) },
                    onProfileClick = { navController.navigate(Akun) },
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Mutasi> { entry ->
            MainScaffold(navController = navController, onScanClick = { navController.navigate(GraphQris) }) { innerPadding ->
                val viewModel: MutasiViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                // Hasil layar Rentang Waktu dikembalikan lewat SavedStateHandle entri
                // ini, bukan lewat ViewModel bersama: Mutasi adalah tab, dan layar
                // rentangnya hanya dibuka sesaat di atasnya.
                val periodResult by entry.savedStateHandle
                    .getStateFlow<String?>(RENTANG_RESULT_KEY, null)
                    .collectAsState()

                LaunchedEffect(periodResult) {
                    val value = periodResult ?: return@LaunchedEffect
                    entry.savedStateHandle[RENTANG_RESULT_KEY] = null
                    val parts = value.split(RENTANG_RESULT_SEPARATOR)
                    if (parts.size == 3 && parts[0] == RENTANG_CUSTOM) {
                        viewModel.onCustomRangeSelected(parts[1], parts[2])
                    } else {
                        MutasiPeriod.entries
                            .firstOrNull { period -> period.name == parts.firstOrNull() }
                            ?.let(viewModel::onPeriodSelected)
                    }
                }

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

        composable<Riwayat> { entry ->
            MainScaffold(navController = navController, onScanClick = { navController.navigate(GraphQris) }) { innerPadding ->
                val viewModel: RiwayatViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                // Sama seperti Mutasi: layar Rentang Waktu dibuka sesaat di atas
                // tab ini dan mengembalikan pilihannya lewat SavedStateHandle
                // entri ini, bukan lewat ViewModel bersama.
                val periodResult by entry.savedStateHandle
                    .getStateFlow<String?>(RENTANG_RESULT_KEY, null)
                    .collectAsState()

                LaunchedEffect(periodResult) {
                    val value = periodResult ?: return@LaunchedEffect
                    entry.savedStateHandle[RENTANG_RESULT_KEY] = null
                    val parts = value.split(RENTANG_RESULT_SEPARATOR)
                    if (parts.size == 3 && parts[0] == RENTANG_CUSTOM) {
                        viewModel.onCustomRangeSelected(parts[1], parts[2])
                    } else {
                        RiwayatPeriod.entries
                            .firstOrNull { period -> period.name == parts.firstOrNull() }
                            ?.let(viewModel::onPeriodSelected)
                    }
                }

                RiwayatScreen(
                    state = state,
                    onFilterSelected = viewModel::onFilterSelected,
                    onPeriodSelected = viewModel::onPeriodSelected,
                    onCustomDateClick = { navController.navigate(RentangWaktu) },
                    // Struk ditarik ulang dari server, bukan dirakit dari baris daftar.
                    onItemClick = { item -> navController.navigate(BuktiTransaksi(item.id)) },
                    onLoadMore = viewModel::onLoadMore,
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<Notifikasi> {
            val viewModel: NotifikasiViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            NotifikasiScreen(
                state = state,
                onTabSelected = viewModel::onTabSelected,
                onItemClick = viewModel::onItemOpened,
                onTandaiSemuaClick = viewModel::onTandaiSemuaClick,
                onLoadMore = viewModel::onLoadMore,
                onBackClick = { navController.popBackStack() },
                onRetry = viewModel::load,
            )
        }

        composable<BuktiTransaksi> {
            val viewModel: BuktiTransaksiViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            val snackbarHostState = LocalSnackbarHostState.current

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is BuktiTransaksiEvent.SaveFinished ->
                            snackbarHostState.showSnackbar(event.message)
                    }
                }
            }

            BuktiTransaksiScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onBagikanClick = {},
                onSimpanClick = viewModel::onSimpanClick,
                onRetry = viewModel::load,
            )
        }

        composable<Akun> {
            MainScaffold(navController = navController, onScanClick = { navController.navigate(GraphQris) }) { innerPadding ->
                val viewModel: AkunViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()

                val snackbarHostState = LocalSnackbarHostState.current
                val scope = rememberCoroutineScope()
                val belumTersedia = stringResource(R.string.profil_menu_belum_tersedia)

                // Navigasi keluar dikendalikan SessionState, bukan dipanggil layar.
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            AkunEvent.LoggedOut -> onLogout()

                            // Verifikasi PIN diwajibkan server untuk blokir dan
                            // ganti kartu, jadi dialognya bagian dari alur.
                            AkunEvent.PinRequired -> navController.navigate(KartuPin)

                            is AkunEvent.CardActionDone -> {
                                // Dua dialog ditutup sekaligus: PIN di atas,
                                // pemilih alasan di bawahnya.
                                navController.popBackStack(KartuAksi, inclusive = true)
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                    }
                }

                AkunScreen(
                    state = state,
                    onNotificationClick = { navController.navigate(Notifikasi) },
                    // Halaman ini sendiri sudah halaman profil; tidak ada route terpisah.
                    onProfileClick = {},
                    onLihatProfilClick = {},
                    onMenuItemClick = { menu ->
                        when (menu) {
                            AkunMenuItem.UBAH_PIN -> navController.navigate(UbahKodeAkses)
                            AkunMenuItem.ATUR_LIMIT -> navController.navigate(AturLimit)
                            AkunMenuItem.PUSAT_BANTUAN -> navController.navigate(PusatBantuan)
                            AkunMenuItem.HUBUNGI_CS -> navController.navigate(HubungiCs)
                            AkunMenuItem.BLOKIR_KARTU -> {
                                viewModel.onBlokirKartuClick()
                                navController.navigate(KartuAksi)
                            }
                            // Dua pintu ke alur yang sama: desain menaruhnya
                            // sebagai aksi kartu dan sebagai baris pengaturan.
                            AkunMenuItem.GANTI_KARTU,
                            AkunMenuItem.PENGGANTIAN_KARTU,
                            -> {
                                viewModel.onGantiKartuClick()
                                navController.navigate(KartuAksi)
                            }
                            // Kontrol Akses ada di desain tetapi belum punya
                            // endpoint sendiri — yang dilayani server hanya dua
                            // sakelar kanal yang sudah tampil di bawah kartu.
                            AkunMenuItem.KONTROL_AKSES,
                            AkunMenuItem.TENTANG_APLIKASI,
                            AkunMenuItem.NOTIFIKASI_PUSH,
                            AkunMenuItem.EMAIL_STATEMENT,
                            -> scope.launch {
                                snackbarHostState.showSnackbar(belumTersedia)
                            }
                        }
                    },
                    onCardSettingToggle = viewModel::onCardSettingToggle,
                    onBiometricToggle = viewModel::onBiometricToggle,
                    onNotificationToggle = viewModel::onNotificationToggle,
                    onEmailStatementToggle = viewModel::onEmailStatementToggle,
                    onKeluarClick = viewModel::onLogout,
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        composable<UbahKodeAkses> {
            val viewModel: UbahKodeAksesViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            UbahKodeAksesScreen(
                state = state,
                onKodeLamaChanged = viewModel::onKodeLamaChanged,
                onKodeBaruChanged = viewModel::onKodeBaruChanged,
                onKonfirmasiChanged = viewModel::onKonfirmasiChanged,
                onToggleKodeLamaVisibility = viewModel::onToggleKodeLamaVisibility,
                onToggleKodeBaruVisibility = viewModel::onToggleKodeBaruVisibility,
                onToggleKonfirmasiVisibility = viewModel::onToggleKonfirmasiVisibility,
                onLanjutClick = viewModel::onLanjutClick,
                onSelesaiClick = { navController.popBackStack() },
                // Back di langkah kedua kembali ke langkah pertama dulu.
                onBackClick = {
                    if (!viewModel.onBackRequested()) navController.popBackStack()
                },
            )
        }

        composable<RekeningKartu> {
            val viewModel: RekeningKartuViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            RekeningKartuScreen(
                state = state,
                onToggleBalance = viewModel::onToggleBalance,
                onDetailToggle = viewModel::onDetailToggle,
                // Mutasi memilih rekening utamanya sendiri; membawa account_id ke
                // sana butuh argumen route baru, jadi itu pekerjaan terpisah.
                onMutasiClick = { navController.navigate(Mutasi) },
                onBackClick = { navController.popBackStack() },
                onRetry = viewModel::load,
            )
        }

        composable<AturLimit> {
            val viewModel: AturLimitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            val snackbarHostState = LocalSnackbarHostState.current
            val savedMessage = stringResource(R.string.limit_berhasil)

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        // Verifikasi PIN diwajibkan server, jadi dialognya bagian dari alur.
                        AturLimitEvent.PinRequired -> navController.navigate(AturLimitPin)
                        AturLimitEvent.Saved -> {
                            navController.popBackStack(AturLimitPin, inclusive = true)
                            snackbarHostState.showSnackbar(savedMessage)
                        }
                    }
                }
            }

            AturLimitScreen(
                state = state,
                onEditClick = viewModel::onEditClick,
                onInputChanged = viewModel::onInputChanged,
                onCancelEdit = viewModel::onCancelEdit,
                onSaveClick = viewModel::onSaveClick,
                onBackClick = { navController.popBackStack() },
            )
        }

        dialog<KartuAksi>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) { entry ->
            // ViewModel milik tab Akun: pilihan alasan, PIN, dan daftar kartu
            // harus satu instance, kalau tidak aksinya kehilangan konteksnya.
            val owner = remember(entry) {
                runCatching { navController.getBackStackEntry(Akun) }.getOrDefault(entry)
            }
            val viewModel: AkunViewModel = hiltViewModel(owner)
            val state by viewModel.aksiKartuState.collectAsState()

            AksiKartuDialog(
                state = state,
                onBlockReasonSelected = viewModel::onBlockReasonSelected,
                onReplacementReasonSelected = viewModel::onReplacementReasonSelected,
                onDeliveryMethodSelected = viewModel::onDeliveryMethodSelected,
                onConfirm = viewModel::onAksiKartuConfirm,
                onDismiss = {
                    viewModel.onAksiKartuDismiss()
                    navController.popBackStack()
                },
            )
        }

        dialog<KartuPin>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) { entry ->
            val owner = remember(entry) {
                runCatching { navController.getBackStackEntry(Akun) }.getOrDefault(entry)
            }
            val viewModel: AkunViewModel = hiltViewModel(owner)
            val pinState by viewModel.pinState.collectAsState()

            KodeAksesScreen(
                state = pinState,
                onDigitClick = viewModel::onPinDigit,
                onDeleteClick = viewModel::onPinDelete,
                // Batal hanya menutup dialog PIN; pilihan alasan di bawahnya
                // dibiarkan supaya nasabah tidak memilih ulang dari awal.
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = viewModel::submitPin,
                onForgotClick = {},
            )
        }

        composable<PusatBantuan> {
            val viewModel: PusatBantuanViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            PusatBantuanScreen(
                state = state,
                onItemToggle = viewModel::onItemToggle,
                onHubungiCsClick = { navController.navigate(HubungiCs) },
                onBackClick = { navController.popBackStack() },
                onRetry = viewModel::load,
            )
        }

        composable<HubungiCs> {
            val viewModel: HubungiCsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            HubungiCsScreen(
                state = state,
                // Aplikasi hanya menyerahkan niatnya ke sistem; tidak ada
                // nomor yang dirakit di sini.
                onPhoneClick = { context.dial(it) },
                onWhatsAppClick = { context.openWhatsApp(it) },
                onEmailClick = { context.sendEmail(it) },
                onChatClick = { context.openLink(it) },
                onBackClick = { navController.popBackStack() },
                onRetry = viewModel::load,
            )
        }

        dialog<AturLimitPin>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            // ViewModel milik layar di bawah dialog: nilai limit dan PIN harus satu instance.
            val owner = remember(it) {
                runCatching { navController.getBackStackEntry(AturLimit) }.getOrDefault(it)
            }
            val viewModel: AturLimitViewModel = hiltViewModel(owner)
            val pinState by viewModel.pinState.collectAsState()

            KodeAksesScreen(
                state = pinState,
                onDigitClick = viewModel::onPinDigit,
                onDeleteClick = viewModel::onPinDelete,
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = viewModel::submitPin,
                onForgotClick = {},
            )
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
                onNotificationClick = { navController.navigate(Notifikasi) },
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
            var state by remember { mutableStateOf(RentangWaktuUiState()) }

            RentangWaktuScreen(
                state = state,
                onPilihanSelected = { state = state.copy(pilihan = it) },
                onTanggalMulaiSelected = { state = state.copy(tanggalMulai = it.toIsoDate()) },
                onTanggalAkhirSelected = { state = state.copy(tanggalAkhir = it.toIsoDate()) },
                onTerapkanClick = {
                    // Nilainya dikirim balik ke entri Mutasi lalu layar ini ditutup.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RENTANG_RESULT_KEY, state.toResultValue())
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        eWalletGraph(navController)
        qrisGraph(navController)
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

// ── Hasil layar Rentang Waktu ───────────────────────────────────────────

// Dipakai bersama oleh Mutasi dan Riwayat. Tidak ada tabrakan: kuncinya
// disimpan di SavedStateHandle milik masing-masing entri, bukan satu tempat.
private const val RENTANG_RESULT_KEY = "rentang_waktu_result"
private const val RENTANG_RESULT_SEPARATOR = "|"
private const val RENTANG_CUSTOM = "CUSTOM"

/**
 * Pilihan rentang dikirim sebagai satu String supaya cukup lewat SavedStateHandle
 * tanpa menambah tipe yang harus di-parcel.
 */
private fun RentangWaktuUiState.toResultValue(): String = when (pilihan) {
    RentangPilihan.HARI_INI -> {
        val today = LocalDate.now().toString()
        listOf(RENTANG_CUSTOM, today, today).joinToString(RENTANG_RESULT_SEPARATOR)
    }
    RentangPilihan.TUJUH_HARI -> MutasiPeriod.LAST_7_DAYS.name
    RentangPilihan.BULAN_INI -> MutasiPeriod.THIS_MONTH.name
    RentangPilihan.BULAN_LALU -> MutasiPeriod.LAST_MONTH.name
    RentangPilihan.PILIH_TANGGAL -> listOf(RENTANG_CUSTOM, tanggalMulai, tanggalAkhir)
        .joinToString(RENTANG_RESULT_SEPARATOR)
}

/** Milidetik dari DatePicker adalah UTC; tanggal dikirim apa adanya sebagai `yyyy-MM-dd`. */
private fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
