package id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject

@HiltViewModel
class BukaRekeningAntreanVideoCallViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: AntreanVideoCallEvent) {
        when (event) {
            AntreanVideoCallEvent.QueueJoinRequested -> joinQueue()
        }
    }

    private fun joinQueue() {
        launchWithLoading {
            when (val result = repository.joinVideoCallQueue()) {
                is DataResult.Success -> store.update { it.copy(queue = result.value) }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
