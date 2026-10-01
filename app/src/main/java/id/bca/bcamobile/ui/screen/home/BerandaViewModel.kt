package id.bca.bcamobile.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.format.CurrencyFormatter
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.core.push.PushEventBus
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.config.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BerandaViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val appConfigRepository: AppConfigRepository,
    private val pushEventBus: PushEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BerandaUiState(quickActions = visibleQuickActions()),
    )
    val uiState: StateFlow<BerandaUiState> = _uiState.asStateFlow()

    /** Nominal asli dipisah dari teks tampilan supaya saldo tidak bocor saat disembunyikan. */
    private var totalBalance: Long = 0L

    init {
        load()

        // Lencana belum dibaca hanya punya satu sumber yang dilayani server:
        // `unread_notifications` di `GET /account/dashboard`. Push yang masuk
        // membuat angka itu basi, jadi dashboard ditarik ulang — bukan dihitung
        // dari muatan push, yang tidak punya status baca.
        viewModelScope.launch {
            pushEventBus.arrivals.collect { load() }
        }
    }

    /**
     * Menu disaring `feature_flags` dari `GET /health` yang sudah dibaca saat splash.
     *
     * Tanpa config (mis. panggilan health gagal) semua menu tampil: menyembunyikan
     * menu karena config tidak terbaca justru menghilangkan layanan yang sebenarnya hidup.
     */
    private fun visibleQuickActions(): List<QuickAction> {
        val flags = appConfigRepository.cached()?.featureFlags ?: return QuickAction.entries
        return QuickAction.entries.filter { action ->
            when (action) {
                QuickAction.E_WALLET -> flags.eWalletEnabled
                else -> true
            }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = accountRepository.dashboard()) {
                is DataResult.Success -> {
                    totalBalance = result.value.totalBalance
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = null,
                            quickActions = visibleQuickActions(),
                            userName = result.value.displayName,
                            balance = renderBalance(state.isBalanceVisible),
                            promoItems = result.value.promotions.map {
                                PromoItem(
                                    id = it.id,
                                    title = it.title,
                                    description = "",
                                    imageUrl = it.imageUrl,
                                )
                            },
                        )
                    }
                }

                is DataResult.Failure -> _uiState.update {
                    // Layar tidak dikosongkan: menu dan promo lama tetap terlihat.
                    it.copy(isLoading = false, errorMessage = result.error.messageOrNull())
                }
            }
        }
    }

    /**
     * Saldo hanya diambil saat pengguna benar-benar membukanya. Nilai dari dashboard
     * di-cache server 1 menit, sedangkan endpoint saldo 30 detik — jadi saat dibuka
     * angkanya disegarkan.
     */
    fun onToggleBalance() {
        val willShow = !_uiState.value.isBalanceVisible
        _uiState.update { it.copy(isBalanceVisible = willShow, balance = renderBalance(willShow)) }
        if (!willShow) return

        viewModelScope.launch {
            when (val result = accountRepository.balance()) {
                is DataResult.Success -> {
                    totalBalance = result.value.totalBalance
                    _uiState.update { it.copy(balance = renderBalance(it.isBalanceVisible)) }
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(errorMessage = result.error.messageOrNull())
                }
            }
        }
    }

    private fun renderBalance(visible: Boolean): String =
        if (visible) CurrencyFormatter.rupiah(totalBalance) else HIDDEN_BALANCE

    private companion object {
        const val HIDDEN_BALANCE = "••••••••"
    }
}
