package id.bca.bcamobile.ui.screen.limit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.bca.bcamobile.R
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.account.model.TransactionLimitInput
import id.bca.bcamobile.domain.account.model.TransactionLimits
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.auth.model.PinPurpose
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.ui.screen.kode_akses.KodeAksesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AturLimitEvent {
    /** Nilai sudah siap; layar PIN dibuka karena server mewajibkan verifikasi. */
    data object PinRequired : AturLimitEvent
    data object Saved : AturLimitEvent
}

/**
 * Atur limit transaksi.
 *
 * Batas yang berlaku **tidak bisa dibaca**: kontrak hanya punya `PUT`, tanpa `GET`.
 * Karena itu layar dibuka dengan nilai kosong dan baru menampilkan angka setelah
 * server membalas perubahan. Ini dilaporkan ke backend, bukan ditutupi dengan
 * angka contoh.
 */
@HiltViewModel
class AturLimitViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AturLimitUiState(items = emptyItems()))
    val uiState: StateFlow<AturLimitUiState> = _uiState.asStateFlow()

    private val _events = Channel<AturLimitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** PIN di luar state supaya tidak ikut ter-snapshot bersama UI. */
    private val pin = StringBuilder()

    private val _pinState = MutableStateFlow(KodeAksesUiState())
    val pinState: StateFlow<KodeAksesUiState> = _pinState.asStateFlow()

    private var limits: TransactionLimits? = null

    init {
        load()
    }

    /**
     * Memuat batas yang berlaku beserta pemakaian hari ini lewat
     * `GET /account/transaction-limit`.
     *
     * Endpoint ini sudah ada sejak awal tapi tidak pernah dipanggil, jadi layar
     * menampilkan nilai kosong sampai nasabah menyimpan satu kali.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = accountRepository.transactionLimits()) {
                is DataResult.Success -> {
                    limits = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            items = result.value.toItems(),
                            isCurrentLimitUnknown = false,
                        )
                    }
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    fun onEditClick(jenis: LimitJenis) {
        _uiState.update { state ->
            state.copy(
                editing = jenis,
                error = null,
                isSaveEnabled = false,
                items = state.items.map { if (it.jenis == jenis) it.copy(input = "") else it },
            )
        }
    }

    fun onCancelEdit() {
        _uiState.update { state ->
            state.copy(
                editing = null,
                isSaveEnabled = false,
                items = state.items.map { it.copy(input = "") },
            )
        }
    }

    fun onInputChanged(jenis: LimitJenis, value: String) {
        val digits = value.filter(Char::isDigit)
        _uiState.update { state ->
            state.copy(
                error = null,
                isSaveEnabled = CurrencyFormatter.parseAmount(digits) > 0,
                items = state.items.map {
                    if (it.jenis == jenis) it.copy(input = digits) else it
                },
            )
        }
    }

    /** Langkah 1: nilai dikunci, PIN diminta. Perubahan belum dikirim di sini. */
    fun onSaveClick() {
        val state = _uiState.value
        if (state.editing == null || !state.isSaveEnabled) return
        viewModelScope.launch { _events.send(AturLimitEvent.PinRequired) }
    }

    fun onPinDigit(digit: Int) {
        if (pin.length >= PIN_LENGTH || _pinState.value.isSubmitting) return
        pin.append(digit)
        _pinState.update { it.copy(enteredDigits = pin.length, isError = false, errorMessage = null) }
        if (pin.length == PIN_LENGTH) submitPin()
    }

    fun onPinDelete() {
        if (pin.isEmpty() || _pinState.value.isSubmitting) return
        pin.deleteCharAt(pin.lastIndex)
        _pinState.update { it.copy(enteredDigits = pin.length, isError = false) }
    }

    /** Langkah 2 dan 3: verifikasi PIN lalu kirim batas baru. */
    fun submitPin() {
        val state = _uiState.value
        val jenis = state.editing ?: return
        val amount = state.items.firstOrNull { it.jenis == jenis }
            ?.input
            ?.let(CurrencyFormatter::parseAmount)
            ?: return
        if (pin.length < PIN_LENGTH || _pinState.value.isSubmitting) return

        val code = pin.toString()
        _pinState.update { it.copy(isSubmitting = true, isError = false, errorMessage = null) }

        viewModelScope.launch {
            val verification = authRepository.verifyPin(code, PinPurpose.CHANGE_LIMIT)
            clearPin()

            if (verification !is DataResult.Success) {
                val failure = verification as DataResult.Failure
                _pinState.value = KodeAksesUiState(
                    isError = true,
                    errorMessage = failure.error.messageOrNull(),
                )
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = accountRepository.updateTransactionLimit(
                limits = jenis.toInput(amount),
                verificationToken = verification.value.verificationToken,
            )

            _pinState.value = KodeAksesUiState()

            when (result) {
                is DataResult.Success -> {
                    limits = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editing = null,
                            isSaveEnabled = false,
                            isCurrentLimitUnknown = false,
                            items = result.value.toItems(),
                        )
                    }
                    _events.send(AturLimitEvent.Saved)
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.messageOrNull()
                            ?: context.getString(R.string.error_general_retry),
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

// -- Pemetaan --------------------------------------------------------------

private const val UNKNOWN_VALUE = "-"

private fun emptyItems(): List<LimitItem> = LimitJenis.entries.map {
    LimitItem(
        jenis = it,
        limitHarian = UNKNOWN_VALUE,
        terpakai = UNKNOWN_VALUE,
        sisa = UNKNOWN_VALUE,
        progress = null,
        isNearLimit = false,
    )
}

private fun LimitJenis.toInput(amount: Long): TransactionLimitInput = when (this) {
    LimitJenis.TRANSFER_INTERNAL -> TransactionLimitInput(transferInternalDaily = amount)
    LimitJenis.TRANSFER_EXTERNAL -> TransactionLimitInput(transferExternalDaily = amount)
    LimitJenis.EWALLET -> TransactionLimitInput(eWalletDaily = amount)
}

/** Ambang "hampir habis" dipakai untuk mewarnai bilah pemakaian. */
private const val NEAR_LIMIT_RATIO = 0.8f

private fun TransactionLimits.toItems(): List<LimitItem> = listOf(
    item(LimitJenis.TRANSFER_INTERNAL, transferInternalDaily, transferInternalUsedToday),
    item(LimitJenis.TRANSFER_EXTERNAL, transferExternalDaily, transferExternalUsedToday),
    item(LimitJenis.EWALLET, eWalletDaily, eWalletUsedToday),
)

private fun item(jenis: LimitJenis, daily: Long, used: Long): LimitItem {
    val ratio = if (daily > 0) (used.toFloat() / daily).coerceIn(0f, 1f) else 0f
    return LimitItem(
        jenis = jenis,
        limitHarian = CurrencyFormatter.rupiah(daily),
        terpakai = CurrencyFormatter.rupiah(used),
        sisa = CurrencyFormatter.rupiah((daily - used).coerceAtLeast(0L)),
        progress = ratio,
        isNearLimit = ratio >= NEAR_LIMIT_RATIO,
    )
}
