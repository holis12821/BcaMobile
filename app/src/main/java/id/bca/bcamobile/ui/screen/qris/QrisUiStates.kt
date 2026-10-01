package id.bca.bcamobile.ui.screen.qris

import androidx.compose.runtime.Composable
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState

/** Pemetaan state alur QRIS ke state tiap layar. */
@Composable
fun QrisFlowState.toScanUiState(): ScanQrisUiState = ScanQrisUiState(
    isFlashOn = isFlashOn,
    isDecoding = isDecoding,
    error = error,
)

@Composable
fun QrisFlowState.toKonfirmasiUiState(): QrisKonfirmasiUiState = QrisKonfirmasiUiState(
    isLoading = isSubmitting,
    error = error,
    merchantName = payload?.merchantName.orEmpty(),
    merchantCity = payload?.merchantCity.orEmpty(),
    sourceAccount = "$sourceAccountName ($sourceAccountNumber)",
    sourceBalance = CurrencyFormatter.rupiah(sourceBalance),
    isAmountEditable = isAmountEditable,
    amountInput = amountInput,
    amount = CurrencyFormatter.rupiah(amount),
    // Biaya admin QRIS baru diketahui dari response pembayaran; ringkasan memakai nol.
    adminFee = CurrencyFormatter.rupiah(0L),
    total = CurrencyFormatter.rupiah(total),
    isInsufficientBalance = amount > 0 && !hasEnoughBalance,
    isPayEnabled = isPayEnabled,
)

@Composable
fun QrisFlowState.toPinUiState(): KodeAksesUiState = KodeAksesUiState(
    enteredDigits = pinDigits,
    isError = isPinError,
    errorMessage = error,
    isSubmitting = isSubmitting,
)

@Composable
fun QrisFlowState.toBuktiUiState(): BuktiTransaksiUiState {
    val data = receipt
    return BuktiTransaksiUiState(
        isLoading = isSubmitting,
        error = error,
        tanggal = data?.createdAt.orEmpty(),
        noReferensi = data?.referenceNumber.orEmpty(),
        sumberRekening = "${data?.sourceName.orEmpty()} (${data?.sourceAccount.orEmpty()})",
        namaPengirim = data?.sourceName.orEmpty(),
        nomorTujuan = data?.merchantCity.orEmpty(),
        namaTujuan = data?.merchantName.orEmpty(),
        jenisTransaksi = data?.status.orEmpty(),
        nominal = CurrencyFormatter.rupiah(data?.amount ?: 0L),
        biayaAdmin = CurrencyFormatter.rupiah(data?.adminFee ?: 0L),
        total = CurrencyFormatter.rupiah(data?.total ?: 0L),
    )
}
