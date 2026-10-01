package id.bca.bcamobile.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import id.bca.bcamobile.R

/**
 * Channel notifikasi sistem.
 *
 * `minSdk = 26`, jadi channel **selalu** wajib dan tidak perlu penjagaan versi —
 * notifikasi tanpa channel dibuang tanpa pesan error apa pun.
 *
 * Keamanan dipisahkan dari sisanya dengan sengaja: nasabah boleh mematikan
 * pemberitahuan transaksi dan promo yang terasa ramai, tapi tidak boleh mematikan
 * peringatan "kode akses Anda diubah" tanpa menyadari itu yang ia matikan.
 *
 * **Yang menentukan sebuah notifikasi masuk channel mana adalah muatan FCM, bukan
 * kode ini.** Untuk pesan ber-`notification` yang ditampilkan SDK saat aplikasi di
 * background, channel-nya diambil dari `android.notification.channel_id`; kalau
 * tidak ada, dari meta-data `default_notification_channel_id` di manifest — yaitu
 * [R.string.push_channel_umum_id]. Jadi sampai backend mengirim
 * `channel_id = bca_keamanan` untuk notifikasi bertipe `SECURITY`, seluruh
 * notifikasi mendarat di channel umum. Channel keamanan sudah dibuat supaya
 * pemisahannya berlaku begitu backend mengirimnya, tanpa nasabah harus mengatur
 * ulang preferensinya.
 *
 * Channel yang dimatikan dari Setelan sistem berada di luar kendali aplikasi dan
 * server — berbeda dari sakelar `push_notification_enabled`, yang ditegakkan server
 * dan tetap meloloskan notifikasi keamanan.
 */
object PushChannels {

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val security = NotificationChannel(
            context.getString(R.string.push_channel_keamanan_id),
            context.getString(R.string.push_channel_keamanan_nama),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_channel_keamanan_deskripsi)
        }

        val general = NotificationChannel(
            context.getString(R.string.push_channel_umum_id),
            context.getString(R.string.push_channel_umum_nama),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.push_channel_umum_deskripsi)
        }

        // Aman dipanggil berulang: channel dengan id yang sama tidak dibuat dua kali,
        // dan perubahan nasabah atas suara/getar tidak ditimpa.
        manager.createNotificationChannels(listOf(security, general))
    }
}
