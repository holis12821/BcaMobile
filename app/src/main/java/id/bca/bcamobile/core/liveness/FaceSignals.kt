package id.bca.bcamobile.core.liveness

import kotlin.math.abs

/**
 * Satu pengamatan wajah dari satu frame, sudah lepas dari tipe ML Kit.
 *
 * Mesin status sengaja tidak menerima `Face` milik ML Kit supaya seluruh aturan
 * deteksi bisa diuji di JVM dengan angka sintetis — tanpa kamera, tanpa emulator,
 * tanpa Robolectric. Yang mengubah `Face` jadi bentuk ini adalah analyzer.
 *
 * Sudut di sini masih **mentah** seperti yang dilaporkan ML Kit; penyesuaian tanda
 * ke arah yang dirasakan pengguna dilakukan [normalized] memakai [LivenessCalibration].
 */
data class FaceSignals(
    val timestampMillis: Long,
    /** 0 berarti tidak ada wajah; >1 berarti bingkai tidak bersih dan harus ditolak. */
    val faceCount: Int,
    /** Null bila pelacakan tidak aktif — tanpa ini kesinambungan orang tidak bisa dijaga. */
    val trackingId: Int?,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    /** Null bila klasifikasi mati; kedipan tidak bisa dinilai tanpa keduanya. */
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    /** Lebar kotak wajah dibagi lebar frame. */
    val faceWidthRatio: Float,
    /** Pusat kotak wajah, ternormalisasi terhadap ukuran frame (0..1). */
    val faceCenterX: Float,
    val faceCenterY: Float,
) {
    val hasFace: Boolean get() = faceCount == 1

    fun normalized(calibration: LivenessCalibration): NormalizedFace = NormalizedFace(
        timestampMillis = timestampMillis,
        /** Positif = menoleh ke kiri pengguna. */
        userYawDegrees = yawDegrees * calibration.yawSignForUserLeft,
        /** Positif = menengadah. */
        userPitchDegrees = pitchDegrees * calibration.pitchSignForUp,
        rollDegrees = rollDegrees,
        eyeOpenProbability = minOfOrNull(leftEyeOpenProbability, rightEyeOpenProbability),
        faceWidthRatio = faceWidthRatio,
        faceCenterX = if (calibration.isAnalysisMirrored) 1f - faceCenterX else faceCenterX,
        faceCenterY = faceCenterY,
    )

    private fun minOfOrNull(left: Float?, right: Float?): Float? {
        if (left == null || right == null) return null
        return minOf(left, right)
    }
}

/**
 * Pengamatan yang sudah berorientasi pengguna.
 *
 * [eyeOpenProbability] adalah nilai **terkecil** dari kedua mata, bukan rata-rata:
 * tantangan menuntut *kedua* mata terbuka atau *kedua* mata terpejam, dan rata-rata
 * meloloskan satu mata terbuka satu terpejam di tengah-tengah.
 */
data class NormalizedFace(
    val timestampMillis: Long,
    val userYawDegrees: Float,
    val userPitchDegrees: Float,
    val rollDegrees: Float,
    val eyeOpenProbability: Float?,
    val faceWidthRatio: Float,
    val faceCenterX: Float,
    val faceCenterY: Float,
) {
    fun isNeutralPose(config: LivenessConfig): Boolean =
        abs(userYawDegrees) < config.neutralYawDegrees &&
            abs(userPitchDegrees) < config.neutralPitchDegrees &&
            abs(rollDegrees) < config.neutralRollDegrees

    fun areEyesOpen(config: LivenessConfig): Boolean =
        (eyeOpenProbability ?: 0f) > config.eyeOpenProbability

    fun areEyesClosed(config: LivenessConfig): Boolean =
        (eyeOpenProbability ?: 1f) < config.eyeClosedProbability

    /** Gerakan kepala apa saja yang sedang dipenuhi frame ini. */
    fun satisfiedHeadPose(config: LivenessConfig): Set<LivenessAction> = buildSet {
        if (userYawDegrees >= config.turnYawDegrees) add(LivenessAction.TURN_LEFT)
        if (userYawDegrees <= -config.turnYawDegrees) add(LivenessAction.TURN_RIGHT)
        if (userPitchDegrees >= config.lookUpPitchDegrees) add(LivenessAction.LOOK_UP)
        if (userPitchDegrees <= -config.lookDownPitchDegrees) add(LivenessAction.LOOK_DOWN)
    }
}
