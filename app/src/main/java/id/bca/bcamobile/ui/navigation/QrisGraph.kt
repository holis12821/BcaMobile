package id.bca.bcamobile.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.qris.QrisScanAnalyzer
import id.bca.bcamobile.core.qris.QrisScanner
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.CameraPermissionGate
import id.bca.bcamobile.ui.components.CameraPermissionNotice
import id.bca.bcamobile.ui.components.CameraPreview
import id.bca.bcamobile.ui.components.hasCameraPermission
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.qris.QrisFlowEvent
import id.bca.bcamobile.ui.screen.qris.QrisFlowViewModel
import id.bca.bcamobile.ui.screen.qris.QrisKonfirmasiScreen
import id.bca.bcamobile.ui.screen.qris.ScanQrisScreen
import id.bca.bcamobile.ui.screen.qris.toBuktiUiState
import id.bca.bcamobile.ui.screen.qris.toKonfirmasiUiState
import id.bca.bcamobile.ui.screen.qris.toPinUiState
import id.bca.bcamobile.ui.screen.qris.toScanUiState
import kotlinx.coroutines.launch

/**
 * Alur pembayaran QRIS: pindai → konfirmasi → PIN → bukti.
 *
 * Satu [QrisFlowViewModel] untuk empat layar, di-scope ke entri graph seperti
 * alur e-wallet dan transfer, sehingga PIN dan payload QR ikut hilang begitu
 * graph lepas dari back stack.
 */
fun NavGraphBuilder.qrisGraph(navController: NavHostController) {
    navigation<GraphQris>(startDestination = QrisScan) {

        composable<QrisScan> {
            val viewModel = qrisViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // Payload QR yang lama tidak boleh menempel saat nasabah memindai ulang.
            LaunchedEffect(viewModel) { viewModel.onScanResumed() }

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is QrisFlowEvent.PayloadReady) {
                        navController.navigate(QrisKonfirmasi)
                    }
                }
            }

            var hasCamera by remember { mutableStateOf(context.hasCameraPermission()) }
            val controller = remember(hasCamera) {
                if (hasCamera) CameraCapture.createController(context) else null
            }

            // Flash pemindai adalah torch, bukan mode flash jepretan.
            LaunchedEffect(controller, state.isFlashOn) {
                controller?.enableTorch(state.isFlashOn)
            }

            DisposableEffect(controller) {
                val active = controller
                if (active == null) return@DisposableEffect onDispose { }
                val scanner = QrisScanner()
                val analyzer = QrisScanAnalyzer(
                    scanner = scanner,
                    onQrScanned = viewModel::onQrScanned,
                )
                active.setImageAnalysisAnalyzer(CameraCapture.analysisExecutor, analyzer)
                onDispose {
                    active.clearImageAnalysisAnalyzer()
                    // Client ML Kit yang dibuat manual harus dilepas, kalau tidak
                    // memori native-nya menumpuk setiap kali layar dibuka.
                    scanner.close()
                }
            }

            val galleryPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia(),
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val scanner = QrisScanner()
                        val value = scanner.scan(context, uri)
                        scanner.close()
                        if (value != null) viewModel.onQrScanned(value)
                    }
                }
            }

            ScanQrisScreen(
                state = state.toScanUiState(),
                onFlashToggle = viewModel::onFlashToggle,
                onGalleryClick = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onBackClick = { navController.popBackStack() },
                cameraPreview = {
                    CameraPermissionGate(
                        rationale = { request -> CameraPermissionNotice(onAction = request) },
                        settingsPrompt = { open ->
                            CameraPermissionNotice(onAction = open, isPermanentlyDenied = true)
                        },
                        modifier = Modifier.fillMaxSize(),
                        onGrantedChange = { hasCamera = it },
                    ) {
                        controller?.let { active ->
                            CameraPreview(controller = active, modifier = Modifier.fillMaxSize())
                        }
                    }
                },
            )
        }

        composable<QrisKonfirmasi> {
            val viewModel = qrisViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            QrisKonfirmasiScreen(
                state = state.toKonfirmasiUiState(),
                onAmountChanged = viewModel::onAmountChanged,
                onPayClick = { navController.navigate(QrisPin) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<QrisPin> {
            val viewModel = qrisViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    if (event is QrisFlowEvent.PaymentSucceeded) {
                        navController.navigate(QrisBukti) {
                            popUpTo<QrisPin> { inclusive = true }
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

        composable<QrisBukti> {
            val viewModel = qrisViewModel(navController, it)
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

@Composable
private fun qrisViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): QrisFlowViewModel {
    // Dikunci ke entri layar ini sendiri, bukan currentBackStackEntry: saat flow
    // dipop, layar yang sedang keluar masih ter-compose selama animasi transisi.
    val owner = remember(entry) {
        runCatching { navController.getBackStackEntry(GraphQris) }.getOrDefault(entry)
    }
    return hiltViewModel(owner)
}
