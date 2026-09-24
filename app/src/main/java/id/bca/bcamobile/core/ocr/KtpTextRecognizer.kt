package id.bca.bcamobile.core.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * OCR e-KTP di perangkat.
 *
 * Seluruh pengenalan teks terjadi lokal — foto mentah tidak pernah dikirim ke
 * layanan OCR pihak ketiga. Yang berangkat ke backend hanya berkas foto lewat
 * `POST /onboarding/ocr`, untuk dicocokkan ke Dukcapil.
 */
@Singleton
class KtpTextRecognizer @Inject constructor() {

    @Volatile
    private var recognizer: TextRecognizer? = null

    private fun recognizer(): TextRecognizer =
        recognizer ?: synchronized(this) {
            recognizer ?: TextRecognition
                .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .also { recognizer = it }
        }

    suspend fun scan(bitmap: Bitmap): LocalKtpScan? {
        val text = recognize(bitmap) ?: return null
        return KtpParser.parse(text)
    }

    /**
     * Melepas client ML Kit beserta memori native-nya.
     *
     * Wajib dipanggil pada instance yang dibuat manual (mis. milik
     * [id.bca.bcamobile.core.ocr.KtpAutoCaptureAnalyzer]) — tanpa ini setiap
     * pemasangan analyzer meninggalkan satu client yang tidak pernah dilepas.
     * Instance singleton milik Hilt tidak perlu ditutup; kalaupun ditutup,
     * [recognizer] dibangun ulang pada pemakaian berikutnya.
     */
    fun close() {
        synchronized(this) {
            runCatching { recognizer?.close() }
            recognizer = null
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String? =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer().process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resume(null) }
        }

    companion object {
        /** Di bawah ambang ini, foto disarankan diulang alih-alih diunggah. */
        const val MIN_ACCEPTABLE_ACCURACY = 80.0
    }
}
