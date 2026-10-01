package id.bca.bcamobile.ui.screen.buka_rekening.ringkasan

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject

@HiltViewModel
class BukaRekeningRingkasanViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: RingkasanEvent) {
        when (event) {
            is RingkasanEvent.AgreementToggled ->
                store.update { it.copy(isAgreed = event.agreed, error = null) }

            RingkasanEvent.ApplicationSubmitted -> submitApplication()

            RingkasanEvent.DraftSaveRequested ->
                emitMessage(ErrorText.Res(R.string.buka_rekening_draft_saved))
        }
    }

    private fun submitApplication() {
        if (!store.current.isAgreed) {
            store.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_agreement_required))
            }
            return
        }

        launchWithLoading {
            val result = repository.submitApplication(
                agreementVersion = AGREEMENT_VERSION,
                // Kunci hidup di store, bukan di ViewModel ini: nasabah bisa
                // meninggalkan layar lalu kembali, dan kuncinya harus tetap sama.
                idempotencyKey = store.idempotencyKey(),
            )
            when (result) {
                is DataResult.Success -> {
                    store.update { it.copy(account = result.value) }
                    repository.clearLocalSession()
                    store.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.COMPLETED))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
