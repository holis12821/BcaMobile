package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.toPersonalData
import javax.inject.Inject

@HiltViewModel
class BukaRekeningDataPribadiViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: DataPribadiEvent) {
        when (event) {
            is DataPribadiEvent.GenderSelected ->
                store.update { it.copy(jenisKelamin = event.value) }

            is DataPribadiEvent.DomicileSameToggled ->
                store.update { it.copy(alamatDomisiliSama = event.same) }

            DataPribadiEvent.PersonalDataSubmitted -> savePersonalData()

            DataPribadiEvent.DraftSaveRequested ->
                emitMessage(ErrorText.Res(R.string.buka_rekening_draft_saved))
        }
    }

    private fun savePersonalData() {
        val current = store.current
        val ocr = current.ocr
        if (ocr == null) {
            store.update { it.copy(error = ErrorText.Res(R.string.buka_rekening_error_no_ocr)) }
            return
        }

        val data = ocr.toPersonalData(
            jenisKelamin = current.jenisKelamin,
            alamatDomisiliSama = current.alamatDomisiliSama,
        )

        launchWithLoading {
            when (val result = repository.savePersonalData(ocr.ocrId, data)) {
                is DataResult.Success -> {
                    val challenge = result.value
                    // OTP pembuka step ini menolkan penghitung gagal dan kuota
                    // kirim ulang di server, jadi blokir lokal ikut dilepas.
                    // Jendela hitung mundurnya dimulai di ViewModel layar OTP.
                    store.update {
                        it.copy(
                            otpSentTo = challenge.otpSentTo,
                            otpExpiresAt = challenge.otpExpiresAt,
                            otpCode = "",
                            isOtpInputBlocked = false,
                            isOtpResendBlocked = false,
                        )
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(challenge.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
