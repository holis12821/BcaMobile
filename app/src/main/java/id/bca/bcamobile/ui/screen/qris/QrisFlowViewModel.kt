package id.bca.bcamobile.ui.screen.qris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.qris.QrisRepository
import id.bca.bcamobile.domain.qris.model.QrisPayload
import id.bca.bcamobile.domain.qris.model.QrisReceipt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State bersama empat layar QRIS. Hidup selama graph QRIS ada di back stack. */
data class QrisFlowState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val isFlashOn: Boolean = false,
    val isDecoding: Boolean = false,

    val payload: QrisPayload? = null,
    val amountInput: String = "",

    val sourceAccountId: String = "",
    val sourceAccountName: String = "",
    val sourceAccountNumber: String = "",
    val sourceBalance: Long = 0L,

    val receipt: QrisReceipt? = null,

    val pinDigits: Int = 0,
    val isPinError: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    /** QR dinamis mengunci nominal; QR statis mengharuskan nasabah mengisinya. */
    val isAmountEditable: Boolean get() = payload?.isAmountFixed == false

    val amount: Long
        get() = if (isAmountEditable) {
            CurrencyFormatter.parseAmount(amountInput)
        } else {
            payload?.amount ?: 0L
        }

    /** Biaya admin QRIS hanya diketahui saat pembayaran; ringkasan memakai nol. */
    val total: Long get() = amount

    val hasEnoughBalance: Boolean get() = total <= sourceBalance

    val isPayEnabled: Boolean
        get() = payload != null &&
            amount > 0 &&
            hasEnoughBalance &&
            sourceAccountId.isNotBlank() &&
            !isSubmitting
}

sealed interface QrisFlowEvent {
    /** QR sudah diterjemahkan server; lanjut ke konfirmasi. */
    data object PayloadReady : QrisFlowEvent
    data object PaymentSucceeded : QrisFlowEvent
}

/**
 * Alur QRIS: decode → verifikasi PIN → bayar.
 *
 * Idempotency dipegang repository dan terikat `qris_id`, jadi retry karena timeout
 * tidak membayar dua kali.
 */
@HiltViewModel
class QrisFlowViewModel @Inject constructor(
    private val qrisRepository: QrisRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QrisFlowState())
    val state: StateFlow<QrisFlowState> = _state.asStateFlow()

    private val _events = Channel<QrisFlowEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** PIN disimpan di luar state supaya tidak ikut ter-snapshot bersama UI. */
    private val pin = StringBuilder()

    init {
        loadSourceAccount()
    }

    private fun loadSourceAccount() {
        viewModelScope.launch {
            val profile = accountRepository.profile()
            if (profile !is DataResult.Success) return@launch
            val primary = profile.value.accounts.firstOrNull { it.isPrimary }
                ?: profile.value.accounts.firstOrNull()
                ?: return@launch

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
    }

    fun onFlashToggle() = _state.update { it.copy(isFlashOn = !it.isFlashOn) }

    /**
     * QR terbaca — teks mentahnya dikirim ke server tanpa diurai di client.
     *
     * Pemindaian berikutnya ditahan selama decode berjalan supaya satu QR tidak
     * dikirim dua kali saat kamera masih menyorotnya.
     */
    fun onQrScanned(rawValue: String) {
        if (_state.value.isDecoding || _state.value.payload != null) return
        _state.update { it.copy(isDecoding = true, error = null) }

        viewModelScope.launch {
            when (val result = qrisRepository.decode(rawValue)) {
                is DataResult.Success -> {
                    _state.update { it.copy(isDecoding = false, payload = result.value) }
                    _events.send(QrisFlowEvent.PayloadReady)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isDecoding = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    /** Dipakai saat nasabah kembali ke pemindai: QR lama tidak boleh menempel. */
    fun onScanResumed() = _state.update {
        it.copy(payload = null, amountInput = "", error = null, isDecoding = false)
    }

    fun onAmountChanged(value: String) = _state.update {
        it.copy(amountInput = value.filter(Char::isDigit), error = null)
    }

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
        val payload = current.payload ?: return
        if (pin.length < PIN_LENGTH || current.isSubmitting) return

        val code = pin.toString()
        _state.update { it.copy(isSubmitting = true, isPinError = false, error = null) }

        viewModelScope.launch {
            val verification = authRepository.verifyPin(code, PinPurpose.QRIS_PAYMENT)
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

            val payment = qrisRepository.pay(
                qrisId = payload.qrisId,
                sourceAccountId = current.sourceAccountId,
                amount = current.amount,
                verificationToken = verification.value.verificationToken,
            )

            when (payment) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, pinDigits = 0, receipt = payment.value)
                    }
                    _events.send(QrisFlowEvent.PaymentSucceeded)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        pinDigits = 0,
                        error = payment.error.messageOrNull(),
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
