package id.bca.bcamobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.bca.bcamobile.core.network.NetworkStatusMonitor
import id.bca.bcamobile.core.push.PushChannels
import javax.inject.Inject

@HiltAndroidApp
class BcaMobileApplication : Application() {

    /**
     * Pengamat jaringan untuk lampu indikator.
     *
     * Disuntik di sini supaya ia hidup selama proses, bukan selama satu layar:
     * lampunya tampil di setiap halaman, dan monitor yang ikut mati bersama satu
     * layar akan kehilangan riwayat panggilan setiap kali nasabah berpindah.
     */
    @Inject
    lateinit var networkStatusMonitor: NetworkStatusMonitor

    override fun onCreate() {
        super.onCreate()
        // Dibuat di sini, bukan saat notifikasi pertama datang: notifikasi yang
        // menyebut channel belum ada akan dibuang sistem tanpa pesan error, dan
        // yang pertama datang justru bisa saat aplikasi belum pernah dibuka.
        PushChannels.create(this)

        // start(), bukan init kelasnya: pemasangan observer ProcessLifecycleOwner
        // mensyaratkan main thread, dan onCreate dijamin berjalan di situ.
        networkStatusMonitor.start()
    }
}
