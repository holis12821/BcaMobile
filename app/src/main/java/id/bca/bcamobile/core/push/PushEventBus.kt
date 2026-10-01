package id.bca.bcamobile.core.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Penghubung antara Firebase dan aplikasi.
 *
 * Dua kejadian yang datang dari jalur berbeda dan sengaja tidak dicampur:
 *
 * - [arrivals] — pesan masuk saat aplikasi di **foreground**
 *   ([BcaFirebaseMessagingService.onMessageReceived]). Gunanya menyegarkan layar,
 *   bukan menampilkan apa pun: nasabah sedang melihat aplikasinya.
 * - [pendingOpen] — nasabah **menekan** notifikasi di tray. Itu tidak melewati
 *   service sama sekali; muatannya sampai sebagai extras Intent dan dibaca
 *   `MainActivity`.
 *
 * Isi notifikasinya sendiri tidak pernah dibawa di sini. Muatan push tidak punya
 * `id` maupun status baca, jadi ia bukan sumber daftar — daftar dan lencana tetap
 * ditarik dari server.
 */
@Singleton
class PushEventBus @Inject constructor() {

    // extraBufferCapacity, bukan replay: penyegaran yang terlewat karena tidak ada
    // pengumpul tidak perlu diputar ulang nanti — layar memuat sendiri saat dibuka.
    private val _arrivals = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val arrivals: SharedFlow<Unit> = _arrivals.asSharedFlow()

    private val _pendingOpen = MutableStateFlow<PushOpen?>(null)

    /** Notifikasi yang sudah ditekan tapi tujuannya belum dibuka. */
    val pendingOpen: StateFlow<PushOpen?> = _pendingOpen.asStateFlow()

    fun onMessageArrived() {
        _arrivals.tryEmit(Unit)
    }

    /**
     * @param deepLink nilai `data.deep_link` apa adanya, boleh null — notifikasi
     *   keamanan tidak membawanya. Null bukan berarti tidak ada yang dibuka:
     *   tujuannya jadi layar Notifikasi (lihat [PushDeepLink.routeFor]).
     */
    fun onNotificationOpened(deepLink: String?) {
        _pendingOpen.value = PushOpen(deepLink)
    }

    fun consumePendingOpen() {
        _pendingOpen.value = null
    }
}

/** Satu penekanan notifikasi yang belum ditindaklanjuti. */
data class PushOpen(val deepLink: String?)
