package id.bca.bcamobile.ui.screen.ewallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.ewallet.EWalletRepository
import id.bca.bcamobile.domain.ewallet.model.EWalletInquiry
import id.bca.bcamobile.domain.ewallet.model.EWalletProvider
import id.bca.bcamobile.domain.ewallet.model.EWalletReceipt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State bersama empat layar top up. Hidup selama graph e-wallet ada di back stack. */
data class EWalletFlowState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val providers: List<EWalletProvider> = emptyList(),
    val selectedProviderId: String? = null,
    val phoneNumber: String = "",
    val selectedPresetIndex: Int = -1,
    val manualAmount: String = "",

    val sourceAccountId: String = "",
    val sourceAccountName: String = "",
    val sourceAccountNumber: String = "",
    val sourceBalance: Long = 0L,

    val inquiry: EWalletInquiry? = null,
    val receipt: EWalletReceipt? = null,

    val pinDigits: Int = 0,
    val isPinError: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    val selectedProvider: EWalletProvider?
        get() = providers.firstOrNull { it.id == selectedProviderId }

    /** Nominal aktif: preset yang dipilih, atau ketikan manual. */
    val amount: Long
        get() = selectedProvider?.presetAmounts?.getOrNull(selectedPresetIndex)
            ?: CurrencyFormatter.parseAmount(manualAmount)

    val isAmountWithinLimit: Boolean
        get() = selectedProvider?.let { amount in it.minAmount..it.maxAmount } ?: false

    val hasEnoughBalance: Boolean
        get() = amount + (selectedProvider?.adminFee ?: 0L) <= sourceBalance

    val isFormValid: Boolean
        get() = selectedProviderId != null &&
            phoneNumber.length >= MIN_PHONE_LENGTH &&
            isAmountWithinLimit &&
            hasEnoughBalance

    private companion object {
        const val MIN_PHONE_LENGTH = 9
    }
}

sealed interface EWalletFlowEvent {
    data object InquiryReady : EWalletFlowEvent
    data object TopUpSucceeded : EWalletFlowEvent
}

@HiltViewModel
class EWalletFlowViewModel @Inject constructor(
    private val eWalletRepository: EWalletRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EWalletFlowState())
    val state: StateFlow<EWalletFlowState> = _state.asStateFlow()

    private val _events = Channel<EWalletFlowEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** PIN disimpan di luar state supaya tidak ikut ter-snapshot bersama UI. */
    private val pin = StringBuilder()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            loadSourceAccount()
            when (val result = eWalletRepository.providers()) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        providers = result.value.filter { provider -> provider.isActive },
                    )
                }

                is DataResult.Failure -> _state.update {
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
                sourceAccountName = primary.label,
                sourceAccountNumber = primary.number,
            )
        }

        val balance = accountRepository.balance()
        if (balance is DataResult.Success) {
            val match = balance.value.accounts.firstOrNull { it.accountId == primary.id }
            _state.update { it.copy(sourceBalance = match?.balance ?: 0L) }
        }
    }

    fun onProviderSelected(id: String) = _state.update {
        it.copy(selectedProviderId = id, selectedPresetIndex = -1, manualAmount = "")
    }

    fun onPhoneNumberChanged(value: String) = _state.update {
        it.copy(phoneNumber = value.filter(Char::isDigit))
    }

    fun onPresetSelected(index: Int) = _state.update {
        it.copy(selectedPresetIndex = index, manualAmount = "")
    }

    fun onManualAmountChanged(value: String) = _state.update {
        it.copy(manualAmount = value, selectedPresetIndex = -1)
    }

    /** Langkah 1 dari tiga: hasil inquiry berumur pendek, jadi jangan dipanggil di awal layar. */
    fun onContinue() {
        val current = _state.value
        if (!current.isFormValid || current.isSubmitting) return

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = eWalletRepository.inquiry(
                providerId = current.selectedProviderId.orEmpty(),
                phoneNumber = current.phoneNumber,
                amount = current.amount,
                sourceAccountId = current.sourceAccountId,
            )
            when (result) {
                is DataResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, inquiry = result.value) }
                    _events.send(EWalletFlowEvent.InquiryReady)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    // -- Langkah 2 dan 3: verifikasi PIN lalu eksekusi --------------------------

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
        val inquiry = _state.value.inquiry ?: return
        if (pin.length < PIN_LENGTH || _state.value.isSubmitting) return

        val code = pin.toString()
        _state.update { it.copy(isSubmitting = true, isPinError = false, error = null) }

        viewModelScope.launch {
            val verification = authRepository.verifyPin(code, PinPurpose.EWALLET_TOPUP)
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

            when (
                val topUp = eWalletRepository.topUp(
                    inquiryId = inquiry.inquiryId,
                    verificationToken = verification.value.verificationToken,
                )
            ) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, pinDigits = 0, receipt = topUp.value)
                    }
                    _events.send(EWalletFlowEvent.TopUpSucceeded)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        pinDigits = 0,
                        error = topUp.error.messageOrNull(),
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
    }
}
