package id.bca.bcamobile.ui.screen.ewallet

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.ui.screen.bukti_transaksi.BuktiTransaksiUiState
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState

/**
 * Pemetaan state alur ke state tiap layar.
 *
 * Catatan warna: `EWalletOption` meminta `brandColor`, tetapi API hanya mengirim
 * `icon_url` dan **belum ada token warna merek** di `ui/theme/`. Sesuai aturan wajib
 * #2 di CLAUDE.md, warna merek tidak dikarang di sini — semua penyedia memakai satu
 * token netral sampai desain menyediakan tokennya.
 */
@Composable
fun EWalletFlowState.toTopUpUiState(): TopUpEWalletUiState {
    val neutralBrand = MaterialTheme.colorScheme.primaryContainer
    return TopUpEWalletUiState(
        isLoading = isLoading,
        error = error,
        accountName = sourceAccountName,
        accountNumber = sourceAccountNumber,
        balance = CurrencyFormatter.rupiah(sourceBalance),
        walletOptions = providers.map {
            EWalletOption(id = it.id, name = it.name, brandColor = neutralBrand)
        },
        selectedWalletId = selectedProviderId,
        phoneNumber = phoneNumber,
        presetAmounts = selectedProvider?.presetAmounts.orEmpty().map {
            PresetAmount(value = it, label = CurrencyFormatter.rupiah(it))
        },
        selectedPresetIndex = selectedPresetIndex,
        manualAmount = manualAmount,
        isFormValid = isFormValid && !isSubmitting,
        insufficientBalance = selectedProviderId != null && amount > 0 && !hasEnoughBalance,
    )
}

@Composable
fun EWalletFlowState.toConfirmUiState(): ConfirmEWalletUiState {
    val data = inquiry
    return ConfirmEWalletUiState(
        isLoading = isSubmitting,
        error = error,
        destinationName = data?.destinationName.orEmpty(),
        destinationProvider = data?.provider.orEmpty(),
        destinationPhone = data?.destinationPhone.orEmpty(),
        sourceAccount = "$sourceAccountName ($sourceAccountNumber)",
        nominalAmount = CurrencyFormatter.rupiah(data?.amount ?: 0L),
        adminFee = CurrencyFormatter.rupiah(data?.adminFee ?: 0L),
        totalAmount = CurrencyFormatter.rupiah(data?.total ?: 0L),
    )
}

@Composable
fun EWalletFlowState.toPinUiState(): KodeAksesUiState = KodeAksesUiState(
    enteredDigits = pinDigits,
    isError = isPinError,
    errorMessage = error,
    isSubmitting = isSubmitting,
)

@Composable
fun EWalletFlowState.toBuktiUiState(): BuktiTransaksiUiState {
    val data = receipt
    return BuktiTransaksiUiState(
        isLoading = isSubmitting,
        error = error,
        tanggal = data?.createdAt.orEmpty(),
        noReferensi = data?.referenceNumber.orEmpty(),
        sumberRekening = "${data?.sourceName.orEmpty()} (${data?.sourceAccount.orEmpty()})",
        namaPengirim = data?.sourceName.orEmpty(),
        nomorTujuan = "${data?.provider.orEmpty()} - ${data?.destinationPhone.orEmpty()}",
        namaTujuan = data?.destinationName.orEmpty(),
        jenisTransaksi = data?.provider.orEmpty(),
        nominal = CurrencyFormatter.rupiah(data?.amount ?: 0L),
        biayaAdmin = CurrencyFormatter.rupiah(data?.adminFee ?: 0L),
        total = CurrencyFormatter.rupiah(data?.total ?: 0L),
    )
}
