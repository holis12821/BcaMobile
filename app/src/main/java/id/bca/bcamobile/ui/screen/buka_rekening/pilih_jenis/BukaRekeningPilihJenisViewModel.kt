package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.SavingsProductCatalog
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.isProductCatalogFresh
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Layar pertama flow, jadi ViewModel inilah yang mengurus hidup-matinya sesi:
 * menandai adanya draf yang bisa dilanjutkan, melanjutkannya, dan membatalkannya.
 *
 * Ia juga memuat katalog jenis rekening — satu-satunya pembacaan di flow ini yang
 * kegagalannya **tidak** menghentikan apa pun; lihat [loadCatalog].
 */
@HiltViewModel
class BukaRekeningPilihJenisViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    init {
        // Draf dari sesi sebelumnya (mis. proses sempat mati) ditandai supaya
        // UI bisa menawarkan "Lanjutkan pendaftaran?".
        store.update { it.copy(hasResumableDraft = repository.savedSessionId() != null) }
    }

    fun onEvent(event: PilihJenisEvent) {
        when (event) {
            PilihJenisEvent.ProductCatalogRequested -> loadCatalog()
            PilihJenisEvent.ProductCatalogReloadRequested -> reloadCatalog()

            is PilihJenisEvent.ProductSelected ->
                store.update { it.copy(selectedProductType = event.type, error = null) }

            PilihJenisEvent.DraftResumeRequested -> resumeDraft()
            PilihJenisEvent.FlowAbandoned -> abandonFlow()
            PilihJenisEvent.ErrorDismissed -> store.update { it.copy(error = null) }
        }
    }

    /**
     * Katalog yang gagal dimuat **tidak** ditulis ke `error`, dan itu disengaja.
     *
     * Layar punya daftar bawaan dari `strings.xml`, dan `POST /sessions` tidak menuntut
     * katalog ini ada — jadi menampilkan layar error di langkah pertama hanya menghentikan
     * pendaftaran yang sebenarnya masih bisa jalan. `503 ONBOARDING_CATALOG_UNAVAILABLE`
     * bahkan bukan kerusakan: itu jawaban server saat katalognya dimatikan feature flag.
     *
     * Yang hilang saat fallback hanya setoran awal dan teks fitur yang terkini. Karena
     * nilai yang tampil itulah yang dicatat server sebagai "yang dilihat nasabah", angka
     * bawaan sengaja dibuat sama dengan isi katalog.
     *
     * Katalog yang masih segar tidak ditarik ulang, tapi yang sudah lewat 5 menit ditarik:
     * umur itu mengikuti `max-age=300` dari server. Guard "sudah ada, lewati" tanpa batas
     * umur membuat flow yang dibiarkan terbuka menampilkan setoran awal yang sudah berubah,
     * padahal angka yang tampil itulah yang dicatat server sebagai yang dilihat nasabah.
     */
    private fun loadCatalog() {
        if (store.current.isProductCatalogFresh()) return

        launchWithLoading {
            when (val result = repository.savingsProducts()) {
                is DataResult.Success -> applyCatalog(result.value)
                is DataResult.Failure -> Unit
            }
        }
    }

    /**
     * Muat ulang yang diminta nasabah dari penanda daftar bawaan.
     *
     * Tidak lewat `launchWithLoading`: penanda kemajuannya sendiri
     * ([BukaRekeningFlowState.isRefreshingProductCatalog]), supaya daftar yang sedang
     * dibaca tidak diganti spinner layar penuh.
     *
     * Tidak memeriksa kesegaran katalog — kalau nasabah meminta, itu permintaan, bukan
     * saran. Yang dijaga hanya satu permintaan pada satu waktu.
     *
     * Kegagalannya tetap sunyi, sama seperti [loadCatalog]: penandanya tinggal di tempat
     * dan tombolnya bisa ditekan lagi. ATURAN #3 skill tidak berubah di sini.
     */
    private fun reloadCatalog() {
        if (store.current.isRefreshingProductCatalog) return

        // Ditandai di sini, BUKAN di dalam coroutine: `viewModelScope.launch` menjadwalkan
        // body-nya, tidak menjalankannya seketika. Penanda di dalam coroutine berarti
        // tekanan tombol kedua yang datang sebelum body jalan ikut lolos guard di atas —
        // dua request untuk satu permintaan. Ketahuan oleh test "muat ulang ganda".
        store.update { it.copy(isRefreshingProductCatalog = true) }
        viewModelScope.launch {
            try {
                when (val result = repository.savingsProducts()) {
                    is DataResult.Success -> applyCatalog(result.value)
                    is DataResult.Failure -> Unit
                }
            } finally {
                // Tanpa finally, satu exception mengunci tombolnya mati selamanya.
                store.update { it.copy(isRefreshingProductCatalog = false) }
            }
        }
    }

    private fun applyCatalog(catalog: SavingsProductCatalog) {
        store.update {
            it.copy(
                productCatalog = catalog,
                productCatalogFetchedAt = System.currentTimeMillis(),
                // Pilihan nasabah yang sudah ada (mis. kembali lewat Back, atau dipilih
                // dari daftar bawaan sebelum katalog datang) menang atas default server.
                selectedProductType = it.selectedProductType ?: catalog.defaultSelection(),
            )
        }
    }

    private fun resumeDraft() {
        val sessionId = repository.savedSessionId() ?: return
        launchWithLoading {
            when (val result = repository.getSession(sessionId)) {
                is DataResult.Success -> {
                    val session = result.value
                    store.update {
                        it.copy(
                            sessionId = session.sessionId,
                            product = session.product,
                            // Produk sesi yang dilanjutkan menang atas pilihan lokal;
                            // `product` boleh kosong di response, jadi pilihan lama dijaga.
                            selectedProductType = session.product?.type ?: it.selectedProductType,
                            selectedCard = session.card,
                            currentStep = session.currentStep,
                            isOtpVerified = session.stepsCompleted.otpVerified,
                            isVideoCallVerified = session.stepsCompleted.videoCallVerified,
                            isCredentialsSaved = session.stepsCompleted.credentialsSet,
                            hasResumableDraft = false,
                        )
                    }
                    store.send(BukaRekeningSideEffect.AdvanceTo(session.currentStep))
                }
                is DataResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun abandonFlow() {
        viewModelScope.launch {
            repository.cancelSession()
            store.clear()
            store.send(BukaRekeningSideEffect.ExitFlow)
        }
    }
}

/**
 * Produk yang terpilih saat layar pertama dibuka: `is_default` dari server, dan kalau
 * server tidak menandai apa pun, produk pertama yang **bisa** dipilih.
 *
 * Bukan "produk pertama": baris pertama bisa saja sedang tutup, dan memilihkannya berarti
 * tombol Lanjut mengirim produk yang pasti ditolak `422 ONBOARDING_PRODUCT_UNAVAILABLE`.
 */
private fun SavingsProductCatalog.defaultSelection() =
    sortedProducts.firstOrNull { it.isDefault && it.availability.isSelectable }?.type
        ?: sortedProducts.firstOrNull { it.availability.isSelectable }?.type
