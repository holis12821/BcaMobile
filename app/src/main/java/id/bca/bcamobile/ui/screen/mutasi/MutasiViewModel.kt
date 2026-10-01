package id.bca.bcamobile.ui.screen.mutasi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transaction.TransactionRepository
import id.bca.bcamobile.domain.transaction.model.Mutation
import id.bca.bcamobile.domain.transaction.model.MutationType
import id.bca.bcamobile.domain.transaction.model.TransactionPeriod
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MutasiViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MutasiUiState())
    val uiState: StateFlow<MutasiUiState> = _uiState.asStateFlow()

    private var accountId: String? = null
    private var customStartDate: String? = null
    private var customEndDate: String? = null
    private var nextCursor: String? = null
    private var isLoadingPage = false
    private val loaded = mutableListOf<Mutation>()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loaded.clear()
        nextCursor = null
        viewModelScope.launch {
            val id = accountId ?: resolvePrimaryAccountId() ?: return@launch
            fetch(id, reset = true)
        }
    }

    fun onPeriodSelected(period: MutasiPeriod) {
        if (period == _uiState.value.selectedPeriod) return
        customStartDate = null
        customEndDate = null
        _uiState.update { it.copy(selectedPeriod = period, periodInfo = "") }
        load()
    }

    /**
     * Rentang bebas dari layar Rentang Waktu.
     *
     * [startDate] dan [endDate] berformat `yyyy-MM-dd` seperti yang diminta
     * `GET /transactions/mutations`; keduanya wajib ada saat `period=CUSTOM`.
     */
    fun onCustomRangeSelected(startDate: String, endDate: String) {
        customStartDate = startDate
        customEndDate = endDate
        _uiState.update {
            it.copy(selectedPeriod = MutasiPeriod.CUSTOM, periodInfo = "$startDate - $endDate")
        }
        load()
    }

    /** Halaman berikutnya memakai cursor; jangan pernah memuat ulang dari awal di sini. */
    fun onLoadMore() {
        val id = accountId ?: return
        if (isLoadingPage || nextCursor == null) return
        viewModelScope.launch { fetch(id, reset = false) }
    }

    /**
     * Mutasi butuh `account_id`, sedangkan dashboard hanya membalas nomor rekening.
     * Karena itu id rekening utama diambil sekali dari profil lalu disimpan.
     */
    private suspend fun resolvePrimaryAccountId(): String? =
        when (val result = accountRepository.profile()) {
            is DataResult.Success -> {
                val primary = result.value.accounts.firstOrNull { it.isPrimary }
                    ?: result.value.accounts.firstOrNull()
                if (primary == null) {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    null
                } else {
                    accountId = primary.id
                    _uiState.update { it.copy(accountLabel = primary.label) }
                    primary.id
                }
            }

            is DataResult.Failure -> {
                _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
                null
            }
        }

    private suspend fun fetch(id: String, reset: Boolean) {
        isLoadingPage = true
        val result = transactionRepository.mutations(
            accountId = id,
            period = _uiState.value.selectedPeriod.toDomain(),
            startDate = customStartDate,
            endDate = customEndDate,
            cursor = if (reset) null else nextCursor,
        )
        isLoadingPage = false

        when (result) {
            is DataResult.Success -> {
                if (reset) loaded.clear()
                loaded += result.value.page.items
                nextCursor = result.value.page.nextCursor
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        accountLabel = result.value.accountLabel.ifBlank { it.accountLabel },
                        balance = CurrencyFormatter.rupiah(result.value.balance),
                        transactionGroups = loaded.toGroups(),
                        hasMore = result.value.page.hasMore,
                    )
                }
            }

            is DataResult.Failure -> _uiState.update {
                it.copy(isLoading = false, error = result.error.messageOrNull())
            }
        }
    }
}

// -- Pemetaan ke bentuk tampilan ----------------------------------------------

private fun MutasiPeriod.toDomain(): TransactionPeriod = when (this) {
    MutasiPeriod.LAST_7_DAYS -> TransactionPeriod.LAST_7_DAYS
    MutasiPeriod.THIS_MONTH -> TransactionPeriod.THIS_MONTH
    MutasiPeriod.LAST_MONTH -> TransactionPeriod.LAST_MONTH
    MutasiPeriod.CUSTOM -> TransactionPeriod.CUSTOM
}

/** Server sudah mengurutkan dari terbaru, jadi urutan grup mengikuti urutan datang. */
private fun List<Mutation>.toGroups(): List<TransactionGroup> =
    groupBy { it.date }.map { (date, items) ->
        TransactionGroup(
            date = date,
            transactions = items.map { it.toItem() },
        )
    }

private fun Mutation.toItem(): TransactionItem = TransactionItem(
    id = id,
    title = description,
    description = detail,
    amount = CurrencyFormatter.signedRupiah(amount, type == MutationType.CREDIT),
    time = time,
    isCredit = type == MutationType.CREDIT,
    icon = category.toIcon(),
)

private fun String.toIcon(): ImageVector = when (uppercase()) {
    "TRANSFER" -> Icons.AutoMirrored.Filled.Send
    "TOPUP", "EWALLET" -> Icons.Default.KeyboardArrowDown
    "PAYMENT", "PURCHASE" -> Icons.Default.ShoppingCart
    else -> Icons.Default.Star
}
