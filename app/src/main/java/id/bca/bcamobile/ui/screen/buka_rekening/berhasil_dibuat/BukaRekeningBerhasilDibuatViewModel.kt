package id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.R
import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject

@HiltViewModel
class BukaRekeningBerhasilDibuatViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: BerhasilDibuatEvent) {
        when (event) {
            is BerhasilDibuatEvent.NomorRekeningDisalin ->
                emitMessage(ErrorText.Res(R.string.buka_rekening_nomor_disalin))
        }
    }
}
