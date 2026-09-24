package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.biometric.toTouchIdUiState
import id.bca.bcamobile.ui.screen.biometric.toFaceIdUiState
import id.bca.bcamobile.ui.screen.biometric.BiometricPromptText
import id.bca.bcamobile.ui.screen.biometric.BiometricLoginViewModel
import id.bca.bcamobile.ui.screen.biometric.BiometricLoginEvent
import id.bca.bcamobile.domain.auth.model.BiometricType
import androidx.fragment.app.FragmentActivity
import android.content.ContextWrapper
import android.content.Context
import id.bca.bcamobile.ui.screen.faceid.FaceIdScreen
import id.bca.bcamobile.ui.screen.faceid.FaceIdUiState
import id.bca.bcamobile.ui.screen.finger_print.TouchIdScreen
import id.bca.bcamobile.ui.screen.finger_print.TouchIdUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesEvent
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesViewModel
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.liveness.LivenessAnalyzer
import id.bca.bcamobile.core.liveness.LivenessDetector
import id.bca.bcamobile.core.ocr.KtpAutoCaptureAnalyzer
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.components.CameraPermissionGate
import id.bca.bcamobile.ui.components.CameraPermissionNotice
import id.bca.bcamobile.ui.components.CameraPreview
import id.bca.bcamobile.ui.components.LocalSnackbarHostState
import id.bca.bcamobile.ui.components.resolve
import id.bca.bcamobile.ui.components.hasCameraPermission
import id.bca.bcamobile.ui.screen.buka_rekening.FlashMode
import id.bca.bcamobile.ui.screen.buka_rekening.toKameraFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toVerifikasiBiometrikUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningEvent
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningFlowViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.toAntreanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toBerhasilDibuatUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toBuatKredensialUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toDataPribadiUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toHasilFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toPilihJenisUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toPilihKartuUiState
import id.bca.bcamobile.ui.screen.buka_rekening.toRingkasanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPilihJenisScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPilihKartuScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningDataPribadiScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningVerifikasiBiometrikScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningVerifikasiOtpScreen
import id.bca.bcamobile.ui.screen.buka_rekening.toOtpUiState
import id.bca.bcamobile.ui.screen.buka_rekening.VerifikasiBiometrikUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningAntreanVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.VideoCallUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningBuatKredensialScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningRingkasanScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningBerhasilDibuatScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningHasilFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningKameraFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningKameraFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPanduanFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningSyaratKetentuanScreen
import id.bca.bcamobile.ui.screen.login.LoginScreen
import id.bca.bcamobile.ui.screen.login.LoginUiState
import kotlinx.coroutines.launch

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthenticated: (displayName: String) -> Unit,
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
            val viewModel: KodeAksesViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            // Keberhasilan login datang dari server, bukan dari hasil tombol.
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is KodeAksesEvent.LoginSucceeded ->
                            onAuthenticated(event.displayName)
                    }
                }
            }

            KodeAksesScreen(
                state = state,
                onDigitClick = viewModel::onDigitClick,
                onDeleteClick = viewModel::onDeleteClick,
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = viewModel::submit,
                onForgotClick = {},
            )
        }

        composable<FaceId> {
            val viewModel: BiometricLoginViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val activity = LocalContext.current.findFragmentActivity()
            val promptText = BiometricPromptText(
                title = stringResource(R.string.biometric_prompt_title_face),
                subtitle = stringResource(R.string.biometric_prompt_subtitle),
                negativeButton = stringResource(R.string.biometric_prompt_use_access_code),
            )

            BiometricLoginEffects(viewModel, navController, onAuthenticated)

            // Layar Face ID langsung memindai begitu dibuka — tidak ada tombol mulai.
            LaunchedEffect(activity) {
                activity?.let {
                    viewModel.authenticate(it, BiometricType.FACE_ID, promptText)
                }
            }

            FaceIdScreen(
                state = state.toFaceIdUiState(),
                onBackClick = { navController.popBackStack() },
                onRetryClick = {
                    activity?.let {
                        viewModel.authenticate(it, BiometricType.FACE_ID, promptText)
                    }
                },
                onCancelClick = { navController.popBackStack() },
            )
        }

        composable<TouchId> {
            val viewModel: BiometricLoginViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val activity = LocalContext.current.findFragmentActivity()
            val promptText = BiometricPromptText(
                title = stringResource(R.string.biometric_prompt_title_fingerprint),
                subtitle = stringResource(R.string.biometric_prompt_subtitle),
                negativeButton = stringResource(R.string.biometric_prompt_use_access_code),
            )
            val startAuth = {
                activity?.let {
                    viewModel.authenticate(it, BiometricType.FINGERPRINT, promptText)
                }
                Unit
            }

            BiometricLoginEffects(viewModel, navController, onAuthenticated)

            TouchIdScreen(
                state = state.toTouchIdUiState(),
                onFingerprintPress = startAuth,
                onUseAccessCode = {
                    navController.navigate(KodeAkses) {
                        popUpTo<Login> { inclusive = false }
                    }
                },
                onRetryClick = startAuth,
            )
        }

        composable<BukaRekening> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            BukaRekeningPilihJenisScreen(
                state = state.toPilihJenisUiState(),
                onJenisSelected = { index ->
                    viewModel.onEvent(BukaRekeningEvent.ProductSelected(index))
                    navController.navigate(BukaRekeningPilihKartu)
                },
                onBackClick = { navController.popBackStack() },
                onRetry = { viewModel.onEvent(BukaRekeningEvent.ErrorDismissed) },
            )
        }

        composable<BukaRekeningPilihKartu> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            // Pilihan kartu belum menyentuh API: sesi baru dibuat di layar S&K,
            // jadi perpindahan di sini boleh dipicu tombol.
            BukaRekeningPilihKartuScreen(
                state = state.toPilihKartuUiState(),
                onKartuSelected = { index ->
                    viewModel.onEvent(BukaRekeningEvent.CardTypeSelected(index))
                },
                onLanjutClick = { navController.navigate(BukaRekeningSyaratKetentuan) },
                onBackClick = { navController.popBackStack() },
                onRetry = { viewModel.onEvent(BukaRekeningEvent.ErrorDismissed) },
            )
        }

        composable<BukaRekeningSyaratKetentuan> {
            val viewModel = bukaRekeningViewModel(navController, it)
            BukaRekeningSideEffects(viewModel, navController)

            // Sesi baru dibuat di sini: server butuh versi S&K yang disetujui.
            BukaRekeningSyaratKetentuanScreen(
                onAgreeClick = { viewModel.onEvent(BukaRekeningEvent.TncAccepted) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningPanduanFoto> {
            BukaRekeningPanduanFotoScreen(
                onMulaiAmbilFoto = { navController.navigate(BukaRekeningKameraFoto) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningKameraFoto> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var hasCamera by remember { mutableStateOf(context.hasCameraPermission()) }
            val controller = remember(hasCamera) {
                if (hasCamera) CameraCapture.createController(context) else null
            }

            LaunchedEffect(controller, state.flashMode) {
                controller?.let { CameraCapture.applyFlashMode(it, state.flashMode) }
            }

            val capture: (Boolean) -> Unit = { autoCaptured ->
                val active = controller
                if (active != null) {
                    scope.launch {
                        val photo = CameraCapture.capture(context, active, CACHE_PREFIX_KTP)
                        if (photo != null) {
                            viewModel.onEvent(
                                BukaRekeningEvent.KtpPhotoCaptured(
                                    photo = photo.file,
                                    flashUsed = state.flashMode != FlashMode.OFF,
                                    autoCaptured = autoCaptured,
                                    resolution = photo.resolution,
                                ),
                            )
                        }
                    }
                }
            }

            // Auto-capture: ML Kit membaca frame preview dan menjepret sendiri
            // begitu NIK 16 digit terbaca. Dilepas saat toggle dimatikan.
            DisposableEffect(controller, state.isAutoCaptureEnabled, state.ktpPhoto) {
                val active = controller
                if (active == null || !state.isAutoCaptureEnabled || state.ktpPhoto != null) {
                    return@DisposableEffect onDispose { }
                }
                val analyzer = KtpAutoCaptureAnalyzer(
                    recognizer = KtpTextRecognizer(),
                    scope = scope,
                    onDocumentReady = { capture(true) },
                )
                active.setImageAnalysisAnalyzer(CameraCapture.analysisExecutor, analyzer)
                onDispose {
                    active.clearImageAnalysisAnalyzer()
                    analyzer.close()
                }
            }

            BukaRekeningKameraFotoScreen(
                state = state.toKameraFotoUiState(),
                onShutterClick = { capture(false) },
                onGalleryClick = {},
                onHelpClick = {},
                onFlashToggle = { viewModel.onEvent(BukaRekeningEvent.FlashModeToggled) },
                onAutoCaptureToggle = {
                    viewModel.onEvent(BukaRekeningEvent.AutoCaptureToggled(it))
                },
                onBackClick = { navController.popBackStack() },
                // Gerbang izin dirender tanpa syarat. Sebelumnya slot ini ikut null
                // saat izin belum ada, sehingga dialog izin tidak pernah muncul dan
                // layar kamera jadi jalan buntu.
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
                            CameraPreview(
                                controller = active,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
            )
        }

        composable<BukaRekeningHasilFoto> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            // Saat melanjutkan draf, hasil OCR sudah ada di server — ambil dari sana.
            LaunchedEffect(state.sessionId, state.ocr, state.ktpPhoto) {
                // Hanya saat melanjutkan draf: tidak ada foto lokal, tapi server
                // mungkin sudah menyimpan hasil OCR dari sesi sebelumnya.
                if (state.sessionId != null && state.ocr == null && state.ktpPhoto == null) {
                    viewModel.onEvent(BukaRekeningEvent.OcrResultRequested)
                }
            }

            BukaRekeningHasilFotoScreen(
                state = state.toHasilFotoUiState(),
                onGunakanFoto = { viewModel.onEvent(BukaRekeningEvent.OcrConfirmed) },
                onAmbilUlang = {
                    viewModel.onEvent(BukaRekeningEvent.PhotoDiscarded)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningDataPribadi> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            BukaRekeningDataPribadiScreen(
                state = state.toDataPribadiUiState(),
                onJenisKelaminSelect = {
                    viewModel.onEvent(BukaRekeningEvent.GenderSelected(it))
                },
                onAlamatDomisiliToggle = {
                    viewModel.onEvent(BukaRekeningEvent.DomicileSameToggled(it))
                },
                onLanjutClick = { viewModel.onEvent(BukaRekeningEvent.PersonalDataSubmitted) },
                onSimpanClick = { viewModel.onEvent(BukaRekeningEvent.DraftSaveRequested) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningOtp> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            // Kode OTP tidak boleh tertinggal di memory setelah layar ditutup.
            DisposableEffect(viewModel) {
                onDispose { viewModel.onEvent(BukaRekeningEvent.OtpCodeCleared) }
            }

            BukaRekeningVerifikasiOtpScreen(
                state = state.toOtpUiState(),
                onKodeChange = { kode ->
                    viewModel.onEvent(BukaRekeningEvent.OtpCodeChanged(kode))
                },
                // Perpindahan ke biometrik datang dari `current_step` di response
                // verify-otp, bukan dari callback tombol ini.
                onVerifikasiClick = { viewModel.onEvent(BukaRekeningEvent.OtpSubmitted) },
                onKirimUlangClick = { viewModel.onEvent(BukaRekeningEvent.OtpResendRequested) },
                // Back kembali ke Data Pribadi supaya nomor HP bisa diperbaiki.
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVerifikasiBiometrik> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var hasCamera by remember { mutableStateOf(context.hasCameraPermission()) }
            val controller = remember(hasCamera) {
                if (hasCamera) {
                    CameraCapture.createController(context, CameraSelector.LENS_FACING_FRONT)
                } else {
                    null
                }
            }

            // Tantangan liveness berjalan di atas aliran frame kamera depan.
            // Analyzer baru dipasang setelah pengguna menekan Mulai, supaya
            // ML Kit tidak bekerja saat layar hanya dilihat sekilas.
            DisposableEffect(controller, state.isLivenessRunning) {
                val active = controller
                if (active == null || !state.isLivenessRunning) {
                    return@DisposableEffect onDispose { }
                }

                val analyzer = LivenessAnalyzer(
                    context = context,
                    detector = LivenessDetector(),
                    scope = scope,
                    onProgress = { viewModel.onEvent(BukaRekeningEvent.LivenessProgressed(it)) },
                    onFramesReady = { frames ->
                        scope.launch {
                            val face = CameraCapture.capture(context, active, CACHE_PREFIX_FACE)
                                ?: return@launch
                            viewModel.onEvent(
                                BukaRekeningEvent.BiometricCaptured(
                                    facePhoto = face.file,
                                    livenessFrames = frames,
                                    meta = LivenessMeta(
                                        challengeType = CHALLENGE_BLINK,
                                        completedActions = LivenessDetector.TOTAL_CHALLENGES,
                                        precisionScore =
                                            viewModel.state.value.liveness.precisionPercent
                                                .toDouble(),
                                    ),
                                ),
                            )
                        }
                    },
                )
                active.setImageAnalysisAnalyzer(CameraCapture.analysisExecutor, analyzer)

                onDispose {
                    active.clearImageAnalysisAnalyzer()
                    analyzer.close()
                }
            }

            BukaRekeningVerifikasiBiometrikScreen(
                state = state.toVerifikasiBiometrikUiState(),
                onMulaiClick = { viewModel.onEvent(BukaRekeningEvent.LivenessStarted) },
                onTipsClick = {},
                onBackClick = { navController.popBackStack() },
                // Gerbang izin dirender tanpa syarat — lihat catatan di layar Kamera Foto.
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
                            CameraPreview(
                                controller = active,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
            )
        }

        composable<BukaRekeningAntreanVideoCall> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            LaunchedEffect(state.sessionId) {
                if (state.sessionId != null && state.queue == null) {
                    viewModel.onEvent(BukaRekeningEvent.QueueJoinRequested)
                }
            }

            BukaRekeningAntreanVideoCallScreen(
                state = state.toAntreanUiState(),
                onTungguClick = { navController.navigate(BukaRekeningVideoCall) },
                onJadwalkanClick = {},
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVideoCall> {
            val viewModel = bukaRekeningViewModel(navController, it)
            BukaRekeningSideEffects(viewModel, navController)

            // TODO(webrtc): ganti state statis ini dengan sesi WebRTC dari signaling_url
            //  pada QueueTicket — lihat skill buka-rekening-video-call.
            BukaRekeningVideoCallScreen(
                state = VideoCallUiState(),
                onBack = { navController.popBackStack() },
                onMuteToggle = {},
                onSwitchCamera = {},
                onEndCall = { viewModel.onEvent(BukaRekeningEvent.VideoCallCompleted("")) },
            )
        }

        composable<BukaRekeningBuatKredensial> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            BukaRekeningBuatKredensialScreen(
                state = state.toBuatKredensialUiState(),
                onKodeAksesChange = {
                    viewModel.onEvent(BukaRekeningEvent.AccessCodeChanged(it))
                },
                onKonfirmasiKodeAksesChange = {
                    viewModel.onEvent(BukaRekeningEvent.ConfirmAccessCodeChanged(it))
                },
                onToggleKodeAksesVisibility = {
                    viewModel.onEvent(BukaRekeningEvent.AccessCodeVisibilityToggled)
                },
                onToggleKonfirmasiVisibility = {
                    viewModel.onEvent(BukaRekeningEvent.ConfirmAccessCodeVisibilityToggled)
                },
                onSimpanClick = { viewModel.onEvent(BukaRekeningEvent.CredentialsSubmitted) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningRingkasan> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(viewModel, navController)

            BukaRekeningRingkasanScreen(
                state = state.toRingkasanUiState(),
                onAgreementToggle = {
                    viewModel.onEvent(BukaRekeningEvent.AgreementToggled(it))
                },
                onUbahRekeningClick = { navController.popBackStack(BukaRekening, false) },
                onUbahNasabahClick = { navController.popBackStack(BukaRekeningDataPribadi, false) },
                onProsesClick = { viewModel.onEvent(BukaRekeningEvent.ApplicationSubmitted) },
                onSimpanDrafClick = { viewModel.onEvent(BukaRekeningEvent.DraftSaveRequested) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningBerhasilDibuat> {
            val viewModel = bukaRekeningViewModel(navController, it)
            val state by viewModel.state.collectAsState()

            BukaRekeningBerhasilDibuatScreen(
                state = state.toBerhasilDibuatUiState(),
                onMasukMbcaClick = {
                    navController.navigate(Login) {
                        popUpTo<BukaRekening> { inclusive = true }
                    }
                },
                onBagikanClick = {},
                onSalinClick = {},
            )
        }

        composable<GantiKodeAkses> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.navigation_change_access_code))
            }
        }
    }
}

/**
 * ViewModel bersama seluruh flow buka rekening.
 *
 * Di-scope ke back stack entry [BukaRekening] — layar pertama flow — sehingga
 * dua belas layar sesudahnya memakai instance yang sama, dan instance itu ikut
 * dibuang begitu flow keluar dari back stack.
 */
@Composable
private fun bukaRekeningViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): BukaRekeningFlowViewModel {
    // Dikunci ke entri layar ini sendiri, bukan currentBackStackEntry: saat flow
    // dipop, layar yang sedang keluar masih ter-compose selama animasi transisi.
    // Dengan key lama, remember dievaluasi ulang di saat itu dan
    // getBackStackEntry(BukaRekening) melempar IllegalArgumentException.
    val owner = remember(entry) {
        runCatching { navController.getBackStackEntry(BukaRekening) }.getOrDefault(entry)
    }
    return hiltViewModel(owner)
}

/**
 * Menyalurkan side effect ViewModel ke navigasi.
 *
 * Perpindahan maju selalu berasal dari `current_step` yang dikirim server,
 * bukan dari tebakan lokal — itu sebabnya layar tidak lagi memanggil
 * `navigate` sendiri setelah sebuah aksi API.
 */
@Composable
private fun BukaRekeningSideEffects(
    viewModel: BukaRekeningFlowViewModel,
    navController: NavHostController,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is BukaRekeningSideEffect.AdvanceTo ->
                    navController.navigate(effect.step.toRoute())

                BukaRekeningSideEffect.RestartFlow ->
                    navController.navigate(BukaRekening) {
                        popUpTo<BukaRekening> { inclusive = true }
                    }

                BukaRekeningSideEffect.ExitFlow ->
                    navController.popBackStack(BukaRekening, true)

                BukaRekeningSideEffect.ShowCaptureResult ->
                    navController.navigate(BukaRekeningHasilFoto)

                is BukaRekeningSideEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.text.resolve(resources))
            }
        }
    }
}

/**
 * Peta step server ke route.
 *
 * Setiap step punya tujuan, jadi `AdvanceTo` tidak pernah kehabisan route.
 * Menambah step baru di [OnboardingStep] membuat `when` ini gagal dikompilasi —
 * itu disengaja, supaya step tanpa layar ketahuan saat build, bukan saat dipakai.
 */
private fun OnboardingStep.toRoute(): Any = when (this) {
    OnboardingStep.TNC -> BukaRekeningSyaratKetentuan
    OnboardingStep.OCR -> BukaRekeningPanduanFoto
    OnboardingStep.PERSONAL_DATA -> BukaRekeningDataPribadi
    OnboardingStep.OTP_VERIFY -> BukaRekeningOtp
    OnboardingStep.BIOMETRIC -> BukaRekeningVerifikasiBiometrik
    OnboardingStep.VIDEO_CALL -> BukaRekeningAntreanVideoCall
    OnboardingStep.CREDENTIALS -> BukaRekeningBuatKredensial
    OnboardingStep.REVIEW -> BukaRekeningRingkasan
    OnboardingStep.COMPLETED -> BukaRekeningBerhasilDibuat
}

private const val CACHE_PREFIX_KTP = "ktp_"
private const val CACHE_PREFIX_FACE = "face_"
private const val CHALLENGE_BLINK = "BLINK"

/**
 * Hasil login biometrik: sukses diteruskan ke sesi, sisanya jatuh ke kode akses.
 *
 * Alasan kegagalan belum bisa ditampilkan di layar — `FaceIdUiState` dan
 * `TouchIdUiState` hanya membawa status, tanpa slot pesan. Menambahkannya
 * mengubah kontrak composable, jadi perlu dibahas dengan pemilik desain.
 */
@Composable
private fun BiometricLoginEffects(
    viewModel: BiometricLoginViewModel,
    navController: NavHostController,
    onAuthenticated: (displayName: String) -> Unit,
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Dulu `-> Unit`: login biometrik berhasil tapi sesi tidak pernah
                // ikut terbuka, jadi layar tetap diam di Face ID / Touch ID.
                is BiometricLoginEvent.LoginSucceeded ->
                    onAuthenticated(event.displayName)
                is BiometricLoginEvent.FallbackToAccessCode -> {
                    navController.navigate(KodeAkses) {
                        popUpTo<Login> { inclusive = false }
                    }
                }
            }
        }
    }
}

/** BiometricPrompt butuh FragmentActivity; MainActivity sudah diperluas ke sana. */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
