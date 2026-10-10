package id.bca.bcamobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import id.bca.bcamobile.core.camera.CameraCapture
import java.io.File

/**
 * Menampilkan satu foto dari berkas cache lokal.
 *
 * Ada karena project ini **tidak memakai Coil atau Glide** dan tidak boleh
 * menambah dependency tanpa persetujuan. Yang dibutuhkan juga jauh lebih sempit
 * daripada pustaka pemuat gambar: satu berkas lokal, sekali tampil, tanpa cache
 * jaringan dan tanpa cache disk.
 *
 * Dekodenya lewat [CameraCapture.decode], yang sudah menurunkan ukuran lewat
 * `inSampleSize` dan berjalan di `Dispatchers.IO`. Itu penting: JPEG hasil
 * `CAPTURE_MODE_MAXIMIZE_QUALITY` bisa belasan megapiksel, dan men-dekodenya
 * seukuran aslinya di main thread berarti jank atau OOM.
 *
 * Bitmap-nya hidup di composition, **tidak** di UiState: menyimpan bitmap di
 * state yang di-hoist ke graph berarti piksel wajah dan e-KTP ikut bertahan
 * selama flow berjalan, padahal yang dibutuhkan hanya selama layarnya terlihat.
 */
@Composable
fun LocalPhotoPreview(
    file: File?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // Kunci produceState adalah berkasnya: foto yang diganti (ambil ulang) harus
    // memicu dekode baru, bukan menampilkan foto sebelumnya.
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, file) {
        value = file?.let { CameraCapture.decode(it)?.asImageBitmap() }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        val current = bitmap
        if (current == null) {
            // Berkas yang gagal di-dekode berhenti di indikator ini, bukan jatuh
            // ke kotak kosong tanpa penjelasan. Tidak ada teks di sini supaya
            // komponen tetap bebas dari string layar tertentu.
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = current,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
