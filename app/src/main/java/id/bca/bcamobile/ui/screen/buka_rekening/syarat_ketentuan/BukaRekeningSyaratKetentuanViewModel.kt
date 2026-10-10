package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSessionStore
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningSideEffect
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningStepViewModel
import id.bca.bcamobile.ui.screen.buka_rekening.common.selectedCardType
import id.bca.bcamobile.ui.screen.buka_rekening.common.toErrorText
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BukaRekeningSyaratKetentuanViewModel @Inject constructor(
    repository: OnboardingRepository,
    store: BukaRekeningSessionStore,
) : BukaRekeningStepViewModel(repository, store) {

    private val _tnc = MutableStateFlow(TncLoadState())

    /** Dokumen S&K terpisah dari state flow bersama — lihat [TncLoadState]. */
    val tnc: StateFlow<TncLoadState> = _tnc.asStateFlow()

    init {
        loadTnc()
    }

    fun onEvent(event: SyaratKetentuanEvent) {
        when (event) {
            SyaratKetentuanEvent.TncAccepted -> createSession()
            SyaratKetentuanEvent.TncReloadRequested -> loadTnc()
        }
    }

    /**
     * Teks S&K selalu diminta tanpa parameter `version`, jadi yang datang adalah versi
     * yang **sedang** berlaku. Memintanya di `init` membuat nasabah bisa membaca lebih
     * dulu; memintanya saat tombol ditekan berarti dia menyetujui teks yang belum ada.
     */
    private fun loadTnc() {
        if (_tnc.value.isLoading) return
        _tnc.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val result = repository.tnc()) {
                is DataResult.Success -> _tnc.value = TncLoadState(document = result.value)
                is DataResult.Failure ->
                    _tnc.value = TncLoadState(error = result.error.toErrorText())
            }
        }
    }

    private fun createSession() {
        val current = store.current
        // Kode produk, bukan indeks baris: urutan tampil milik server.
        val productType = current.selectedProductType ?: return
        // Versi yang tercatat sebagai disetujui harus versi dokumen yang benar-benar
        // terpampang saat tombol ditekan — bukan konstanta, bukan nilai yang di-cache dari
        // pembukaan layar sebelumnya. Itu yang diperiksa server, dan selisihnya dijawab 409.
        val document = _tnc.value.document ?: return

        launchWithLoading {
            val result = repository.createSession(
                productType = productType,
                acceptedTncVersion = document.version,
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
                is DataResult.Failure -> handleCreateSessionFailure(result.error)
            }
        }
    }

    /**
     * `TNC_VERSION_OUTDATED` adalah "muat ulang", bukan "coba lagi".
     *
     * Mengulang request dengan versi yang sama dijamin gagal lagi, jadi yang dilakukan
     * adalah menarik dokumen terbaru dan memberi tahu nasabah. Centang persetujuan ikut
     * kosong dengan sendirinya: `remember` di layar dikunci ke versi dokumen, jadi versi
     * berganti berarti centangnya lahir ulang. Itu wajib — nasabah yang mencentang lalu
     * teksnya berganti di bawahnya belum menyetujui apa pun, dan membiarkan centang itu
     * hidup mengulangi persis kerusakan yang `409` tolak.
     *
     * Pesannya lewat snackbar, bukan error inline: area pasal sedang diganti isinya, dan
     * teks error di sana akan hilang bersama kerangka muatnya.
     *
     * [ApiFailure.Business] ber-`TNC_VERSION_UNKNOWN` sengaja **tidak** ditangani di sini.
     * Versi yang tidak pernah ada berarti bug client, bukan keadaan yang nasabah bisa
     * pulihkan, jadi biarkan jatuh ke penanganan umum.
     */
    private suspend fun handleCreateSessionFailure(error: ApiFailure) {
        if (error !is ApiFailure.TncOutdated) {
            handleFailure(error)
            return
        }

        store.send(BukaRekeningSideEffect.ShowMessage(error.toErrorText()))
        loadTnc()
    }
}
