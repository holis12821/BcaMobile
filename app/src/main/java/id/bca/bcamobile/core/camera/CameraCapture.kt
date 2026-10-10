package id.bca.bcamobile.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import id.bca.bcamobile.ui.screen.buka_rekening.common.FlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Hasil satu kali jepretan.
 *
 * [file] selalu berada di `cacheDir` dan wajib dihapus lewat [CameraCapture.discard]
 * setelah terunggah — foto e-KTP dan wajah tidak boleh menetap di perangkat.
 */
data class CapturedPhoto(
    val file: File,
    val width: Int,
    val height: Int,
) {
    /** Format `1920x1080`, dipakai sebagai field `resolution` saat unggah OCR. */
    val resolution: String get() = "${width}x$height"
}

/**
 * Pembungkus tipis di atas [LifecycleCameraController].
 *
 * Dipisah dari composable supaya layar tetap stateless: layar hanya menerima
 * slot preview dan callback, tidak pernah memegang controller.
 */
object CameraCapture {

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Executor untuk ImageAnalysis.
     *
     * Wajib **bukan** main executor: analisa ML Kit mengonversi frame penuh jadi
     * bitmap, dan menjalankannya di main thread membuat preview tersendat sampai ANR.
     */
    val analysisExecutor: Executor = Executors.newSingleThreadExecutor()

    fun createController(
        context: Context,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    ): LifecycleCameraController = LifecycleCameraController(context).apply {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }

    fun applyFlashMode(controller: LifecycleCameraController, mode: FlashMode) {
        controller.imageCaptureFlashMode = when (mode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    /**
     * Menjepret lalu menulis JPEG terkompresi ke cache.
     *
     * Kompresi 85% mengikuti catatan Android di kontrak OCR — cukup untuk dibaca
     * server, tapi tidak membuat unggahan menembus batas 10 MB.
     */
    suspend fun capture(
        context: Context,
        controller: LifecycleCameraController,
        fileNamePrefix: String,
        quality: Int = JPEG_QUALITY,
    ): CapturedPhoto? {
        val raw = takePicture(controller) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val target = File.createTempFile(fileNamePrefix, JPEG_SUFFIX, context.cacheDir)
                FileOutputStream(target).use { out ->
                    raw.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                CapturedPhoto(target, raw.width, raw.height)
            }.getOrNull().also { raw.recycle() }
        }
    }

    /**
     * Menyalin gambar pilihan dari galeri jadi berkas cache yang tegak.
     *
     * Dipakai tombol "Dari Galeri" pada layar kamera e-KTP, yang sebelumnya
     * terhubung ke lambda kosong — tombolnya ada, menekannya tidak melakukan
     * apa pun. Hasilnya dialirkan ke jalur yang sama persis dengan hasil
     * jepretan, jadi OCR di perangkat, gerbang kualitas, dan validasi server
     * berlaku sama untuk keduanya.
     *
     * **Orientasi EXIF diterapkan lalu ditulis ulang**, bukan dibiarkan. Foto
     * galeri umumnya menyimpan rotasinya di EXIF sementara piksel-nya tetap
     * mendatar, dan `BitmapFactory` mengabaikan EXIF — jadi e-KTP yang difoto
     * tegak akan dibaca OCR dalam keadaan miring 90° dan tidak terbaca sama
     * sekali. Setelah ditulis ulang, seluruh jalur di hilir melihat JPEG tegak.
     *
     * Berkasnya di `cacheDir` dan wajib dihapus lewat [discard] setelah
     * terunggah, sama seperti hasil jepretan: foto e-KTP tidak boleh menetap.
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        fileNamePrefix: String,
        quality: Int = JPEG_QUALITY,
    ): CapturedPhoto? = withContext(Dispatchers.IO) {
        runCatching {
            val rotation = exifRotationOf(context, uri)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return@runCatching null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            // Dibatasi seperti jalur dekode OCR: gambar galeri bisa jauh lebih
            // besar daripada hasil kamera, dan bitmap seukuran itu memicu OOM.
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, IMPORT_MAX_DIMENSION)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@runCatching null

            val upright = decoded.rotated(rotation)

            val target = File.createTempFile(fileNamePrefix, JPEG_SUFFIX, context.cacheDir)
            FileOutputStream(target).use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            CapturedPhoto(target, upright.width, upright.height)
                .also { upright.recycle() }
        }.getOrNull()
    }

    /**
     * Derajat rotasi yang harus diterapkan supaya gambarnya tegak.
     *
     * `android.media.ExifInterface` dipakai, bukan androidx — fungsinya cukup
     * untuk membaca satu tag dan tidak menambah dependency.
     */
    private fun exifRotationOf(context: Context, uri: Uri): Int {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /** Hapus berkas sementara. Panggil setelah unggah selesai, sukses maupun gagal. */
    fun discard(file: File?) {
        if (file == null) return
        runCatching { if (file.exists()) file.delete() }
    }

    private suspend fun takePicture(controller: LifecycleCameraController): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            controller.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = runCatching { image.toBitmap() }.getOrNull()
                        image.close()
                        continuation.resume(bitmap?.rotated(rotation))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(null)
                    }
                },
            )
        }

    /** CameraX menyerahkan bitmap dalam orientasi sensor; OCR butuh orientasi tegak. */
    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
            .also { if (it != this) recycle() }
    }

    /**
     * Membaca ulang berkas cache jadi bitmap, dipakai saat OCR dijalankan terpisah.
     *
     * Dua hal yang disengaja di sini:
     *
     * - **suspend + [Dispatchers.IO]** — dekode JPEG hasil `CAPTURE_MODE_MAXIMIZE_QUALITY`
     *   bisa belasan megapiksel dan memakan ratusan milidetik. Di main thread itu
     *   jank, bahkan ANR pada perangkat kelas bawah.
     * - **[inSampleSize]** — bitmap seukuran sensor menghabiskan puluhan MB dan
     *   memicu OOM. ML Kit tidak butuh itu; sisi terpanjang [maxDimension] piksel
     *   masih jauh di atas ambang agar NIK 16 digit terbaca.
     *
     * Berkas aslinya tidak disentuh — yang diunggah ke server tetap foto resolusi penuh.
     */
    suspend fun decode(file: File, maxDimension: Int = OCR_MAX_DIMENSION): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    return@runCatching null
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
            }.getOrNull()
        }

    /** Pangkat dua terkecil yang membuat sisi terpanjang turun ke bawah [maxDimension]. */
    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private const val JPEG_QUALITY = 85
    private const val JPEG_SUFFIX = ".jpg"

    /** Sisi terpanjang bitmap yang dipakai OCR di perangkat. */
    private const val OCR_MAX_DIMENSION = 1920

    /**
     * Sisi terpanjang gambar yang diimpor dari galeri.
     *
     * Sama dengan [OCR_MAX_DIMENSION] dan bukan kebetulan: berkas inilah yang
     * dibaca OCR **dan** diunggah, jadi menyimpannya lebih besar hanya menambah
     * ukuran unggahan tanpa menambah yang bisa dibaca. Tetap jauh di atas batas
     * minimum 640×480 yang diperiksa server.
     */
    private const val IMPORT_MAX_DIMENSION = 1920
}
