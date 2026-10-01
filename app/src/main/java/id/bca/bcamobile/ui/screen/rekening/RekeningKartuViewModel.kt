package id.bca.bcamobile.ui.screen.rekening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.account.model.AccountBalance
import id.bca.bcamobile.domain.account.model.BankAccount
import id.bca.bcamobile.domain.card.CardRepository
import id.bca.bcamobile.domain.card.model.CardStatus
import id.bca.bcamobile.domain.card.model.PaymentCard
import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Daftar rekening beserta saldo.
 *
 * Label rekening hanya ada di `GET /account/profile`, sedangkan saldo ada di
 * `GET /account/balance` — keduanya dibaca lalu dipasangkan lewat `account_id`.
 * Saldo baru diminta saat layar dibuka, bukan di latar.
 *
 * Kartu debit datang dari `GET /account/cards` dan dimuat **setelah** keduanya:
 * kegagalannya hanya mengosongkan bagian kartu, tidak seluruh layar.
 */
@HiltViewModel
class RekeningKartuViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val cardRepository: CardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RekeningKartuUiState(isLoading = true))
    val uiState: StateFlow<RekeningKartuUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val profile = repository.profile()
            if (profile is DataResult.Failure) {
                _uiState.update {
                    it.copy(isLoading = false, error = profile.error.messageOrNull())
                }
                return@launch
            }

            val accounts = (profile as DataResult.Success).value.accounts
            when (val balance = repository.balance()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        items = accounts.map { account ->
                            account.toItem(
                                balance.value.accounts.firstOrNull { b ->
                                    b.accountId == account.id
                                },
                            )
                        },
                    )
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = balance.error.messageOrNull())
                }
            }

            // Daftar kosong dijawab 200, bukan 404 — bagian kartu sekadar tidak
            // ditampilkan, dan kegagalannya pun tidak menutupi daftar rekening.
            val cards = cardRepository.cards()
            if (cards is DataResult.Success) {
                _uiState.update { it.copy(cards = cards.value.map(PaymentCard::toItem)) }
            }
        }
    }

    /** Satu tombol mata mengatur semua kartu, seperti toggle saldo di Beranda. */
    fun onToggleBalance() = _uiState.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }

    fun onDetailToggle(accountId: String) = _uiState.update { state ->
        state.copy(
            items = state.items.map {
                if (it.accountId == accountId) {
                    it.copy(isDetailExpanded = !it.isDetailExpanded)
                } else {
                    it
                }
            },
        )
    }
}

// -- Pemetaan --------------------------------------------------------------

private const val GROUP_SIZE = 4

/** `1234567890` menjadi `1234 5678 90`, seperti di desain. */
private fun String.groupDigits(): String = chunked(GROUP_SIZE).joinToString(" ")

private fun BankAccount.toItem(balance: AccountBalance?): RekeningItem = RekeningItem(
    accountId = id,
    label = label.ifBlank { type },
    nomor = number.groupDigits(),
    saldoEfektif = CurrencyFormatter.rupiah(balance?.balance ?: 0L),
    saldoTersedia = CurrencyFormatter.rupiah(balance?.availableBalance ?: 0L),
    danaDitahan = CurrencyFormatter.rupiah(balance?.holdAmount ?: 0L),
)

private fun PaymentCard.toItem(): KartuDebitItem = KartuDebitItem(
    cardId = cardId,
    productName = productName,
    maskedNumber = maskedNumber,
    validThru = validThru,
    isBlocked = status == CardStatus.BLOCKED,
    isExpired = status == CardStatus.EXPIRED,
)
