package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.camera.CameraCapture
import id.bca.bcamobile.core.liveness.LivenessProgress
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject

@HiltViewModel
class BukaRekeningVerifikasiBiometrikViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: VerifikasiBiometrikEvent) {
        when (event) {
            VerifikasiBiometrikEvent.LivenessStarted -> store.update {
                it.copy(isLivenessRunning = true, liveness = LivenessProgress())
            }

            is VerifikasiBiometrikEvent.LivenessProgressed ->
                store.update { it.copy(liveness = event.progress) }

            is VerifikasiBiometrikEvent.BiometricCaptured -> uploadBiometric(event)
        }
    }

    private fun uploadBiometric(event: VerifikasiBiometrikEvent.BiometricCaptured) {
        launchWithLoading {
            val result = repository.uploadBiometric(
                facePhoto = event.facePhoto,
                livenessFrames = event.livenessFrames,
                meta = event.meta,
            )

            // Wajah dan frame bukti tidak boleh menetap di perangkat, apa pun
            // hasil unggahannya.
            CameraCapture.discard(event.facePhoto)
            event.livenessFrames.forEach(CameraCapture::discard)

            when (result) {
                is DataResult.Success -> {
                    store.update { it.copy(biometric = result.value, isLivenessRunning = false) }
                    store.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.VIDEO_CALL))
                }
                is DataResult.Failure -> {
                    store.update { it.copy(isLivenessRunning = false) }
                    handleFailure(result.error)
                }
            }
        }
    }
}
