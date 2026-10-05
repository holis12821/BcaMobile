package id.bca.bcamobile.ui.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.liveness.LivenessAnalyzer
import id.bca.bcamobile.core.liveness.LivenessDetector
import id.bca.bcamobile.core.ocr.KtpAutoCaptureAnalyzer
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.auth.model.BiometricType
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.components.CameraPermissionGate
import id.bca.bcamobile.ui.components.CameraPermissionNotice
import id.bca.bcamobile.ui.components.CameraPreview
import id.bca.bcamobile.ui.components.LocalSnackbarHostState
import id.bca.bcamobile.ui.components.hasCameraPermission
import id.bca.bcamobile.ui.components.hasRequiredVideoCallPermissions
import id.bca.bcamobile.ui.components.hasVideoCallBluetoothPermission
import id.bca.bcamobile.ui.components.hasVideoCallPermissions
import id.bca.bcamobile.ui.components.openAppSettings
import id.bca.bcamobile.ui.components.resolve
import id.bca.bcamobile.ui.components.videoCallRequestedPermissions
import id.bca.bcamobile.ui.screen.biometric.BiometricLoginEvent
import id.bca.bcamobile.ui.screen.biometric.BiometricLoginViewModel
import id.bca.bcamobile.ui.screen.biometric.BiometricPromptText
import id.bca.bcamobile.ui.screen.biometric.toFaceIdUiState
import id.bca.bcamobile.ui.screen.biometric.toTouchIdUiState
import id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call.AntreanVideoCallEvent
import id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call.BukaRekeningAntreanVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call.BukaRekeningAntreanVideoCallViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call.toAntreanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat.BerhasilDibuatEvent
import id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat.BukaRekeningBerhasilDibuatScreen
import id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat.BukaRekeningBerhasilDibuatViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat.toBerhasilDibuatUiState
import id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial.BuatKredensialEvent
import id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial.BukaRekeningBuatKredensialScreen
import id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial.BukaRekeningBuatKredensialViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial.toBuatKredensialUiState
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowScopeViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.FlashMode
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.BukaRekeningDataPribadiScreen
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.BukaRekeningDataPribadiViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.DataPribadiEvent
import id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi.toDataPribadiUiState
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.BukaRekeningHasilFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.BukaRekeningHasilFotoViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.HasilFotoEvent
import id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto.toHasilFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto.BukaRekeningKameraFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto.BukaRekeningKameraFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto.BukaRekeningKameraFotoViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto.KameraFotoEvent
import id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto.toKameraFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.panduan_foto.BukaRekeningPanduanFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.panduan_foto.BukaRekeningPanduanFotoViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.BukaRekeningPilihJenisScreen
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.BukaRekeningPilihJenisViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.PilihJenisEvent
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis.toPilihJenisUiState
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu.BukaRekeningPilihKartuScreen
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu.BukaRekeningPilihKartuViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu.PilihKartuEvent
import id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu.toPilihKartuUiState
import id.bca.bcamobile.ui.screen.buka_rekening.ringkasan.BukaRekeningRingkasanScreen
import id.bca.bcamobile.ui.screen.buka_rekening.ringkasan.BukaRekeningRingkasanViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.ringkasan.RingkasanEvent
import id.bca.bcamobile.ui.screen.buka_rekening.ringkasan.toRingkasanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.BukaRekeningSyaratKetentuanScreen
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.BukaRekeningSyaratKetentuanViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.SyaratKetentuanEvent
import id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan.toSyaratKetentuanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.BukaRekeningVerifikasiBiometrikScreen
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.BukaRekeningVerifikasiBiometrikViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.VerifikasiBiometrikEvent
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.VerifikasiBiometrikUiState
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik.toVerifikasiBiometrikUiState
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.BukaRekeningVerifikasiOtpScreen
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.BukaRekeningVerifikasiOtpViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.VerifikasiOtpEvent
import id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp.toOtpUiState
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.BukaRekeningVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.BukaRekeningVideoCallViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.VideoCallEvent
import id.bca.bcamobile.ui.screen.buka_rekening.video_call.VideoCallUiState
import id.bca.bcamobile.ui.screen.faceid.FaceIdScreen
import id.bca.bcamobile.ui.screen.faceid.FaceIdUiState
import id.bca.bcamobile.ui.screen.finger_print.TouchIdScreen
import id.bca.bcamobile.ui.screen.finger_print.TouchIdUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesEvent
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesViewModel
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
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningPilihJenisViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            BukaRekeningPilihJenisScreen(
                state = state.toPilihJenisUiState(),
                onJenisSelected = { index ->
                    viewModel.onEvent(PilihJenisEvent.ProductSelected(index))
                    navController.navigate(BukaRekeningPilihKartu)
                },
                onBackClick = { navController.popBackStack() },
                onRetry = { viewModel.onEvent(PilihJenisEvent.ErrorDismissed) },
            )
        }

        composable<BukaRekeningPilihKartu> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningPilihKartuViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            // Katalog ditarik saat layar dibuka; endpoint-nya tidak butuh sesi.
            LaunchedEffect(viewModel) {
                viewModel.onEvent(PilihKartuEvent.CardCatalogRequested)
            }

            BukaRekeningPilihKartuScreen(
                state = state.toPilihKartuUiState(),
                onKartuSelected = { index ->
                    viewModel.onEvent(PilihKartuEvent.CardTypeSelected(index))
                },
                // Tujuan berikutnya dari side effect: kalau sesi sudah ada, kartunya
                // dikirim dulu lewat PUT dan server yang menentukan langkahnya.
                onLanjutClick = { viewModel.onEvent(PilihKartuEvent.CardConfirmed) },
                onBackClick = { navController.popBackStack() },
                onRetry = { viewModel.onEvent(PilihKartuEvent.ErrorDismissed) },
            )
        }

        composable<BukaRekeningSyaratKetentuan> {
            val viewModel: BukaRekeningSyaratKetentuanViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            // Dokumen S&K di luar state flow bersama: umurnya hanya selama layar ini.
            val tnc by viewModel.tnc.collectAsState()
            val flowScope = bukaRekeningFlowScope(navController, it)
            BukaRekeningSideEffects(flowScope, navController)

            // Teksnya datang dari `GET tnc` di `init`; sesi baru dibuat saat tombol
            // ditekan, dengan versi dari dokumen yang sedang terpampang.
            BukaRekeningSyaratKetentuanScreen(
                state = state.toSyaratKetentuanUiState(tnc),
                onAgreeClick = { viewModel.onEvent(SyaratKetentuanEvent.TncAccepted) },
                onRetryClick = { viewModel.onEvent(SyaratKetentuanEvent.TncReloadRequested) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningPanduanFoto> {
            val viewModel: BukaRekeningPanduanFotoViewModel = hiltViewModel()
            val flowScope = bukaRekeningFlowScope(navController, it)
            BukaRekeningSideEffects(flowScope, navController)

            BukaRekeningPanduanFotoScreen(
                onMulaiAmbilFoto = { navController.navigate(BukaRekeningKameraFoto) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningKameraFoto> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningKameraFotoViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

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
                                KameraFotoEvent.KtpPhotoCaptured(
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
                onFlashToggle = { viewModel.onEvent(KameraFotoEvent.FlashModeToggled) },
                onAutoCaptureToggle = {
                    viewModel.onEvent(KameraFotoEvent.AutoCaptureToggled(it))
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
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningHasilFotoViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            // Saat melanjutkan draf, hasil OCR sudah ada di server — ambil dari sana.
            LaunchedEffect(state.sessionId, state.ocr, state.ktpPhoto) {
                // Hanya saat melanjutkan draf: tidak ada foto lokal, tapi server
                // mungkin sudah menyimpan hasil OCR dari sesi sebelumnya.
                if (state.sessionId != null && state.ocr == null && state.ktpPhoto == null) {
                    viewModel.onEvent(HasilFotoEvent.OcrResultRequested)
                }
            }

            BukaRekeningHasilFotoScreen(
                state = state.toHasilFotoUiState(),
                onGunakanFoto = { viewModel.onEvent(HasilFotoEvent.OcrConfirmed) },
                onAmbilUlang = {
                    viewModel.onEvent(HasilFotoEvent.PhotoDiscarded)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningDataPribadi> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningDataPribadiViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            // Prefill dari hasil OCR; ViewModel yang menjaga agar hanya sekali.
            LaunchedEffect(viewModel) {
                viewModel.onEvent(DataPribadiEvent.ScreenShown)
            }

            BukaRekeningDataPribadiScreen(
                state = state.toDataPribadiUiState(),
                onFieldChange = { field, value ->
                    viewModel.onEvent(DataPribadiEvent.FieldChanged(field, value))
                },
                onBirthDateSelect = {
                    viewModel.onEvent(DataPribadiEvent.BirthDateSelected(it))
                },
                onJenisKelaminSelect = {
                    viewModel.onEvent(DataPribadiEvent.GenderSelected(it))
                },
                onAlamatDomisiliToggle = {
                    viewModel.onEvent(DataPribadiEvent.DomicileSameToggled(it))
                },
                onPekerjaanSelect = {
                    viewModel.onEvent(DataPribadiEvent.PekerjaanSelected(it))
                },
                onPenghasilanSelect = {
                    viewModel.onEvent(DataPribadiEvent.PenghasilanSelected(it))
                },
                onSumberDanaSelect = {
                    viewModel.onEvent(DataPribadiEvent.SumberDanaSelected(it))
                },
                onLanjutClick = { viewModel.onEvent(DataPribadiEvent.PersonalDataSubmitted) },
                onSimpanClick = { viewModel.onEvent(DataPribadiEvent.DraftSaveRequested) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningOtp> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningVerifikasiOtpViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            // Kode OTP tidak boleh tertinggal di memory setelah layar ditutup.
            DisposableEffect(viewModel) {
                onDispose { viewModel.onEvent(VerifikasiOtpEvent.OtpCodeCleared) }
            }

            BukaRekeningVerifikasiOtpScreen(
                state = state.toOtpUiState(),
                onKodeChange = { kode ->
                    viewModel.onEvent(VerifikasiOtpEvent.OtpCodeChanged(kode))
                },
                // Perpindahan ke biometrik datang dari `current_step` di response
                // verify-otp, bukan dari callback tombol ini.
                onVerifikasiClick = { viewModel.onEvent(VerifikasiOtpEvent.OtpSubmitted) },
                onKirimUlangClick = { viewModel.onEvent(VerifikasiOtpEvent.OtpResendRequested) },
                // Back kembali ke Data Pribadi supaya nomor HP bisa diperbaiki.
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVerifikasiBiometrik> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningVerifikasiBiometrikViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

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
                    onProgress = { viewModel.onEvent(VerifikasiBiometrikEvent.LivenessProgressed(it)) },
                    onFramesReady = { frames ->
                        scope.launch {
                            val face = CameraCapture.capture(context, active, CACHE_PREFIX_FACE)
                                ?: return@launch
                            viewModel.onEvent(
                                VerifikasiBiometrikEvent.BiometricCaptured(
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
                onMulaiClick = { viewModel.onEvent(VerifikasiBiometrikEvent.LivenessStarted) },
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
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningAntreanVideoCallViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            LaunchedEffect(state.sessionId) {
                if (state.sessionId != null && state.queue == null) {
                    viewModel.onEvent(AntreanVideoCallEvent.QueueJoinRequested)
                }
            }

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = LocalSnackbarHostState.current
            val izinKurang = stringResource(R.string.buka_rekening_antrean_izin_dibutuhkan)
            var hasMedia by remember { mutableStateOf(context.hasVideoCallPermissions()) }
            var sudahDiminta by remember { mutableStateOf(false) }

            // Izin bisa diberikan dari Pengaturan saat aplikasi di latar; kembali dari sana
            // tidak menyusun ulang komposisi, jadi statusnya dibaca lagi tiap ON_RESUME.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasMedia = context.hasVideoCallPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Video call butuh kamera **dan** mikrofon. Diminta saat tombol ditekan, bukan
            // saat layar dibuka: skill `buka-rekening-video-call` §2 — nasabah yang baru
            // melihat posisi antrean belum perlu menyerahkan keduanya.
            val izinLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { hasil ->
                sudahDiminta = true
                hasMedia = hasil.hasRequiredVideoCallPermissions()
                if (hasMedia) {
                    navController.navigate(BukaRekeningVideoCall)
                }
            }

            BukaRekeningAntreanVideoCallScreen(
                state = state.toAntreanUiState(isIzinMediaSiap = hasMedia),
                onTungguClick = {
                    when {
                        hasMedia -> navController.navigate(BukaRekeningVideoCall)
                        // Sudah pernah ditolak: dialog sistem tidak muncul lagi, jadi
                        // satu-satunya jalan tersisa adalah Pengaturan aplikasi.
                        sudahDiminta -> {
                            scope.launch { snackbarHostState.showSnackbar(izinKurang) }
                            context.openAppSettings()
                        }
                        else -> izinLauncher.launch(videoCallRequestedPermissions())
                    }
                },
                // Penjadwalan ulang belum punya endpoint di `06-BUKA-REKENING-API-SPEC.md`;
                // tombolnya dimatikan di layar alih-alih memanggil lambda kosong yang
                // membuat nasabah menekan sesuatu yang tidak pernah terjadi.
                onJadwalkanClick = {},
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVideoCall> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningVideoCallViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // Layar ini menjaga izinnya sendiri, tidak menitipkannya ke layar Antrean.
            // Alasannya bukan kehati-hatian berlebih: Navigation Compose memulihkan back
            // stack, jadi proses yang mati saat panggilan berlangsung dibangkitkan dengan
            // layar ini di puncak — melewati gerbang Antrean sepenuhnya, dan izinnya bisa
            // sudah dicabut OS di sela itu.
            var hasMedia by remember { mutableStateOf(context.hasVideoCallPermissions()) }
            var allowBluetooth by remember {
                mutableStateOf(context.hasVideoCallBluetoothPermission())
            }
            var sudahDiminta by remember { mutableStateOf(false) }

            val izinLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { hasil ->
                sudahDiminta = true
                // Hanya izin wajib yang dinilai: peta hasilnya memuat Bluetooth juga, dan
                // `values.all { it }` akan menganggap panggilan gagal padahal kamera dan
                // mikrofon sudah diberikan.
                hasMedia = hasil.hasRequiredVideoCallPermissions()
                allowBluetooth = context.hasVideoCallBluetoothPermission()
            }

            // Satu sumber untuk ViewModel: masuk pertama, pulih dari kematian proses, dan
            // pencabutan di tengah panggilan semuanya lewat sini.
            LaunchedEffect(hasMedia, allowBluetooth) {
                viewModel.onEvent(
                    VideoCallEvent.MediaPermissionsChanged(
                        granted = hasMedia,
                        allowBluetooth = allowBluetooth,
                    ),
                )
            }

            // Kamera dilepas saat aplikasi ke latar, sambungan dibiarkan hidup — skill §10.
            // PeerConnection sendiri tinggal di ViewModel, jadi rotasi tidak memutusnya.
            // ON_RESUME juga membaca ulang izin: pencabutan dari Pengaturan tidak menyusun
            // ulang komposisi, jadi tanpa ini layar tidak pernah tahu.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> viewModel.pauseLocalVideo()
                        Lifecycle.Event.ON_RESUME -> {
                            hasMedia = context.hasVideoCallPermissions()
                            allowBluetooth = context.hasVideoCallBluetoothPermission()
                            viewModel.resumeLocalVideo()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            BukaRekeningVideoCallScreen(
                state = uiState,
                eglBaseContext = viewModel.eglBaseContext,
                onLocalRendererReady = viewModel::attachLocalRenderer,
                onRemoteRendererReady = viewModel::attachRemoteRenderer,
                onBack = { navController.popBackStack() },
                onMuteToggle = { viewModel.onEvent(VideoCallEvent.MuteToggled) },
                onSwitchCamera = { viewModel.onEvent(VideoCallEvent.CameraSwitched) },
                onEndCall = { viewModel.onEvent(VideoCallEvent.EndCallRequested) },
                // Tiket dibuang lalu kembali ke Antrean, yang mengambil tiket baru begitu
                // melihat `queue == null`.
                onRejoin = {
                    viewModel.onEvent(VideoCallEvent.RejoinRequested)
                    navController.popBackStack()
                },
                onRequestPermissions = {
                    // Sudah pernah ditolak: dialog sistem tidak muncul lagi, jadi
                    // satu-satunya jalan tersisa adalah Pengaturan aplikasi.
                    if (sudahDiminta) {
                        context.openAppSettings()
                    } else {
                        izinLauncher.launch(videoCallRequestedPermissions())
                    }
                },
            )
        }

        composable<BukaRekeningBuatKredensial> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningBuatKredensialViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            BukaRekeningBuatKredensialScreen(
                state = state.toBuatKredensialUiState(),
                onKodeAksesChange = {
                    viewModel.onEvent(BuatKredensialEvent.AccessCodeChanged(it))
                },
                onKonfirmasiKodeAksesChange = {
                    viewModel.onEvent(BuatKredensialEvent.ConfirmAccessCodeChanged(it))
                },
                onToggleKodeAksesVisibility = {
                    viewModel.onEvent(BuatKredensialEvent.AccessCodeVisibilityToggled)
                },
                onToggleKonfirmasiVisibility = {
                    viewModel.onEvent(BuatKredensialEvent.ConfirmAccessCodeVisibilityToggled)
                },
                onSimpanClick = { viewModel.onEvent(BuatKredensialEvent.CredentialsSubmitted) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningRingkasan> {
            val flowScope = bukaRekeningFlowScope(navController, it)
            val viewModel: BukaRekeningRingkasanViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            BukaRekeningSideEffects(flowScope, navController)

            BukaRekeningRingkasanScreen(
                state = state.toRingkasanUiState(),
                onAgreementToggle = {
                    viewModel.onEvent(RingkasanEvent.AgreementToggled(it))
                },
                onUbahRekeningClick = { navController.popBackStack(BukaRekening, false) },
                onUbahNasabahClick = { navController.popBackStack(BukaRekeningDataPribadi, false) },
                onProsesClick = { viewModel.onEvent(RingkasanEvent.ApplicationSubmitted) },
                onSimpanDrafClick = { viewModel.onEvent(RingkasanEvent.DraftSaveRequested) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningBerhasilDibuat> {
            val viewModel: BukaRekeningBerhasilDibuatViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val flowScope = bukaRekeningFlowScope(navController, it)
            BukaRekeningSideEffects(flowScope, navController)

            BukaRekeningBerhasilDibuatScreen(
                state = state.toBerhasilDibuatUiState(),
                onMasukMbcaClick = {
                    navController.navigate(Login) {
                        popUpTo<BukaRekening> { inclusive = true }
                    }
                },
                onBagikanClick = {},
                onSalinClick = { nomor ->
                    viewModel.onEvent(BerhasilDibuatEvent.NomorRekeningDisalin(nomor))
                },
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
 * Penanda umur flow buka rekening.
 *
 * Di-scope ke back stack entry [BukaRekening] — layar pertama flow — sehingga
 * seluruh layar sesudahnya memakai instance yang sama. Saat flow keluar dari back
 * stack, instance ini dibuang dan membersihkan `BukaRekeningSessionStore`
 * berikut seluruh PII di dalamnya.
 *
 * ViewModel tiap layar di-resolve dengan `hiltViewModel()` biasa; state bersamanya
 * datang dari store, bukan dari instance ini.
 */
@Composable
private fun bukaRekeningFlowScope(
    navController: NavHostController,
    entry: NavBackStackEntry,
): BukaRekeningFlowScopeViewModel {
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
    viewModel: BukaRekeningFlowScopeViewModel,
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
    OnboardingStep.CARD_SELECTION -> BukaRekeningPilihKartu
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
