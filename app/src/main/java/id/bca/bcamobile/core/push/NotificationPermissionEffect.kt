package id.bca.bcamobile.core.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Meminta izin `POST_NOTIFICATIONS` sekali, saat [enabled] menjadi true.
 *
 * Pemanggilnya menyalakan ini **setelah login berhasil**, bukan saat aplikasi
 * pertama dibuka: nasabah yang belum tahu apa gunanya cenderung menolak, dan
 * penolakan kedua bersifat permanen — sesudah itu izinnya hanya bisa dinyalakan
 * dari Setelan sistem.
 *
 * Di bawah API 33 izin ini tidak ada, jadi tidak ada yang dikerjakan.
 *
 * **Penolakan bukan kegagalan.** Token tetap didaftarkan dan baris notifikasi
 * tetap ditulis server, jadi layar Notifikasi tetap terisi. Tidak ada yang
 * diblokir, dan tidak ada pesan error yang ditampilkan.
 */
@Composable
fun NotificationPermissionEffect(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    // Satu kali per proses: sistem mengabaikan permintaan kedua setelah nasabah
    // menolak, dan memintanya berulang kali hanya mengganggu.
    val alreadyAsked = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* Hasilnya tidak mengubah apa pun — lihat KDoc. */ }

    LaunchedEffect(enabled) {
        if (!enabled || alreadyAsked.value) return@LaunchedEffect
        alreadyAsked.value = true

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
