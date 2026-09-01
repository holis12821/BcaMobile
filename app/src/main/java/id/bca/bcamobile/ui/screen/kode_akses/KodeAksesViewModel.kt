package id.bca.bcamobile.ui.screen.kode_akses

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class KodeAksesViewModel : ViewModel() {

    private val digits = mutableListOf<Int>()
    private val _uiState = MutableStateFlow(KodeAksesUiState())
    val uiState: StateFlow<KodeAksesUiState> = _uiState.asStateFlow()

    fun onDigitClick(digit: Int) {
        if (digits.size >= _uiState.value.maxDigits) return
        digits.add(digit)
        _uiState.update {
            it.copy(enteredDigits = digits.size, isError = false, errorMessage = null)
        }
    }

    fun onDeleteClick() {
        if (digits.isEmpty()) return
        digits.removeLast()
        _uiState.update {
            it.copy(enteredDigits = digits.size, isError = false, errorMessage = null)
        }
    }

    /** Returns true if the code is complete and valid. */
    fun submit(): Boolean {
        if (digits.size < _uiState.value.maxDigits) return false
        // Production: verify against stored credential hash
        return true
    }

    fun showError() {
        digits.clear()
        _uiState.update {
            it.copy(enteredDigits = 0, isError = true)
        }
    }
}