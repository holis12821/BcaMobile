package id.bca.bcamobile.core.liveness

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Menjembatani aliran frame CameraX dan ML Kit ke [LivenessStateMachine].
 *
 * Tanggung jawabnya sengaja sempit: mengubah `Face` jadi [FaceSignals], menyuapkannya
 * ke mesin status, dan menyimpan frame bukti **di memori** saat mesin memintanya.
 * Tidak ada aturan deteksi di sini — semuanya ada di mesin status, supaya bisa diuji
 * tanpa kamera.
 *
 * Yang juga tidak ada di sini: keputusan lulus. Analyzer hanya melaporkan bahwa frame
 * bukti sudah lengkap lewat [onReadyToSubmit]; yang menilainya adalah server.
 */
class LivenessFaceAnalyzer(
    private val machine: LivenessStateMachine,
    private val scope: CoroutineScope,
    private val config: LivenessConfig = LivenessConfig(),
    private val onState: (LivenessState) -> Unit,
    private val onChallengeNeeded: () -> Unit,
    private val onReadyToSubmit: (List<LivenessFrame>) -> Unit,
) : ImageAnalysis.Analyzer {

    private val buffer = LivenessFrameBuffer()

    @Volatile
    private var detector: FaceDetector? = null

    @Volatile
    private var busy = false

    @Volatile
    private var closed = false

    @Volatile
    private var submitted = false

    override fun analyze(image: ImageProxy) {
        // Frame yang datang saat detektor masih sibuk dibuang, bukan diantre: antrean
        // membuat mesin status menilai pose yang sudah lewat beberapa ratus milidetik.
        if (busy || closed) {
            image.close()
            return
        }
        busy = true

        scope.launch(Dispatchers.Default) {
            val update = process(image)
            if (update != null) dispatch(update, image)
        }.invokeOnCompletion {
            // Wajib di sini, bukan di `finally` dalam badan coroutine: kalau scope
            // sudah dibatalkan, badan coroutine tidak pernah berjalan sama sekali dan
            // ImageProxy yang tidak ditutup menghabiskan buffer ImageReader sampai
            // pipeline kamera berhenti total.
            image.close()
            busy = false
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private suspend fun process(image: ImageProxy): LivenessUpdate? {
        val mediaImage = image.image ?: return null
        val rotation = image.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        val faces = detect(input) ?: return null

        val signals = faces.toSignals(
            timestampMillis = System.currentTimeMillis(),
            frameWidth = input.width,
            frameHeight = input.height,
        )

        val update = machine.onFrame(signals)
        LivenessDebugLog.frame(signals, update.state, config)
        return update
    }

    private fun dispatch(update: LivenessUpdate, image: ImageProxy) {
        if (update.discardFrames) buffer.clear()

        update.capture?.let { slot ->
            // Frame netral menandai satu tantangan BARU dimulai, dan itu satu-satunya
            // titik yang pasti dilewati setiap percobaan. Membersihkan di sini
            // memperbaiki dua hal sekaligus:
            //
            // 1. `submitted` dulu hanya diset sekali seumur hidup analyzer. Analyzer
            //    hidup selama controller kamera tidak berubah — jadi melewati percobaan
            //    ulang — sehingga percobaan kedua yang berhasil TIDAK PERNAH dikirim.
            // 2. Slot langkah memuat aksinya, jadi `Step(0, BLINK)` dan
            //    `Step(0, TURN_LEFT)` adalah slot berbeda. Percobaan ulang dengan
            //    urutan aksi yang baru menumpuk di atas frame lama alih-alih
            //    menimpanya, dan frame percobaan sebelumnya ikut terkirim.
            //
            // `retry()` tidak melewati analyzer sama sekali — ViewModel memanggilnya
            // langsung — jadi `discardFrames` di atas tidak menolong untuk kasus itu.
            if (slot is CaptureSlot.Neutral) {
                buffer.clear()
                submitted = false
            }

            // Encoding JPEG hanya dilakukan untuk frame yang benar-benar diminta —
            // mengubah setiap frame jadi bitmap membuat analisa jatuh di bawah 15 fps.
            jpegOf(image)?.let { buffer.put(slot, it, System.currentTimeMillis()) }
        }

        onState(update.state)

        if (update.needsNewChallenge) onChallengeNeeded()

        if (update.state.phase == LivenessPhase.CAPTURE && !submitted) {
            submitted = true
            onReadyToSubmit(buffer.snapshot())
        }
    }

    private suspend fun detect(image: InputImage): List<Face>? =
        suspendCancellableCoroutine { continuation ->
            detector().process(image)
                .addOnSuccessListener { continuation.resume(it) }
                // Kegagalan satu frame bukan kegagalan sesi; frame itu saja dilewati.
                .addOnFailureListener { continuation.resume(null) }
        }

    private fun detector(): FaceDetector =
        detector ?: synchronized(this) {
            detector ?: FaceDetection.getClient(options()).also { detector = it }
        }

    /**
     * `enableTracking` adalah yang membuat kesinambungan orang bisa dijaga —
     * tanpa trackingId, wajah bisa ditukar di tengah tantangan tanpa terdeteksi.
     * `CLASSIFICATION_MODE_ALL` wajib: tanpanya probabilitas mata bernilai null dan
     * kedipan tidak akan pernah bisa dinilai.
     */
    private fun options(): FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(MIN_FACE_SIZE)
        .enableTracking()
        .build()

    private fun jpegOf(image: ImageProxy): ByteArray? = runCatching {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val oriented = if (rotation == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        ByteArrayOutputStream().use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, FRAME_QUALITY, out)
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    /**
     * Melepas detektor dan **menimpa** frame bukti yang masih tersisa.
     *
     * Dipanggil dari `onDispose` layar. Frame yang sudah diserahkan ke pemanggil ikut
     * dibersihkan di sana setelah unggahan — tidak ada jalur di mana piksel wajah
     * tertinggal hidup setelah layar ditutup.
     */
    fun close() {
        closed = true
        buffer.clear()
        synchronized(this) {
            runCatching { detector?.close() }
            detector = null
        }
    }

    private companion object {
        /**
         * Lebar wajah minimum yang masih dicari ML Kit, rasio terhadap lebar frame.
         *
         * **Harus jauh di bawah** `LivenessConfig.minFaceWidthRatio` (0.35). Nilainya
         * dulu 0.3 — hanya 0.05 di bawah ambang gerbang — sehingga wajah yang masih
         * terlalu jauh tidak terdeteksi **sama sekali**, dan panduan berbunyi "wajah
         * tidak terdeteksi" tepat di saat yang benar adalah "dekatkan wajah". Nasabah
         * lalu tidak punya petunjuk arah koreksi.
         *
         * Dengan 0.15 wajah sudah terdeteksi sejak jauh, gerbang menolaknya lewat
         * `MOVE_CLOSER`, dan ambangnya sendiri tidak berubah — yang berubah hanya
         * sejak kapan aplikasi bisa memandu.
         */
        const val MIN_FACE_SIZE = 0.15f
        const val FRAME_QUALITY = 85
    }
}

/**
 * Memetakan hasil ML Kit ke pengamatan bebas-ML Kit.
 *
 * Kotak wajah dinormalisasi terhadap ukuran [InputImage] — bukan terhadap ukuran
 * `ImageProxy` — karena ML Kit melaporkan kotak dalam koordinat gambar yang **sudah**
 * diputar sesuai `rotationDegrees`.
 */
internal fun List<Face>.toSignals(
    timestampMillis: Long,
    frameWidth: Int,
    frameHeight: Int,
): FaceSignals {
    val face = singleOrNull()
    if (face == null || frameWidth <= 0 || frameHeight <= 0) {
        return FaceSignals(
            timestampMillis = timestampMillis,
            faceCount = size,
            trackingId = null,
            yawDegrees = 0f,
            pitchDegrees = 0f,
            rollDegrees = 0f,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            faceWidthRatio = 0f,
            faceCenterX = 0f,
            faceCenterY = 0f,
        )
    }

    val box = face.boundingBox
    return FaceSignals(
        timestampMillis = timestampMillis,
        faceCount = 1,
        trackingId = face.trackingId,
        yawDegrees = face.headEulerAngleY,
        pitchDegrees = face.headEulerAngleX,
        rollDegrees = face.headEulerAngleZ,
        leftEyeOpenProbability = face.leftEyeOpenProbability,
        rightEyeOpenProbability = face.rightEyeOpenProbability,
        faceWidthRatio = box.width().toFloat() / frameWidth,
        faceCenterX = box.exactCenterX() / frameWidth,
        faceCenterY = box.exactCenterY() / frameHeight,
    )
}
