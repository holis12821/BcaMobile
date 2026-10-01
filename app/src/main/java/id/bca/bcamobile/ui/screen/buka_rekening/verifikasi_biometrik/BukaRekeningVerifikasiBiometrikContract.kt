package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import id.bca.bcamobile.core.liveness.LivenessProgress
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import java.io.File

/** Event layar Verifikasi Biometrik Wajah. */
sealed interface VerifikasiBiometrikEvent {

    /** Tombol Mulai: analyzer liveness baru dipasang setelah ini. */
    data object LivenessStarted : VerifikasiBiometrikEvent

    data class LivenessProgressed(val progress: LivenessProgress) : VerifikasiBiometrikEvent

    data class BiometricCaptured(
        val facePhoto: File,
        val livenessFrames: List<File>,
        val meta: LivenessMeta,
    ) : VerifikasiBiometrikEvent
}
