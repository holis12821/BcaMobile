package id.bca.bcamobile.core.liveness

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Tahapan tantangan liveness, berurutan. */
enum class LivenessChallenge {
    /** Wajah harus terdeteksi dan berada di tengah bingkai. */
    FACE_CENTERED,

    /** Mata terpejam lalu terbuka lagi — ini yang membedakan orang dari foto cetak. */
    BLINK,

    /** Kepala menoleh, sebagai bukti kedua bahwa subjek bukan gambar datar. */
    HEAD_TURN,
}

data class LivenessProgress(
    val faceDetected: Boolean = false,
    val currentChallenge: LivenessChallenge = LivenessChallenge.FACE_CENTERED,
    val completedActions: Int = 0,
    val precisionPercent: Int = 0,
    val isComplete: Boolean = false,
)

/**
 * Mesin status tantangan liveness berbasis ML Kit Face Detection.
 *
 * Anti-spoofing dasar di perangkat: kedipan membuktikan mata bergerak, dan
 * perubahan sudut kepala membuktikan subjek punya kedalaman. Keduanya hanya
 * penyaring awal — keputusan akhir tetap milik backend lewat `POST /biometric`.
 */
@Singleton
class LivenessDetector @Inject constructor() {

    @Volatile
    private var detector: FaceDetector? = null

    private fun detector(): FaceDetector =
        detector ?: synchronized(this) {
            detector ?: FaceDetection.getClient(options()).also { detector = it }
        }

    private fun options(): FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(MIN_FACE_SIZE)
        .build()

    /**
     * Melepas client ML Kit beserta memori native-nya.
     *
     * Detektor wajah dibuat ulang tiap kali sesi liveness dimulai, jadi tanpa ini
     * setiap percobaan meninggalkan satu client yang menumpuk di memori native.
     */
    fun close() {
        synchronized(this) {
            runCatching { detector?.close() }
            detector = null
        }
    }

    private var eyesWereClosed = false
    private var baselineHeadAngle: Float? = null
    private var progress = LivenessProgress()

    fun reset() {
        eyesWereClosed = false
        baselineHeadAngle = null
        progress = LivenessProgress()
    }

    /** @return progres terbaru, atau progres sebelumnya bila frame tidak terpakai. */
    @OptIn(ExperimentalGetImage::class)
    suspend fun analyze(imageProxy: ImageProxy): LivenessProgress {
        val mediaImage = imageProxy.image ?: return progress
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val face = detect(image)

        if (face == null) {
            progress = progress.copy(faceDetected = false)
            return progress
        }

        val leftEye = face.leftEyeOpenProbability ?: return progress
        val rightEye = face.rightEyeOpenProbability ?: return progress
        val precision = ((leftEye + rightEye) / 2 * PERCENT).toInt()

        progress = progress.copy(faceDetected = true, precisionPercent = precision)

        when (progress.currentChallenge) {
            LivenessChallenge.FACE_CENTERED -> advance(LivenessChallenge.BLINK)

            LivenessChallenge.BLINK -> {
                val closed = leftEye < EYE_CLOSED_THRESHOLD && rightEye < EYE_CLOSED_THRESHOLD
                val open = leftEye > EYE_OPEN_THRESHOLD && rightEye > EYE_OPEN_THRESHOLD
                when {
                    closed -> eyesWereClosed = true
                    eyesWereClosed && open -> {
                        eyesWereClosed = false
                        advance(LivenessChallenge.HEAD_TURN)
                    }
                }
            }

            LivenessChallenge.HEAD_TURN -> {
                val baseline = baselineHeadAngle ?: face.headEulerAngleY.also {
                    baselineHeadAngle = it
                }
                if (kotlin.math.abs(face.headEulerAngleY - baseline) > HEAD_TURN_DEGREES) {
                    progress = progress.copy(
                        completedActions = TOTAL_CHALLENGES,
                        isComplete = true,
                    )
                }
            }
        }

        return progress
    }

    private fun advance(next: LivenessChallenge) {
        progress = progress.copy(
            currentChallenge = next,
            completedActions = progress.completedActions + 1,
        )
    }

    private suspend fun detect(image: InputImage): Face? =
        suspendCancellableCoroutine { continuation ->
            detector().process(image)
                .addOnSuccessListener { faces ->
                    // Lebih dari satu wajah berarti bingkai tidak bersih; tolak frame-nya.
                    continuation.resume(faces.singleOrNull())
                }
                .addOnFailureListener { continuation.resume(null) }
        }

    companion object {
        const val TOTAL_CHALLENGES = 3
        private const val MIN_FACE_SIZE = 0.3f
        private const val EYE_CLOSED_THRESHOLD = 0.3f
        private const val EYE_OPEN_THRESHOLD = 0.7f
        private const val HEAD_TURN_DEGREES = 15f
        private const val PERCENT = 100
    }
}
