package id.bca.bcamobile.ui.screen.bantuan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.content.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Kontak CS dari `GET /content/contact-cs`.
 *
 * Semua nilainya sudah berbentuk teks siap tampil — server yang memformat,
 * supaya mengubah nomor tidak butuh rilis aplikasi.
 */
@HiltViewModel
class HubungiCsViewModel @Inject constructor(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HubungiCsUiState(isLoading = true))
    val uiState: StateFlow<HubungiCsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.contactCs()) {
                is DataResult.Success -> _uiState.update {
                    val contact = result.value
                    it.copy(
                        isLoading = false,
                        error = null,
                        phone = contact.phone,
                        phoneFree = contact.phoneFree,
                        whatsapp = contact.whatsapp,
                        email = contact.email,
                        chatUrl = contact.chatUrl,
                        hours = contact.hours,
                    )
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }
}
