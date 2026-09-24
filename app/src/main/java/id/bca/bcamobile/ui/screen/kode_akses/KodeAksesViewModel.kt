package id.bca.bcamobile.ui.screen.kode_akses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Kejadian sekali pakai; navigasi tidak boleh disimpan di state. */
sealed interface KodeAksesEvent {
    /** [displayName] datang dari server, bukan tebakan client. */
    data class LoginSucceeded(val displayName: String) : KodeAksesEvent
}

@HiltViewModel
class KodeAksesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Buffer digit disimpan terpisah dari state supaya kode akses tidak ikut terekspos. */
    private val digits = StringBuilder()

    private val _uiState = MutableStateFlow(KodeAksesUiState())
    val uiState: StateFlow<KodeAksesUiState> = _uiState.asStateFlow()

    private val _events = Channel<KodeAksesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onDigitClick(digit: Int) {
        if (digits.length >= _uiState.value.maxDigits || _uiState.value.isSubmitting) return
        digits.append(digit)
        _uiState.update {
            it.copy(enteredDigits = digits.length, isError = false, errorMessage = null)
        }
        if (digits.length == _uiState.value.maxDigits) submit()
    }

    fun onDeleteClick() {
        if (digits.isEmpty() || _uiState.value.isSubmitting) return
        digits.deleteCharAt(digits.lastIndex)
        _uiState.update {
            it.copy(enteredDigits = digits.length, isError = false, errorMessage = null)
        }
    }

    fun submit() {
        val code = digits.toString()
        if (code.length < _uiState.value.maxDigits || _uiState.value.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, isError = false, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.loginWithAccessCode(code)) {
                is DataResult.Success -> {
                    clearBuffer()
                    _uiState.update { it.copy(isSubmitting = false, enteredDigits = 0) }
                    _events.send(KodeAksesEvent.LoginSucceeded(result.value.displayName))
                }

                is DataResult.Failure -> {
                    clearBuffer()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            enteredDigits = 0,
                            isError = true,
                            errorMessage = result.error.toMessage(),
                        )
                    }
                }
            }
        }
    }

    /** Kode akses dibuang dari memory segera setelah dipakai, sukses maupun gagal. */
    private fun clearBuffer() {
        digits.setLength(0)
    }

    override fun onCleared() {
        clearBuffer()
        super.onCleared()
    }
}

/**
 * Pesan bisnis dari server dipakai apa adanya karena sudah berbahasa Indonesia.
 * Sisanya dibiarkan null supaya layar memakai teks bawaannya sendiri.
 */
private fun ApiFailure.toMessage(): String? = when (this) {
    is ApiFailure.Business -> message.takeIf { it.isNotBlank() }
    else -> null
}
