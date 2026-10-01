package id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call

import androidx.compose.runtime.Composable
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState

data class AntreanVideoCallUiState(
    val nomorAntrean: String = "A-042",
    val jumlahAntreanDepan: Int = 2,
    val estimasiMenit: Int = 3,
    val petugasSiap: Boolean = true,
    val isKtpReady: Boolean = true,
    val isKoneksiStabil: Boolean = true,
    val isRuanganTenang: Boolean = true,
)

fun BukaRekeningFlowState.toAntreanUiState(): AntreanVideoCallUiState {
    val ticket = queue ?: return AntreanVideoCallUiState(
        nomorAntrean = "",
        jumlahAntreanDepan = 0,
        estimasiMenit = 0,
        petugasSiap = false,
    )
    return AntreanVideoCallUiState(
        nomorAntrean = ticket.queueNumber,
        jumlahAntreanDepan = ticket.position,
        estimasiMenit = ticket.estimatedWaitSeconds / SECONDS_PER_MINUTE,
        petugasSiap = ticket.position == 0,
    )
}

private const val SECONDS_PER_MINUTE = 60
