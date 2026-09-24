package id.bca.bcamobile.ui.screen.akun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.BuildConfig
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.account.AccountRepository
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

sealed interface AkunEvent {
    data object LoggedOut : AkunEvent
}

@HiltViewModel
class AkunViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AkunUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<AkunUiState> = _uiState.asStateFlow()

    private val _events = Channel<AkunEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = accountRepository.profile()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        userName = result.value.displayName,
                        phoneNumber = result.value.phoneNumber,
                        isBiometricEnabled = result.value.isBiometricEnabled,
                    )
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    /** Saklar dipindah dulu supaya terasa responsif, lalu dikembalikan bila server menolak. */
    fun onBiometricToggle(enabled: Boolean) {
        val previous = _uiState.value.isBiometricEnabled
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
        viewModelScope.launch {
            val result = accountRepository.updateBiometricSetting(enabled)
            if (result is DataResult.Failure) {
                _uiState.update {
                    it.copy(isBiometricEnabled = previous, error = result.error.messageOrNull())
                }
            }
        }
    }

    /** Token lokal dibersihkan repository, termasuk saat server tidak terjangkau. */
    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.send(AkunEvent.LoggedOut)
        }
    }
}
