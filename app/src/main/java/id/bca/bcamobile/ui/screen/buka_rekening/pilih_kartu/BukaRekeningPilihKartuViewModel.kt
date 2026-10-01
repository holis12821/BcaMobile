package id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu

import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.selectedCardType
import javax.inject.Inject

@HiltViewModel
class BukaRekeningPilihKartuViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    fun onEvent(event: PilihKartuEvent) {
        when (event) {
            PilihKartuEvent.CardCatalogRequested -> loadCardCatalog()
            is PilihKartuEvent.CardTypeSelected -> selectCardIndex(event.index)
            PilihKartuEvent.CardConfirmed -> confirmCard()
            PilihKartuEvent.ErrorDismissed -> store.update { it.copy(error = null) }
        }
    }

    /**
     * Endpoint katalog tidak butuh sesi — layar ini tampil sebelum sesi dibuat
     * (`08-PILIH-KARTU-API-SPEC.md` §2). Katalog yang sudah ada tidak ditarik
     * ulang: server menandainya cacheable 15 menit.
     */
    private fun loadCardCatalog() {
        val current = store.current
        if (current.cardCatalog != null) return
        val productType = current.selectedProductIndex?.let(ProductType::fromIndex) ?: return

        launchWithLoading {
            when (val result = repository.cardCatalog(productType)) {
                is DataResult.Success -> {
                    val catalog = result.value
                    // Default dari server, bukan "kartu pertama" — urutan bisa berubah.
                    val defaultIndex = catalog.sortedCards
                        .indexOfFirst { it.cardType == catalog.defaultCardType }
                        .takeIf { it >= 0 }
                        ?: catalog.sortedCards.indexOfFirst { it.availability.isSelectable }
                            .takeIf { it >= 0 }
                        ?: 0
                    store.update {
                        it.copy(cardCatalog = catalog, selectedCardIndex = defaultIndex)
                    }
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    /** Kartu habis stok atau nonaktif tidak boleh terpilih — server akan menolaknya nanti. */
    private fun selectCardIndex(index: Int) {
        val card = store.current.cardCatalog?.sortedCards?.getOrNull(index)
        if (card != null && !card.availability.isSelectable) return
        store.update { it.copy(selectedCardIndex = index, error = null) }
    }

    /**
     * Sesi biasanya belum ada di titik ini, jadi kartunya ikut terkirim nanti saat
     * sesi dibuat di layar S&K. Kalau sesi sudah ada — nasabah menekan Back dari
     * S&K atau melanjutkan draf — kartunya dikirim lewat `PUT sessions/{id}/card`.
     */
    private fun confirmCard() {
        val current = store.current
        val cardType = current.selectedCardType()

        if (current.sessionId == null || cardType == null) {
            advanceTo(OnboardingStep.TNC)
            return
        }

        launchWithLoading {
            val result = repository.selectCard(
                cardType = cardType,
                cardCatalogVersion = current.cardCatalog?.catalogVersion,
            )
            when (result) {
                is DataResult.Success -> {
                    store.update { it.copy(selectedCard = result.value) }
                    // Sesi sudah ada berarti S&K sudah lewat; posisi berikutnya
                    // ditanyakan ke server, bukan ditebak.
                    refreshStep()
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }
}
