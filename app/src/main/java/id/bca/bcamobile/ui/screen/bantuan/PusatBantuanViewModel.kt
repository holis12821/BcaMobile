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
 * Pusat Bantuan dari `GET /content/help-center`.
 *
 * Endpoint-nya publik dan di-cache server 24 jam, jadi tidak ada cache
 * tambahan di sini.
 */
@HiltViewModel
class PusatBantuanViewModel @Inject constructor(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PusatBantuanUiState(isLoading = true))
    val uiState: StateFlow<PusatBantuanUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.helpCenter()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        categories = result.value.map { category ->
                            BantuanKategori(
                                key = category.key,
                                // `title` dikirim server justru supaya key baru
                                // yang belum dikenal build ini tetap punya teks
                                // untuk dicetak — jangan mencetak key mentah.
                                title = category.title.ifBlank { category.key },
                                items = category.items.map { item ->
                                    BantuanItem(item.question, item.answer)
                                },
                            )
                        },
                    )
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }

    fun onItemToggle(question: String) = _uiState.update { state ->
        state.copy(
            expandedQuestion = if (state.expandedQuestion == question) null else question,
        )
    }
}
