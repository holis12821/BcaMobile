package id.bca.bcamobile.ui.components

import android.content.res.Resources
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import id.bca.bcamobile.core.network.ErrorText

/**
 * Tempat menampilkan pesan sekali pakai, disediakan sekali di root navigasi.
 *
 * Layar buka rekening tidak memakai `Scaffold`, jadi tidak ada slot snackbar per
 * layar. Menaruh host-nya di root membuat side effect seperti
 * `BukaRekeningSideEffect.ShowMessage` punya tujuan tanpa mengubah kontrak
 * composable layar mana pun — layar tetap stateless.
 *
 * Nilai bawaannya host lepas yang tidak terpasang ke `SnackbarHost`: pesan
 * dibuang diam-diam, sehingga `@Preview` tetap bisa dirender tanpa penyedia.
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Versi non-composable dari `ErrorText.resolve()`.
 *
 * Dipakai di dalam blok `collect` side effect, tempat `stringResource` tidak
 * bisa dipanggil karena bukan konteks komposisi.
 *
 * Menerima [Resources] dari `LocalResources`, bukan `LocalContext`: pembacaan
 * `LocalContext` tidak ikut batal saat konfigurasi berubah, sehingga teks bisa
 * tertinggal di bahasa lama setelah pengguna mengganti bahasa perangkat.
 */
fun ErrorText.resolve(resources: Resources): String = when (this) {
    is ErrorText.Res -> resources.getString(id, *args.toTypedArray())
    is ErrorText.Raw -> value
}
