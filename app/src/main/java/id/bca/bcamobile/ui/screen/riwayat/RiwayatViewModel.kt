package id.bca.bcamobile.ui.screen.riwayat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.R
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transaction.TransactionRepository
import id.bca.bcamobile.domain.transaction.model.HistoryItem
import id.bca.bcamobile.domain.transaction.model.TransactionPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Riwayat transaksi nasabah dari `GET /transactions/history`.
 *
 * Halaman berikutnya selalu lewat `pagination.cursor`; tidak ada nomor halaman.
 * Ganti filter berarti mulai dari awal — cursor lama milik filter lama.
 */
@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val repository: TransactionRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiwayatUiState())
    val uiState: StateFlow<RiwayatUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null
    private var isLoadingPage = false
    private val loaded = mutableListOf<HistoryItem>()

    /** Hanya terisi saat rentang kustom dipilih; keduanya `yyyy-MM-dd`. */
    private var customStartDate: String? = null
    private var customEndDate: String? = null

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        nextCursor = null
        viewModelScope.launch { fetch(reset = true) }
    }

    fun onFilterSelected(filter: RiwayatFilter) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        load()
    }

    /**
     * Rentang siap pakai. [RiwayatPeriod.CUSTOM] tidak lewat sini — ia butuh
     * dua tanggal dan datang dari [onCustomRangeSelected].
     */
    fun onPeriodSelected(period: RiwayatPeriod) {
        if (period == RiwayatPeriod.CUSTOM) return
        if (period == _uiState.value.selectedPeriod) return
        customStartDate = null
        customEndDate = null
        _uiState.update { it.copy(selectedPeriod = period, customRangeLabel = "") }
        load()
    }

    /**
     * Rentang bebas dari layar Rentang Waktu. Keduanya `yyyy-MM-dd`; server
     * mewajibkan dua-duanya saat `period=CUSTOM`, jadi rentang setengah jadi
     * tidak dikirim sama sekali.
     */
    fun onCustomRangeSelected(startDate: String, endDate: String) {
        if (startDate.isBlank() || endDate.isBlank()) return
        customStartDate = startDate
        customEndDate = endDate
        _uiState.update {
            it.copy(
                selectedPeriod = RiwayatPeriod.CUSTOM,
                customRangeLabel = rangeLabel(startDate, endDate),
            )
        }
        load()
    }

    fun onLoadMore() {
        if (isLoadingPage || nextCursor == null) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch { fetch(reset = false) }
    }

    /**
     * `2026-09-01` + `2026-09-26` menjadi `1 Sep 2026 – 26 Sep 2026`.
     * Pemisahnya dari `strings.xml`, bukan literal di kode.
     */
    private fun rangeLabel(startDate: String, endDate: String): String {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return ""
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return ""
        return context.getString(
            R.string.riwayat_rentang_format,
            GROUP_DATE.format(start),
            GROUP_DATE.format(end),
        )
    }

    private suspend fun fetch(reset: Boolean) {
        isLoadingPage = true
        val state = _uiState.value
        val result = repository.history(
            type = state.selectedFilter.wireValue,
            period = state.selectedPeriod.toDomain(),
            startDate = customStartDate,
            endDate = customEndDate,
            cursor = if (reset) null else nextCursor,
        )
        isLoadingPage = false

        when (result) {
            is DataResult.Success -> {
                if (reset) loaded.clear()
                loaded += result.value.items
                nextCursor = result.value.nextCursor
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        groups = loaded.toGroups(),
                        hasMore = result.value.hasMore,
                    )
                }
            }

            is DataResult.Failure -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = result.error.messageOrNull(),
                )
            }
        }
    }
}

// -- Pemetaan domain → tampilan ------------------------------------------------

/**
 * Nilai query `type`.
 *
 * **"Semua" memetakan ke null, bukan `"ALL"`.** Handler menyaring dengan
 * `AND type = $n` apa adanya, jadi `type=ALL` tidak cocok dengan satu baris pun
 * dan tab Semua tampil kosong padahal riwayatnya ada.
 */
private val RiwayatFilter.wireValue: String?
    get() = when (this) {
        RiwayatFilter.ALL -> null
        RiwayatFilter.TRANSFER -> "TRANSFER"
        RiwayatFilter.EWALLET -> "EWALLET"
        RiwayatFilter.PAYMENT -> "PAYMENT"
        RiwayatFilter.PULSA -> "PULSA"
    }

/** `SEMUA` berarti tanpa parameter `period`, bukan sebuah nilai. */
private fun RiwayatPeriod.toDomain(): TransactionPeriod? = when (this) {
    RiwayatPeriod.SEMUA -> null
    RiwayatPeriod.LAST_7_DAYS -> TransactionPeriod.LAST_7_DAYS
    RiwayatPeriod.LAST_30_DAYS -> TransactionPeriod.LAST_30_DAYS
    RiwayatPeriod.THIS_MONTH -> TransactionPeriod.THIS_MONTH
    RiwayatPeriod.LAST_MONTH -> TransactionPeriod.LAST_MONTH
    RiwayatPeriod.CUSTOM -> TransactionPeriod.CUSTOM
}



private val INDONESIAN = Locale.forLanguageTag("id-ID")
private val GROUP_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", INDONESIAN)
private val ROW_TIME = DateTimeFormatter.ofPattern("HH:mm", INDONESIAN)

/**
 * Mengelompokkan per tanggal dengan mempertahankan urutan dari server.
 *
 * `created_at` adalah UTC; tanggal dan jam yang ditampilkan dikonversi ke zona
 * waktu perangkat, kalau tidak transaksi jelang tengah malam masuk grup yang salah.
 */
private fun List<HistoryItem>.toGroups(): List<RiwayatGroup> {
    val groups = LinkedHashMap<String, MutableList<RiwayatItem>>()
    forEach { item ->
        val instant = item.createdAt.toInstantOrNull()
        val date = instant?.format(GROUP_DATE) ?: item.createdAt
        val time = instant?.format(ROW_TIME).orEmpty()
        groups.getOrPut(date) { mutableListOf() } += item.toRiwayatItem(time)
    }
    return groups.map { (date, items) -> RiwayatGroup(date = date, items = items) }
}

private fun HistoryItem.toRiwayatItem(time: String): RiwayatItem = RiwayatItem(
    id = id,
    title = description.ifBlank { destinationName },
    description = listOf(destinationName.ifBlank { destination }, time)
        .filter { it.isNotBlank() }
        .joinToString(SEPARATOR),
    // Riwayat hanya memuat transaksi yang dimulai nasabah, jadi selalu keluar.
    amount = CurrencyFormatter.signedRupiah(amount, isCredit = false),
    referenceNumber = referenceNumber,
    status = status.toRiwayatStatus(),
    jenis = type.toRiwayatJenis(),
)

private const val SEPARATOR = " • "

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

private fun Instant.format(formatter: DateTimeFormatter): String =
    formatter.format(atZone(ZoneId.systemDefault()))

/** Status yang tidak dikenal ditampilkan sebagai diproses, bukan berhasil. */
private fun String.toRiwayatStatus(): RiwayatStatus = when (uppercase(Locale.ROOT)) {
    "SUCCESS", "COMPLETED" -> RiwayatStatus.SUCCESS
    "FAILED", "REJECTED", "CANCELLED", "EXPIRED" -> RiwayatStatus.FAILED
    else -> RiwayatStatus.PENDING
}

private fun String.toRiwayatJenis(): RiwayatJenis {
    val value = uppercase(Locale.ROOT)
    return when {
        value.contains("QRIS") -> RiwayatJenis.QRIS
        value.contains("EWALLET") -> RiwayatJenis.EWALLET
        value.contains("PULSA") || value.contains("DATA") -> RiwayatJenis.PULSA
        value.contains("TRANSFER") -> RiwayatJenis.TRANSFER
        else -> RiwayatJenis.LAINNYA
    }
}
