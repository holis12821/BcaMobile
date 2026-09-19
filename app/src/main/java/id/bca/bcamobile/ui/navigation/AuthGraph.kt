package id.bca.bcamobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.faceid.FaceIdScreen
import id.bca.bcamobile.ui.screen.faceid.FaceIdUiState
import id.bca.bcamobile.ui.screen.finger_print.TouchIdScreen
import id.bca.bcamobile.ui.screen.finger_print.TouchIdUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesScreen
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPilihJenisUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPilihJenisScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningDataPribadiScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningDataPribadiUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningVerifikasiBiometrikScreen
import id.bca.bcamobile.ui.screen.buka_rekening.VerifikasiBiometrikUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningAntreanVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.AntreanVideoCallUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningVideoCallScreen
import id.bca.bcamobile.ui.screen.buka_rekening.VideoCallUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningBuatKredensialScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BuatKredensialUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningRingkasanScreen
import id.bca.bcamobile.ui.screen.buka_rekening.RingkasanUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningBerhasilDibuatScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BerhasilDibuatUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningHasilFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningHasilFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningKameraFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningKameraFotoUiState
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningPanduanFotoScreen
import id.bca.bcamobile.ui.screen.buka_rekening.BukaRekeningSyaratKetentuanScreen
import id.bca.bcamobile.ui.screen.buka_rekening.defaultJenisRekeningList
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
            val viewModel: KodeAksesViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            KodeAksesScreen(
                state = state,
                onDigitClick = viewModel::onDigitClick,
                onDeleteClick = viewModel::onDeleteClick,
                onCancelClick = { navController.popBackStack() },
                onSubmitClick = {
                    if (viewModel.submit()) {
                        onAuthenticated()
                    } else {
                        viewModel.showError()
                    }
                },
                onForgotClick = {},
            )
        }

        composable<FaceId> {
            FaceIdScreen(
                state = FaceIdUiState(),
                onBackClick = { navController.popBackStack() },
                onRetryClick = {},
                onCancelClick = { navController.popBackStack() },
            )
        }

        composable<TouchId> {
            TouchIdScreen(
                state = TouchIdUiState(),
                onFingerprintPress = {},
                onUseAccessCode = {
                    navController.navigate(KodeAkses) {
                        popUpTo<Login> { inclusive = false }
                    }
                },
                onRetryClick = {},
            )
        }

        composable<BukaRekening> {
            BukaRekeningPilihJenisScreen(
                state = BukaRekeningPilihJenisUiState(
                    jenisRekeningList = defaultJenisRekeningList(),
                ),
                onJenisSelected = { navController.navigate(BukaRekeningSyaratKetentuan) },
                onBackClick = { navController.popBackStack() },
                onRetry = {},
            )
        }

        composable<BukaRekeningSyaratKetentuan> {
            BukaRekeningSyaratKetentuanScreen(
                onAgreeClick = { navController.navigate(BukaRekeningPanduanFoto) },
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
            BukaRekeningKameraFotoScreen(
                state = BukaRekeningKameraFotoUiState(),
                onShutterClick = { navController.navigate(BukaRekeningHasilFoto) },
                onGalleryClick = { /* TODO: open gallery */ },
                onHelpClick = { /* TODO: show help */ },
                onFlashToggle = { /* TODO: toggle flash */ },
                onAutoCaptureToggle = { /* TODO: toggle auto-capture */ },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningHasilFoto> {
            BukaRekeningHasilFotoScreen(
                state = BukaRekeningHasilFotoUiState(),
                onGunakanFoto = { navController.navigate(BukaRekeningDataPribadi) },
                onAmbilUlang = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningDataPribadi> {
            BukaRekeningDataPribadiScreen(
                state = BukaRekeningDataPribadiUiState(),
                onJenisKelaminSelect = { /* TODO */ },
                onAlamatDomisiliToggle = { /* TODO */ },
                onLanjutClick = { navController.navigate(BukaRekeningEkyc) },
                onSimpanClick = { /* TODO: save draft */ },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVerifikasiBiometrik> {
            BukaRekeningVerifikasiBiometrikScreen(
                state = VerifikasiBiometrikUiState(faceDetected = true),
                onMulaiClick = { navController.navigate(BukaRekeningAntreanVideoCall) },
                onTipsClick = { /* TODO: show tips dialog */ },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningAntreanVideoCall> {
            BukaRekeningAntreanVideoCallScreen(
                state = AntreanVideoCallUiState(),
                onTungguClick = { navController.navigate(BukaRekeningVideoCall) },
                onJadwalkanClick = { /* TODO: show schedule picker */ },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningVideoCall> {
            BukaRekeningVideoCallScreen(
                state = VideoCallUiState(
                    instruksiPetugas = "Mohon posisikan fisik e-KTP Anda di depan kamera samping wajah.",
                ),
                onBack = { navController.popBackStack() },
                onMuteToggle = { /* TODO: toggle mute */ },
                onSwitchCamera = { /* TODO: switch camera */ },
                onEndCall = {
                    navController.navigate(BukaRekeningBuatKredensial) {
                        popUpTo<BukaRekeningVideoCall> { inclusive = true }
                    }
                },
            )
        }

        composable<BukaRekeningBuatKredensial> {
            BukaRekeningBuatKredensialScreen(
                state = BuatKredensialUiState(
                    kodeAkses = "Bca202",
                    konfirmasiKodeAkses = "Bca202",
                    pinDigitCount = 6,
                    konfirmasiPinDigitCount = 6,
                    isPinCocok = true,
                    isValid6Karakter = true,
                    isValidTidakBerurutan = true,
                    isValidTidakBerulang = true,
                ),
                onKodeAksesChange = { /* TODO */ },
                onKonfirmasiKodeAksesChange = { /* TODO */ },
                onToggleKodeAksesVisibility = { /* TODO */ },
                onToggleKonfirmasiVisibility = { /* TODO */ },
                onSimpanClick = { navController.navigate(BukaRekeningRingkasan) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningRingkasan> {
            BukaRekeningRingkasanScreen(
                state = RingkasanUiState(
                    produkRekening = "Tahapan BCA",
                    isPalingPopuler = true,
                    mataUang = "IDR (Rupiah)",
                    setoranAwalMinimum = "Rp 500.000",
                    fasilitasDigital = "Paspor BCA Mastercard Debit, m-BCA & KlikBCA",
                    namaLengkap = "MUHAMMAD ARDAN PRAYOGI",
                    nik = "3174 0821 0495 0001",
                    isNikVerified = true,
                    tempatTanggalLahir = "Jakarta, 21 Apr 1995",
                    alamatKtp = "Jl. Sudirman Kav. 45 No. 12B, Jakarta Selatan",
                    pekerjaan = "Karyawan Swasta",
                    nomorHandphone = "0812 \u2022\u2022\u2022\u2022 8889",
                    isOtpVerified = true,
                    alamatEmail = "m.ardan@example.com",
                    isOcrVerified = true,
                    isBiometrikVerified = true,
                    isVideoCallVerified = true,
                    videoCallCsName = "Sarah Adisti",
                ),
                onAgreementToggle = { /* TODO */ },
                onUbahRekeningClick = { /* TODO */ },
                onUbahNasabahClick = { /* TODO */ },
                onProsesClick = { navController.navigate(BukaRekeningBerhasilDibuat) },
                onSimpanDrafClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<BukaRekeningBerhasilDibuat> {
            BukaRekeningBerhasilDibuatScreen(
                state = BerhasilDibuatUiState(
                    jenisRekening = "Tahapan BCA",
                    nomorRekening = "5420 8912 34",
                    namaPemilik = "MUHAMMAD ARDAN PRAYOGI",
                    kantorCabang = "KCU Jakarta Thamrin",
                    minimumSetoran = "Rp 500.000",
                ),
                onMasukMbcaClick = { /* TODO: navigate to main graph */ },
                onBagikanClick = { /* TODO: share account number */ },
                onSalinClick = { /* TODO: copy to clipboard */ },
            )
        }

        composable<GantiKodeAkses> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.navigation_change_access_code))
            }
        }
    }
}