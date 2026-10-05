package id.bca.bcamobile.core.videocall

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Mengarahkan audio panggilan ke speaker — atau ke headset Bluetooth kalau ada.
 *
 * Default sistem untuk panggilan adalah **earpiece**, dan itu salah untuk video call e-KYC:
 * nasabah memegang perangkat sejauh wajah plus e-KTP supaya keduanya terlihat petugas, jadi
 * earpiece praktis tidak terdengar. Skill `buka-rekening-video-call` §10 menyebutnya
 * eksplisit: speaker, bukan earpiece.
 *
 * Butuh `MODIFY_AUDIO_SETTINGS` (install-time) untuk mengubah mode dan speaker, dan
 * `BLUETOOTH_CONNECT` (runtime, API 31+) hanya untuk cabang headset. Keduanya sudah
 * dideklarasikan di manifest.
 *
 * [restore] wajib dipanggil saat panggilan selesai. Mode dan speaker itu **setelan
 * global**: dibiarkan menyala, pemutar musik dan panggilan telepon sesudahnya ikut keluar
 * dari speaker, dan pengguna tidak akan menghubungkannya ke aplikasi ini.
 */
internal class VideoCallAudioRouter(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var previousMode: Int? = null
    private var previousSpeakerOn: Boolean? = null

    /**
     * @param allowBluetooth hasil izin `BLUETOOTH_CONNECT`. `false` berarti cabang headset
     *   dilewati sama sekali — bukan dicoba lalu gagal, karena `setCommunicationDevice` ke
     *   perangkat Bluetooth tanpa izin melempar `SecurityException`.
     */
    fun start(allowBluetooth: Boolean) {
        val manager = audioManager ?: return

        previousMode = manager.mode
        previousSpeakerOn = @Suppress("DEPRECATION") manager.isSpeakerphoneOn

        // MODE_IN_COMMUNICATION mengaktifkan pembatalan gema dan penekan bising milik
        // perangkat. Tanpa itu suara petugas yang keluar dari speaker tertangkap mikrofon
        // dan kembali sebagai gema.
        manager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            routeModern(manager, allowBluetooth)
        } else {
            @Suppress("DEPRECATION")
            manager.isSpeakerphoneOn = true
        }
    }

    /**
     * API 31+ punya pemilihan perangkat yang eksplisit, jadi tidak perlu lagi menebak lewat
     * `isSpeakerphoneOn`. Headset diutamakan kalau memang terpasang: nasabah yang sengaja
     * memakainya tidak ingin suaranya pindah ke speaker.
     *
     * `@RequiresApi` bukan formalitas: penjaga versinya ada di [start], satu tingkat di atas,
     * dan lint tidak menembus batas fungsi. Tanpa anotasi ini `minSdk 26` membuat lint
     * benar-benar menggagalkan build — dan kalau penjaganya suatu saat hilang, yang terjadi
     * adalah `NoSuchMethodError` di perangkat lama, bukan peringatan.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun routeModern(manager: AudioManager, allowBluetooth: Boolean) {
        val devices = manager.availableCommunicationDevices
        val bluetooth = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
        val speaker = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        val target = bluetooth?.takeIf { allowBluetooth } ?: speaker ?: return
        // Gagal memilih perangkat bukan alasan membatalkan panggilan — audio tetap
        // mengalir lewat rute bawaan sistem.
        runCatching { manager.setCommunicationDevice(target) }
    }

    fun restore() {
        val manager = audioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager.clearCommunicationDevice() }
        }
        previousSpeakerOn?.let {
            @Suppress("DEPRECATION")
            manager.isSpeakerphoneOn = it
        }
        previousMode?.let { manager.mode = it }
        previousSpeakerOn = null
        previousMode = null
    }
}
