package id.bca.bcamobile.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Titik masuk FCM.
 *
 * **[onMessageReceived] hanya dipanggil saat aplikasi di foreground.** Muatan dari
 * server memuat blok `notification`, jadi ketika aplikasi di background atau mati,
 * SDK FCM menampilkan notifikasinya sendiri ke system tray dan metode ini tidak
 * pernah jalan.
 *
 * Akibatnya — dan ini yang paling sering salah dikerjakan — **penanganan tap tidak
 * ada di sini.** Notifikasi yang ditekan nasabah hampir selalu yang muncul saat
 * aplikasi di background, yaitu justru kasus di mana kelas ini tidak dipanggil.
 * Tap membuka `MainActivity` dan `data` sampai sebagai extras Intent; dibaca di
 * sana (lihat [PushDeepLink]).
 *
 * Kelas ini juga tidak membangun notifikasi tray sendiri: saat aplikasi terlihat,
 * nasabah akan melihat dua hal untuk satu kejadian. Yang dilakukan hanya
 * menyegarkan keadaan lewat [PushEventBus].
 *
 * Kalau nanti backend berpindah ke pesan data-only, metode ini akan selalu
 * dipanggil dan penampilan notifikasinya jadi tanggung jawab kelas ini — periksa
 * muatannya sebelum berasumsi itu sudah terjadi.
 */
@AndroidEntryPoint
class BcaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushTokenRegistrar

    @Inject lateinit var eventBus: PushEventBus

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Isi pesan sengaja tidak dibaca maupun dicatat: badan notifikasi memuat
        // nominal dan nama tujuan, dan log client ikut terkirim saat nasabah
        // melaporkan keluhan. Yang diperlukan hanya tanda "ada yang baru" —
        // isinya ditarik ulang dari server.
        eventBus.onMessageArrived()
    }
}
