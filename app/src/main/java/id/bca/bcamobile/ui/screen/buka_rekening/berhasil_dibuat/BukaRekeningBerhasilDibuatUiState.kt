package id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.formatAmount
import id.bca.bcamobile.ui.screen.buka_rekening.common.groupNik

data class BerhasilDibuatUiState(
    val jenisRekening: String = "",
    val nomorRekening: String = "",
    val namaPemilik: String = "",
    val kantorCabang: String = "",
    val minimumSetoran: String = "",
)

@Composable
fun BukaRekeningFlowState.toBerhasilDibuatUiState(): BerhasilDibuatUiState {
    val created = account ?: return BerhasilDibuatUiState()
    return BerhasilDibuatUiState(
        jenisRekening = product?.name ?: created.accountType,
        nomorRekening = created.accountNumber.groupNik(),
        namaPemilik = created.accountHolder,
        kantorCabang = created.branch,
        minimumSetoran = stringResource(
            R.string.buka_rekening_ringkasan_setoran_format,
            formatAmount(created.minInitialDeposit),
        ),
    )
}
