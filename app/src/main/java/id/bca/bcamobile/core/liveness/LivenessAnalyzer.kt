package id.bca.bcamobile.core.liveness

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import id.bca.bcamobile.core.camera.CameraCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Menjembatani aliran frame CameraX ke [LivenessDetector].
 *
 * CameraX memanggil [analyze] di thread-nya sendiri dan menuntut setiap
 * [ImageProxy] ditutup; deteksi ML Kit sendiri suspend. Kelas ini yang
 * menjahit keduanya, sekaligus menjaga agar hanya satu frame diproses pada
 * satu waktu — frame yang datang saat detektor masih sibuk dibuang, bukan
 * diantre, supaya preview tidak tersendat.
 */
class LivenessAnalyzer(
    private val context: Context,
    private val detector: LivenessDetector,
    private val scope: CoroutineScope,
    private val onProgress: (LivenessProgress) -> Unit,
    private val onFramesReady: (List<File>) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var busy = false

    @Volatile
    private var finished = false

    private val frames = mutableListOf<File>()
    private var lastCompletedActions = 0

    /** true setelah [onFramesReady] dipanggil: sejak itu frame jadi milik pemanggil. */
    @Volatile
    private var handedOff = false

    override fun analyze(image: ImageProxy) {
        if (busy || finished) {
            image.close()
            return
        }
        busy = true

        // Dispatchers.Default, bukan dispatcher bawaan scope (Main): konversi frame
        // jadi bitmap terlalu berat untuk main thread.
        scope.launch(Dispatchers.Default) {
            val snapshot = runCatching { image.toBitmap() }.getOrNull()
            val progress = detector.analyze(image)
            onProgress(progress)

            // Satu frame bukti disimpan tiap kali satu tantangan terlewati.
            if (progress.completedActions > lastCompletedActions && snapshot != null) {
                lastCompletedActions = progress.completedActions
                persist(snapshot)?.let { frames += it }
            }
            snapshot?.recycle()

            if (progress.isComplete && !finished) {
                finished = true
                handedOff = true
                onFramesReady(frames.toList())
            }
        }.invokeOnCompletion {
            // Harus invokeOnCompletion, bukan `finally` di dalam blok: kalau scope
            // sudah dibatalkan (layar dipop tepat setelah busy diset), badan coroutine
            // tidak pernah dijalankan sama sekali. ImageProxy yang tidak ditutup
            // menghabiskan buffer ImageReader dan membuat pipeline kamera berhenti.
            image.close()
            busy = false
        }
    }

    /** Frame bukti ditulis ke cache dan dihapus lagi setelah diunggah. */
    private suspend fun persist(bitmap: Bitmap): File? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File.createTempFile(FRAME_PREFIX, FRAME_SUFFIX, context.cacheDir)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, FRAME_QUALITY, out)
            }
            file
        }.getOrNull()
    }

    fun reset() {
        detector.reset()
        frames.clear()
        lastCompletedActions = 0
        finished = false
        handedOff = false
    }

    /**
     * Melepas detektor dan membuang frame bukti yang belum sempat diserahkan.
     *
     * Frame yang sudah lewat [onFramesReady] tidak disentuh — sejak saat itu
     * pemanggil yang bertanggung jawab menghapusnya setelah unggah. Yang dibuang
     * di sini adalah sisa sesi yang ditinggalkan di tengah jalan; foto wajah tidak
     * boleh menetap di cache perangkat.
     */
    fun close() {
        finished = true
        detector.close()
        if (!handedOff) frames.forEach(CameraCapture::discard)
        frames.clear()
    }

    private companion object {
        const val FRAME_PREFIX = "liveness_"
        const val FRAME_SUFFIX = ".jpg"
        const val FRAME_QUALITY = 80
    }
}
