package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.selectedCardType
import javax.inject.Inject

@HiltViewModel
class BukaRekeningSyaratKetentuanViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: SyaratKetentuanEvent) {
        when (event) {
            SyaratKetentuanEvent.TncAccepted -> createSession()
        }
    }

    private fun createSession() {
        val current = store.current
        val index = current.selectedProductIndex ?: return
        val productType = ProductType.fromIndex(index) ?: return

        launchWithLoading {
            val result = repository.createSession(
                productType = productType,
                acceptedTncVersion = TNC_VERSION,
                cardType = current.selectedCardType(),
                cardCatalogVersion = current.cardCatalog?.catalogVersion,
            )
            when (result) {
                is DataResult.Success -> {
                    val session = result.value
                    store.update {
                        it.copy(
                            sessionId = session.sessionId,
                            product = session.product,
                            selectedCard = session.card,
                            currentStep = session.currentStep,
                            hasResumableDraft = false,
                        )
                    }
                    // Langkah berikutnya selalu dari server, bukan tebakan lokal.
                    store.send(BukaRekeningSideEffect.AdvanceTo(session.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
