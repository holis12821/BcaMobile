package id.bca.bcamobile.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
}
