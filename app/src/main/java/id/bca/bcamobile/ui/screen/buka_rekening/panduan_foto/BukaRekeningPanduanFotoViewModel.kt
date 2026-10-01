package id.bca.bcamobile.ui.screen.buka_rekening.panduan_foto

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import javax.inject.Inject

/**
 * Tidak memanggil repository sama sekali — layarnya memang statis.
 *
 * Tetap ada supaya tiap folder layar punya bentuk yang sama, dan supaya layar ini
 * membaca state bersama lewat jalur yang sama dengan layar lain.
 */
@HiltViewModel
class BukaRekeningPanduanFotoViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: PanduanFotoEvent) {
        when (event) {
            PanduanFotoEvent.ErrorDismissed -> store.update { it.copy(error = null) }
        }
    }
}
