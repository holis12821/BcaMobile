package id.bca.bcamobile.ui.screen.buka_rekening.video_call

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BukaRekeningVideoCallViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: VideoCallEvent) {
        when (event) {
            is VideoCallEvent.VideoCallCompleted -> completeVideoCall(event.csName)
        }
    }

    private fun completeVideoCall(csName: String) {
        store.update { it.copy(isVideoCallVerified = true, videoCallCsName = csName) }
        if (store.current.sessionId == null) return
        launchWithLoading { refreshStep() }
    }
}
