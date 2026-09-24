package id.bca.bcamobile.ui.screen.transfer

import androidx.compose.runtime.Composable
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState

private const val HIDDEN_BALANCE = "••••••••"

@Composable
fun TransferFlowState.toTransferUiState(): TransferUiState = TransferUiState(
    balance = if (isBalanceVisible) CurrencyFormatter.rupiah(sourceBalance) else HIDDEN_BALANCE,
    isBalanceVisible = isBalanceVisible,
    accountNumber = sourceAccountNumber,
    searchQuery = searchQuery,
    recentTransfers = recents
        .filter { searchQuery.isBlank() || it.accountName.contains(searchQuery, true) }
        .map {
            RecentTransferItem(
                id = it.accountNumber,
                name = it.accountName,
                bankAccount = "${it.bankName} - ${it.accountNumber}",
                initial = it.accountName.firstOrNull()?.uppercaseChar() ?: '?',
            )
        },
)

@Composable
fun TransferFlowState.toAntarRekeningUiState(): TransferAntarRekeningUiState =
    TransferAntarRekeningUiState(
        currentStep = if (inquiry == null) 1 else 2,
        sourceAccountType = sourceAccountLabel,
        sourceAccountNumber = sourceAccountNumber,
        sourceBalance = CurrencyFormatter.rupiah(sourceBalance),
        destinationAccount = destinationAccount,
        amount = amountInput,
        notes = notes,
        isLanjutEnabled = isFormValid,
    )

@Composable
fun TransferFlowState.toPinUiState(): KodeAksesUiState = KodeAksesUiState(
    enteredDigits = pinDigits,
    isError = isPinError,
    errorMessage = error,
    isSubmitting = isSubmitting,
)

@Composable
fun TransferFlowState.toBuktiUiState(): BuktiTransaksiUiState {
    val data = receipt
    return BuktiTransaksiUiState(
        isLoading = isSubmitting,
        error = error,
        tanggal = data?.createdAt.orEmpty(),
        noReferensi = data?.referenceNumber.orEmpty(),
        sumberRekening = "${data?.sourceName.orEmpty()} (${data?.sourceAccount.orEmpty()})",
        namaPengirim = data?.sourceName.orEmpty(),
        nomorTujuan = "${data?.destinationBank.orEmpty()} - ${data?.destinationAccount.orEmpty()}",
        namaTujuan = data?.destinationName.orEmpty(),
        jenisTransaksi = data?.destinationBank.orEmpty(),
        nominal = CurrencyFormatter.rupiah(data?.amount ?: 0L),
        biayaAdmin = CurrencyFormatter.rupiah(data?.adminFee ?: 0L),
        total = CurrencyFormatter.rupiah(data?.total ?: 0L),
    )
}
