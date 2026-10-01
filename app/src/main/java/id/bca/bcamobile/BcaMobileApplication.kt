package id.bca.bcamobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.bca.bcamobile.core.push.PushChannels

@HiltAndroidApp
class BcaMobileApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Dibuat di sini, bukan saat notifikasi pertama datang: notifikasi yang
        // menyebut channel belum ada akan dibuang sistem tanpa pesan error, dan
        // yang pertama datang justru bisa saat aplikasi belum pernah dibuka.
        PushChannels.create(this)
    }
}
