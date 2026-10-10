package id.bca.bcamobile.core.liveness

import android.util.Log
import id.bca.bcamobile.BuildConfig

/**
 * Jejak per-frame untuk mengkalibrasi tanda sumbu di perangkat fisik (keputusan Q7).
 *
 * Isinya **hanya angka**: sudut, probabilitas mata, trackingId, dan status mesin.
 * Tidak ada gambar, tidak ada potongan frame, tidak ada identitas — yang dicatat
 * tidak cukup untuk merekonstruksi wajah siapa pun.
 *
 * Seluruh badan fungsi dijaga `BuildConfig.DEBUG`, yang nilainya konstan saat
 * kompilasi, sehingga R8 membuang pemanggilan ini dari build release. Verifikasinya
 * ada di daftar periksa kalibrasi: build release lalu pastikan tag di bawah tidak
 * pernah muncul di logcat.
 */
object LivenessDebugLog {

    const val TAG = "LivenessCalibration"

    fun frame(signals: FaceSignals, state: LivenessState, config: LivenessConfig) {
        if (!BuildConfig.DEBUG) return
        if (signals.faceCount != 1) {
            Log.d(TAG, "faces=${signals.faceCount} phase=${state.phase}")
            return
        }
        val face = signals.normalized(config.calibration)
        Log.d(
            TAG,
            "raw[yaw=%.1f pitch=%.1f roll=%.1f] user[yaw=%.1f pitch=%.1f] eye=%.2f ".format(
                signals.yawDegrees,
                signals.pitchDegrees,
                signals.rollDegrees,
                face.userYawDegrees,
                face.userPitchDegrees,
                face.eyeOpenProbability ?: -1f,
            ) +
                "box[w=%.2f cx=%.2f cy=%.2f] track=%s ".format(
                    signals.faceWidthRatio,
                    face.faceCenterX,
                    face.faceCenterY,
                    signals.trackingId?.toString() ?: "-",
                ) +
                "phase=${state.phase} expect=${state.currentAction ?: "-"} " +
                "step=${state.completedSteps}/${state.totalSteps} " +
                "neutral=${state.awaitingNeutral} guide=${state.guidance} " +
                "pose=${face.satisfiedHeadPose(config)}",
        )
    }
}
