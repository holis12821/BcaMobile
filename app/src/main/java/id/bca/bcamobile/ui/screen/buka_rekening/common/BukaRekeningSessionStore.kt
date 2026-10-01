package id.bca.bcamobile.ui.screen.buka_rekening.common

import dagger.hilt.android.scopes.ActivityRetainedScoped
import id.bca.bcamobile.core.camera.CameraCapture
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

/**
 * Pemegang state bersama seluruh flow buka rekening.
 *
 * Empat belas layar berbagi satu sesi — `session_id`, hasil OCR, produk dan kartu
 * terpilih, status OTP, biometrik, antrean, kredensial. Sejak tiap layar punya
 * ViewModel sendiri, data itu tidak bisa lagi menumpang di satu ViewModel flow,
 * jadi tinggal di sini.
 *
 * **Umurnya bukan umur Activity.** Scope Hilt-nya memang `ActivityRetained`, tapi
 * [BukaRekeningFlowScopeViewModel] yang di-scope ke graph memanggil [clear] di
 * `onCleared()`. Jadi PII di sini ikut hilang begitu graph buka rekening dilepas
 * dari back stack — sama persis dengan perilaku ViewModel flow yang lama, dan
 * tanpa perlu diingat-ingat di tiap titik keluar.
 *
 * Tidak ada yang ditulis ke disk dari sini (aturan PII #5).
 */
@ActivityRetainedScoped
class BukaRekeningSessionStore @Inject constructor() {

    private val _state = MutableStateFlow(BukaRekeningFlowState())
    val state: StateFlow<BukaRekeningFlowState> = _state.asStateFlow()

    private val _sideEffect = Channel<BukaRekeningSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<BukaRekeningSideEffect> = _sideEffect.receiveAsFlow()

    val current: BukaRekeningFlowState get() = _state.value

    fun update(block: (BukaRekeningFlowState) -> BukaRekeningFlowState) {
        _state.update(block)
    }

    fun reset(state: BukaRekeningFlowState = BukaRekeningFlowState()) {
        _state.value = state
    }

    suspend fun send(effect: BukaRekeningSideEffect) {
        _sideEffect.send(effect)
    }

    private var cachedIdempotencyKey: String? = null

    /**
     * Dibuat sekali lalu dipakai ulang, supaya retry submit tidak membuat rekening
     * kedua. Tinggal di store, bukan di ViewModel layar Ringkasan: kalau nasabah
     * meninggalkan layar itu lalu kembali, kuncinya harus tetap sama.
     */
    fun idempotencyKey(): String =
        cachedIdempotencyKey ?: UUID.randomUUID().toString().also { cachedIdempotencyKey = it }

    /**
     * Membuang seluruh state flow berikut berkas foto yang masih menggantung di
     * cache. Dipanggil saat graph dilepas, sesi dibatalkan, dan sesi hangus.
     */
    fun clear() {
        CameraCapture.discard(_state.value.ktpPhoto)
        cachedIdempotencyKey = null
        _state.value = BukaRekeningFlowState()
    }
}
