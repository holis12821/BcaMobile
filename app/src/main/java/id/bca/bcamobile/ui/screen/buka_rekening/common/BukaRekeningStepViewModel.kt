package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Induk seluruh ViewModel langkah buka rekening.
 *
 * Tiap layar punya ViewModel-nya sendiri, tapi keempat belasnya memakai
 * infrastruktur yang sama: state bersama dari [BukaRekeningSessionStore],
 * penanda loading, dan penanganan kegagalan yang seragam. Disatukan di sini
 * supaya tidak ada empat belas salinan yang perlahan berbeda perilaku.
 *
 * ViewModel turunan hanya menulis logika langkahnya sendiri.
 */
abstract class BukaRekeningStepViewModel(
    protected val repository: OnboardingRepository,
    protected val store: BukaRekeningSessionStore,
) : ViewModel() {

    /**
     * State bersama flow. Tiap layar menurunkan `UiState` tampilannya dari sini
     * lewat mapper `@Composable` di foldernya masing-masing — konversi itu butuh
     * `stringResource`, dan ViewModel tidak boleh menyentuh `Context`.
     */
    val state: StateFlow<BukaRekeningFlowState> = store.state

    /** Navigasi dan pesan sekali pakai; sumbernya satu untuk seluruh flow. */
    val sideEffect: Flow<BukaRekeningSideEffect> = store.sideEffect

    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            store.update { it.copy(isLoading = true, error = null) }
            try {
                block()
            } finally {
                // Tanpa finally, satu exception di block() mengunci layar di
                // keadaan loading selamanya.
                store.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Kegagalan yang berlaku sama di semua langkah.
     *
     * Sesi yang hangus membersihkan seluruh state dan melempar nasabah ke awal
     * flow; step yang tidak sinkron ditanyakan ulang ke server, bukan ditebak.
     */
    protected suspend fun handleFailure(error: ApiFailure) {
        if (error.isFatalForSession) {
            repository.clearLocalSession()
            store.clear()
            store.update { it.copy(error = error.toErrorText()) }
            store.send(BukaRekeningSideEffect.RestartFlow)
            return
        }

        store.update { it.copy(error = error.toErrorText()) }

        if (error is ApiFailure.InvalidStep) {
            refreshStep()
        }
    }

    /** Menanyakan posisi sebenarnya ke server lalu mengikutinya. */
    protected suspend fun refreshStep() {
        val sessionId = store.current.sessionId ?: return
        when (val result = repository.getSession(sessionId)) {
            is DataResult.Success -> {
                store.update { it.copy(currentStep = result.value.currentStep) }
                store.send(BukaRekeningSideEffect.AdvanceTo(result.value.currentStep))
            }
            is DataResult.Failure -> store.update { it.copy(error = result.error.toErrorText()) }
        }
    }

    protected fun emitMessage(text: ErrorText) {
        viewModelScope.launch { store.send(BukaRekeningSideEffect.ShowMessage(text)) }
    }

    protected fun advanceTo(step: id.bca.bcamobile.domain.onboarding.model.OnboardingStep) {
        viewModelScope.launch { store.send(BukaRekeningSideEffect.AdvanceTo(step)) }
    }

    protected companion object {
        /**
         * Versi dokumen persetujuan data pribadi di layar Ringkasan — **bukan** versi S&K.
         *
         * Pasangannya `TNC_VERSION` sudah dihapus: versi S&K sekarang datang dari
         * `GET /onboarding/tnc` dan harus berasal dari dokumen yang benar-benar terpampang
         * saat nasabah menyetujuinya. Sebagai konstanta, nilainya membeku saat server
         * pindah versi, dan server menjawab `409 TNC_VERSION_OUTDATED`.
         */
        const val AGREEMENT_VERSION = "2026-09-01"
    }
}
