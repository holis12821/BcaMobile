package id.bca.bcamobile.core.qris

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Pembaca QR di perangkat.
 *
 * Payload QRIS **tidak** diurai di client: yang dikirim ke `POST /qris/decode`
 * adalah teks mentah hasil pemindaian. Nama merchant, nominal, dan masa berlaku
 * semuanya ditentukan server.
 *
 * Hanya format QR_CODE yang diminta — barcode ritel tidak relevan dan
 * mempersempit format membuat deteksi lebih cepat.
 */
@Singleton
class QrisScanner @Inject constructor() {

    @Volatile
    private var scanner: BarcodeScanner? = null

    private fun scanner(): BarcodeScanner =
        scanner ?: synchronized(this) {
            scanner ?: BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build(),
            ).also { scanner = it }
        }

    /** Membaca QR dari gambar di galeri; null bila tidak ada QR yang terbaca. */
    suspend fun scan(context: Context, uri: Uri): String? {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull() ?: return null
        return process(image)
    }

    private suspend fun process(image: InputImage): String? =
        suspendCancellableCoroutine { continuation ->
            scanner().process(image)
                .addOnSuccessListener { barcodes ->
                    continuation.resume(barcodes.firstNotNullOfOrNull { it.rawValue })
                }
                .addOnFailureListener { continuation.resume(null) }
        }

    /**
     * Melepas client ML Kit beserta memori native-nya. Wajib dipanggil pada
     * instance yang dibuat manual, mis. milik [QrisScanAnalyzer].
     */
    fun close() {
        synchronized(this) {
            runCatching { scanner?.close() }
            scanner = null
        }
    }

    internal fun client(): BarcodeScanner = scanner()
}

/**
 * Analyzer CameraX yang memanggil [onQrScanned] sekali saat QR pertama terbaca.
 *
 * Satu frame diproses pada satu waktu dan sisanya dibuang, supaya preview tetap
 * mulus. [ImageProxy] selalu ditutup — buffer yang tidak dilepas menghentikan
 * pipeline kamera.
 */
class QrisScanAnalyzer(
    private val scanner: QrisScanner,
    private val onQrScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var busy = false

    @Volatile
    private var triggered = false

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        if (busy || triggered) {
            image.close()
            return
        }
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        busy = true

        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        scanner.client().process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (value != null && !triggered) {
                    triggered = true
                    onQrScanned(value)
                }
            }
            .addOnCompleteListener {
                image.close()
                busy = false
            }
    }

    /** Dipakai saat nasabah kembali ke pemindai setelah QR sebelumnya ditolak server. */
    fun reset() {
        triggered = false
    }
}
