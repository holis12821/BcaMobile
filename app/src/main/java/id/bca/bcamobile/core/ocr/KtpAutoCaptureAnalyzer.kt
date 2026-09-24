package id.bca.bcamobile.core.ocr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Memicu jepretan otomatis saat e-KTP sudah terbaca cukup jelas di bingkai.
 *
 * Kriterianya sengaja ketat: NIK 16 digit harus terbaca. Itu penanda paling
 * andal bahwa kartu sudah tegak, fokus, dan tidak terpotong — jauh lebih
 * bermakna daripada sekadar mendeteksi ada tulisan.
 *
 * Satu frame diproses pada satu waktu; sisanya dibuang supaya preview tetap mulus.
 */
class KtpAutoCaptureAnalyzer(
    private val recognizer: KtpTextRecognizer,
    private val scope: CoroutineScope,
    private val onDocumentReady: () -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var busy = false

    @Volatile
    private var triggered = false

    override fun analyze(image: ImageProxy) {
        if (busy || triggered) {
            image.close()
            return
        }
        busy = true

        // Dispatchers.Default, bukan dispatcher bawaan scope (Main): konversi frame
        // jadi bitmap terlalu berat untuk main thread.
        scope.launch(Dispatchers.Default) {
            val bitmap = runCatching { image.toBitmap() }.getOrNull()
            val scan = bitmap?.let { recognizer.scan(it) }
            bitmap?.recycle()

            if (scan != null && scan.data.nik.length == NIK_LENGTH && !triggered) {
                triggered = true
                onDocumentReady()
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

    fun reset() {
        triggered = false
    }

    /**
     * Melepas client ML Kit milik analyzer ini.
     *
     * [recognizer] dibuat ulang setiap kali analyzer dipasang, jadi tanpa ini
     * tiap penyalaan auto-capture meninggalkan satu client di memori native.
     */
    fun close() {
        triggered = true
        recognizer.close()
    }

    private companion object {
        const val NIK_LENGTH = 16
    }
}
