package id.bca.bcamobile.ui.screen.bukti_transaksi

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.R
import id.bca.bcamobile.core.download.ReceiptPdfSaver
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transaction.TransactionRepository
import id.bca.bcamobile.domain.transaction.model.Receipt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BuktiTransaksiEvent {
    /** Unduhan selesai — berhasil maupun gagal; pesannya sudah siap tampil. */
    data class SaveFinished(val message: String) : BuktiTransaksiEvent
}

/**
 * Bukti transaksi yang dibuka dari daftar — Riwayat atau Mutasi.
 *
 * Berbeda dari bukti yang muncul di akhir alur transfer atau top-up: di sana
 * datanya sudah ada di state alur, sedangkan di sini struknya ditarik ulang dari
 * `GET /transactions/{id}/receipt`.
 */
@HiltViewModel
class BuktiTransaksiViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val pdfSaver: ReceiptPdfSaver,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val transactionId: String = savedStateHandle[ARG_TRANSACTION_ID] ?: ""

    private val _uiState = MutableStateFlow(BuktiTransaksiUiState(isLoading = true))
    val uiState: StateFlow<BuktiTransaksiUiState> = _uiState.asStateFlow()

    private val _events = Channel<BuktiTransaksiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        if (transactionId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.receipt(transactionId)) {
                is DataResult.Success -> _uiState.value = result.value.toUiState()
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    /**
     * Mengunduh struk PDF lalu menyimpannya ke folder Unduhan.
     *
     * Berkasnya ditarik ulang dari `GET /transactions/{id}/receipt/pdf`, bukan
     * dirender di client: yang dibagikan nasabah harus struk yang sama dengan
     * yang dipegang bank.
     */
    fun onSimpanClick() {
        if (transactionId.isBlank() || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = repository.receiptPdf(transactionId)
            _uiState.update { it.copy(isSaving = false) }

            val message = when (result) {
                is DataResult.Success -> {
                    val fileName = pdfSaver.save(
                        bytes = result.value,
                        referenceNumber = _uiState.value.noReferensi,
                    )
                    if (fileName != null) {
                        context.getString(R.string.bukti_transaksi_tersimpan, fileName)
                    } else {
                        context.getString(R.string.bukti_transaksi_gagal_simpan)
                    }
                }

                is DataResult.Failure -> result.error.messageOrNull()
                    ?: context.getString(R.string.bukti_transaksi_gagal_unduh)
            }

            _events.send(BuktiTransaksiEvent.SaveFinished(message))
        }
    }

    private companion object {
        /** Harus sama dengan nama properti di route `BuktiTransaksi`. */
        const val ARG_TRANSACTION_ID = "transactionId"
    }
}

private fun Receipt.toUiState(): BuktiTransaksiUiState = BuktiTransaksiUiState(
    isLoading = false,
    error = null,
    tanggal = listOf(date, time).filter { it.isNotBlank() }.joinToString(DATE_TIME_SEPARATOR),
    noReferensi = referenceNumber,
    sumberRekening = sourceAccount,
    namaPengirim = sourceName,
    // Top-up e-wallet memakai nomor tujuan; transfer memakai nomor rekening.
    nomorTujuan = destinationNumber,
    namaTujuan = destinationName.ifBlank { provider.orEmpty() },
    jenisTransaksi = type,
    nominal = CurrencyFormatter.rupiah(amount),
    biayaAdmin = CurrencyFormatter.rupiah(adminFee),
    total = CurrencyFormatter.rupiah(total),
)

private const val DATE_TIME_SEPARATOR = " • "
