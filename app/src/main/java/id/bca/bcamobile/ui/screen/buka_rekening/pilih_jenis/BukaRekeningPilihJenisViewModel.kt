package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Layar pertama flow, jadi ViewModel inilah yang mengurus hidup-matinya sesi:
 * menandai adanya draf yang bisa dilanjutkan, melanjutkannya, dan membatalkannya.
 */
@HiltViewModel
class BukaRekeningPilihJenisViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    init {
        // Draf dari sesi sebelumnya (mis. proses sempat mati) ditandai supaya
        // UI bisa menawarkan "Lanjutkan pendaftaran?".
        store.update { it.copy(hasResumableDraft = repository.savedSessionId() != null) }
    }

    fun onEvent(event: PilihJenisEvent) {
        when (event) {
            is PilihJenisEvent.ProductSelected ->
                store.update { it.copy(selectedProductIndex = event.index, error = null) }

            PilihJenisEvent.DraftResumeRequested -> resumeDraft()
            PilihJenisEvent.FlowAbandoned -> abandonFlow()
            PilihJenisEvent.ErrorDismissed -> store.update { it.copy(error = null) }
        }
    }

    private fun resumeDraft() {
        val sessionId = repository.savedSessionId() ?: return
        launchWithLoading {
            when (val result = repository.getSession(sessionId)) {
                is DataResult.Success -> {
                    val session = result.value
                    store.update {
                        it.copy(
                            sessionId = session.sessionId,
                            product = session.product,
                            selectedCard = session.card,
                            currentStep = session.currentStep,
                            isOtpVerified = session.stepsCompleted.otpVerified,
                            isVideoCallVerified = session.stepsCompleted.videoCallVerified,
                            isCredentialsSaved = session.stepsCompleted.credentialsSet,
                            hasResumableDraft = false,
                        )
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(session.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun abandonFlow() {
        viewModelScope.launch {
            repository.cancelSession()
            store.clear()
            store.send(BukaRekeningSideEffect.ExitFlow)
        }
    }
}
