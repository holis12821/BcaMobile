package id.bca.bcamobile.ui.screen.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transfer.TransferRepository
import id.bca.bcamobile.domain.transfer.model.RecentTransfer
import id.bca.bcamobile.domain.transfer.model.TransferInquiry
import id.bca.bcamobile.domain.transfer.model.TransferReceipt
import id.bca.bcamobile.domain.transfer.model.TransferType as DomainTransferType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferFlowState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,

    val sourceAccountId: String = "",
    val sourceAccountLabel: String = "",
    val sourceAccountNumber: String = "",
    val sourceBalance: Long = 0L,
    val isBalanceVisible: Boolean = true,

    val recents: List<RecentTransfer> = emptyList(),
    val searchQuery: String = "",

    val destinationAccount: String = "",
    val amountInput: String = "",
    val notes: String = "",

    val inquiry: TransferInquiry? = null,
    val receipt: TransferReceipt? = null,

    val pinDigits: Int = 0,
    val isPinError: Boolean = false,
) {
    val amount: Long get() = CurrencyFormatter.parseAmount(amountInput)

    val isFormValid: Boolean
        get() = destinationAccount.length >= MIN_ACCOUNT_LENGTH &&
            amount > 0 &&
            amount <= sourceBalance &&
            !isSubmitting

    private companion object {
        const val MIN_ACCOUNT_LENGTH = 6
    }
}

sealed interface TransferFlowEvent {
    data object InquiryReady : TransferFlowEvent
    data object TransferSucceeded : TransferFlowEvent
}

/**
 * Satu ViewModel untuk seluruh alur transfer, mengikuti pola tiga langkah
 * `inquiry -> verifikasi PIN -> execute` di `01-API-SPECIFICATION.md` §5.
 */
@HiltViewModel
class TransferFlowViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferFlowState())
    val state: StateFlow<TransferFlowState> = _state.asStateFlow()

    private val _events = Channel<TransferFlowEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val pin = StringBuilder()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            loadSourceAccount()
            when (val result = transferRepository.recent()) {
                is DataResult.Success ->
                    _state.update { it.copy(isLoading = false, recents = result.value) }

                is DataResult.Failure -> _state.update {
                    // Daftar transfer terakhir bukan penghalang: form tetap bisa dipakai.
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    private suspend fun loadSourceAccount() {
        val profile = accountRepository.profile()
        if (profile !is DataResult.Success) return
        val primary = profile.value.accounts.firstOrNull { it.isPrimary }
            ?: profile.value.accounts.firstOrNull()
            ?: return

        _state.update {
            it.copy(
                sourceAccountId = primary.id,
                sourceAccountLabel = primary.label,
                sourceAccountNumber = primary.number,
            )
        }

        val balance = accountRepository.balance()
        if (balance is DataResult.Success) {
            val match = balance.value.accounts.firstOrNull { it.accountId == primary.id }
            _state.update { it.copy(sourceBalance = match?.balance ?: 0L) }
        }
    }

    fun onToggleBalance() = _state.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }

    fun onSearchChange(value: String) = _state.update { it.copy(searchQuery = value) }

    fun onDestinationChange(value: String) = _state.update {
        it.copy(destinationAccount = value.filter(Char::isDigit))
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amountInput = value) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /** Memilih tujuan dari daftar terakhir mengisi form, tidak langsung mengirim. */
    fun onRecentSelected(accountNumber: String) = _state.update {
        it.copy(destinationAccount = accountNumber)
    }

    fun onContinue() {
        val current = _state.value
        if (!current.isFormValid) return

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = transferRepository.inquiry(
                destinationAccount = current.destinationAccount,
                destinationBank = DEFAULT_BANK,
                bankCode = DEFAULT_BANK_CODE,
                type = DomainTransferType.INTERNAL,
                amount = current.amount,
                notes = current.notes.takeIf { it.isNotBlank() },
            )
            when (result) {
                is DataResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, inquiry = result.value) }
                    _events.send(TransferFlowEvent.InquiryReady)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    // -- PIN dan eksekusi -------------------------------------------------------

    fun onPinDigit(digit: Int) {
        if (pin.length >= PIN_LENGTH || _state.value.isSubmitting) return
        pin.append(digit)
        _state.update { it.copy(pinDigits = pin.length, isPinError = false, error = null) }
        if (pin.length == PIN_LENGTH) submitPin()
    }

    fun onPinDelete() {
        if (pin.isEmpty() || _state.value.isSubmitting) return
        pin.deleteCharAt(pin.lastIndex)
        _state.update { it.copy(pinDigits = pin.length, isPinError = false) }
    }

    fun submitPin() {
        val current = _state.value
        val inquiry = current.inquiry ?: return
        if (pin.length < PIN_LENGTH || current.isSubmitting) return

        val code = pin.toString()
        _state.update { it.copy(isSubmitting = true, isPinError = false, error = null) }

        viewModelScope.launch {
            val verification = authRepository.verifyPin(code, PinPurpose.TRANSFER)
            clearPin()

            if (verification !is DataResult.Success) {
                val failure = verification as DataResult.Failure
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        pinDigits = 0,
                        isPinError = true,
                        error = failure.error.messageOrNull(),
                    )
                }
                return@launch
            }

            val execution = transferRepository.execute(
                inquiryId = inquiry.inquiryId,
                sourceAccountId = current.sourceAccountId,
                amount = current.amount,
                notes = current.notes.takeIf { it.isNotBlank() },
                verificationToken = verification.value.verificationToken,
            )

            when (execution) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, pinDigits = 0, receipt = execution.value)
                    }
                    _events.send(TransferFlowEvent.TransferSucceeded)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        pinDigits = 0,
                        error = execution.error.messageOrNull(),
                    )
                }
            }
        }
    }

    private fun clearPin() {
        pin.setLength(0)
    }

    override fun onCleared() {
        clearPin()
        super.onCleared()
    }

    private companion object {
        const val PIN_LENGTH = 6

        /**
         * Layar Transfer Antar Rekening hanya melayani sesama BCA; pilihan bank
         * belum ada di desain. Transfer antar bank butuh pemilih bank lebih dulu.
         */
        const val DEFAULT_BANK = "BCA"
        const val DEFAULT_BANK_CODE = "014"
    }
}
