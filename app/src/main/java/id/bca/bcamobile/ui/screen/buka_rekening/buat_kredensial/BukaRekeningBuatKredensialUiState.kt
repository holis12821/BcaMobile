package id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.CREDENTIAL_LENGTH
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.isAllSameChar
import id.bca.bcamobile.ui.screen.buka_rekening.common.isSequential

data class BuatKredensialUiState(
    val kodeAkses: String = "",
    val konfirmasiKodeAkses: String = "",
    val isKodeAksesVisible: Boolean = false,
    val isKonfirmasiVisible: Boolean = false,
    val pinDigitCount: Int = 0,
    val konfirmasiPinDigitCount: Int = 0,
    val isPinCocok: Boolean = false,
    val isValid6Karakter: Boolean = false,
    val isValidTidakBerurutan: Boolean = false,
    val isValidTidakBerulang: Boolean = false,
)

fun BukaRekeningFlowState.toBuatKredensialUiState(): BuatKredensialUiState =
    BuatKredensialUiState(
        kodeAkses = accessCode,
        konfirmasiKodeAkses = confirmAccessCode,
        isKodeAksesVisible = isAccessCodeVisible,
        isKonfirmasiVisible = isConfirmAccessCodeVisible,
        pinDigitCount = pin.length,
        konfirmasiPinDigitCount = confirmPin.length,
        isPinCocok = pin.isNotEmpty() && pin == confirmPin,
        isValid6Karakter = accessCode.length == CREDENTIAL_LENGTH,
        isValidTidakBerurutan = accessCode.isNotEmpty() && !accessCode.isSequential(),
        isValidTidakBerulang = accessCode.isNotEmpty() && !accessCode.isAllSameChar(),
    )
