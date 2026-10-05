package id.bca.bcamobile.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Izin yang **wajib** ada sebelum video call e-KYC boleh dimulai.
 *
 * Kamera **dan** mikrofon, bukan kamera saja: verifikasi tatap muka digital mewajibkan
 * nasabah menjawab pertanyaan petugas secara langsung (`06-BUKA-REKENING-API-SPEC.md`
 * §3c), jadi panggilan tanpa audio tidak ada gunanya diteruskan.
 *
 * Dipisah dari [hasCameraPermission] yang dipakai foto KTP dan liveness — keduanya memang
 * hanya butuh kamera, dan menyatukannya akan meminta mikrofon di layar yang tidak
 * memerlukannya.
 */
val VIDEO_CALL_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
)

/**
 * Izin yang **enak dimiliki** tapi tidak boleh memblokir panggilan.
 *
 * `BLUETOOTH_CONNECT` hanya menentukan apakah audio bisa dialihkan ke headset; tanpa itu
 * speaker perangkat tetap bekerja. Karena itu ia tidak pernah masuk
 * [VIDEO_CALL_PERMISSIONS]: kalau ikut di sana, nasabah yang menolak akses Bluetooth
 * kehilangan seluruh verifikasi identitasnya karena alasan yang tidak relevan.
 *
 * Kosong di bawah API 31, tempat izin ini belum ada — padanannya `BLUETOOTH` yang
 * install-time dan sudah dideklarasikan di manifest.
 */
fun videoCallOptionalPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray()
    }

/** Diminta sekaligus dalam satu dialog; hasil yang opsional diabaikan saat menilai. */
fun videoCallRequestedPermissions(): Array<String> =
    VIDEO_CALL_PERMISSIONS + videoCallOptionalPermissions()

fun Context.hasVideoCallPermissions(): Boolean = VIDEO_CALL_PERMISSIONS.all { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Hanya menilai izin wajib.
 *
 * Dipakai sebagai hasil launcher: peta hasilnya memuat Bluetooth juga, dan menilai
 * `values.all { it }` akan menganggap panggilan gagal padahal kamera dan mikrofon sudah
 * diberikan.
 */
fun Map<String, Boolean>.hasRequiredVideoCallPermissions(): Boolean =
    VIDEO_CALL_PERMISSIONS.all { this[it] == true }

/**
 * Izin Bluetooth saja, dinilai terpisah dari yang wajib.
 *
 * `true` di bawah API 31: izin runtime-nya belum ada di sana, dan padanan install-time
 * `BLUETOOTH` sudah dideklarasikan di manifest — jadi routing ke headset memang boleh
 * dicoba tanpa dialog apa pun.
 */
fun Context.hasVideoCallBluetoothPermission(): Boolean =
    videoCallOptionalPermissions().all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
