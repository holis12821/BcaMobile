package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel penanda umur flow buka rekening.
 *
 * Tidak memproses event apa pun. Tugasnya satu: di-resolve dengan
 * `hiltViewModel(parentEntry)` pada graph buka rekening supaya umurnya sama
 * dengan graph, lalu membersihkan [BukaRekeningSessionStore] saat graph lepas.
 *
 * Tanpa ini, [BukaRekeningSessionStore] akan hidup sampai Activity mati dan PII
 * nasabah — foto e-KTP, NIK, kode akses, PIN — ikut menetap di memory jauh
 * setelah nasabah meninggalkan flow.
 *
 * Graph juga memakainya sebagai sumber [state] dan [sideEffect] bersama.
 */
@HiltViewModel
class BukaRekeningFlowScopeViewModel @Inject constructor(
    private val store: BukaRekeningSessionStore,
) : ViewModel() {

    val state = store.state
    val sideEffect = store.sideEffect

    override fun onCleared() {
        store.clear()
    }
}
