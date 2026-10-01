package id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.isCredentialValid
import javax.inject.Inject

@HiltViewModel
class BukaRekeningBuatKredensialViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: BuatKredensialEvent) {
        when (event) {
            is BuatKredensialEvent.AccessCodeChanged ->
                store.update { it.copy(accessCode = event.value, error = null) }

            is BuatKredensialEvent.ConfirmAccessCodeChanged ->
                store.update { it.copy(confirmAccessCode = event.value, error = null) }

            BuatKredensialEvent.AccessCodeVisibilityToggled ->
                store.update { it.copy(isAccessCodeVisible = !it.isAccessCodeVisible) }

            BuatKredensialEvent.ConfirmAccessCodeVisibilityToggled -> store.update {
                it.copy(isConfirmAccessCodeVisible = !it.isConfirmAccessCodeVisible)
            }

            is BuatKredensialEvent.PinChanged ->
                store.update { it.copy(pin = event.value, error = null) }

            is BuatKredensialEvent.ConfirmPinChanged ->
                store.update { it.copy(confirmPin = event.value, error = null) }

            BuatKredensialEvent.CredentialsSubmitted -> saveCredentials()
        }
    }

    private fun saveCredentials() {
        val current = store.current
        if (!current.isCredentialValid()) {
            store.update {
                it.copy(error = ErrorText.Res(R.string.buka_rekening_error_credential_invalid))
            }
            return
        }

        launchWithLoading {
            when (val result = repository.saveCredentials(current.accessCode, current.pin)) {
                is DataResult.Success -> {
                    // Kredensial tidak disimpan di state setelah terkirim.
                    store.update {
                        it.copy(
                            isCredentialsSaved = true,
                            accessCode = "",
                            confirmAccessCode = "",
                            pin = "",
                            confirmPin = "",
                        )
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(OnboardingStep.REVIEW))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
