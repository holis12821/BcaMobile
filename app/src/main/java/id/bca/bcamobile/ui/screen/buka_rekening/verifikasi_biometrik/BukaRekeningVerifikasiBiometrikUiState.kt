package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import androidx.annotation.StringRes
import id.bca.bcamobile.R
import id.bca.bcamobile.core.liveness.LivenessAction
import id.bca.bcamobile.core.liveness.LivenessFailure
import id.bca.bcamobile.core.liveness.LivenessGuidance
import id.bca.bcamobile.core.liveness.LivenessPhase
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState

/**
 * Tampilan layar Verifikasi Biometrik Wajah.
 *
 * Tidak ada lagi `precisionPercent`: angkanya dulu rata-rata probabilitas mata terbuka,
 * ditampilkan ke nasabah sebagai "98% Presisi". Itu bukan ukuran presisi apa pun, dan
 * nilai bawaannya (98) tampil sebelum satu frame pun dinilai. Yang menggantikannya
 * adalah [isFaceReady] — pernyataan yang memang bisa dibuktikan frame kamera.
 */
data class VerifikasiBiometrikUiState(
    val phase: LivenessPhase = LivenessPhase.IDLE,
    /** Gerbang kualitas terpenuhi: wajah tunggal, di dalam oval, lurus, mata terbuka. */
    val isFaceReady: Boolean = false,
    @StringRes val guidanceRes: Int? = null,
    /** Instruksi gerakan yang sedang diminta, dari urutan milik server. */
    @StringRes val instructionRes: Int? = null,
    val completedSteps: Int = 0,
    val totalSteps: Int = 0,
    @StringRes val failureRes: Int? = null,
    /** Sisa masa tunggu dari server; > 0 berarti tombol coba lagi mati. */
    val cooldownSeconds: Int = 0,
    val isBlocked: Boolean = false,
) {
    val isRunning: Boolean
        get() = phase == LivenessPhase.CHALLENGE || phase == LivenessPhase.POSITIONING

    val isVerifying: Boolean
        get() = phase == LivenessPhase.CAPTURE || phase == LivenessPhase.VERIFYING

    /** Coba lagi hanya ditawarkan saat benar-benar gagal dan tidak sedang menunggu. */
    val canRetry: Boolean
        get() = phase == LivenessPhase.FAILED && cooldownSeconds == 0 && !isBlocked
}

fun BukaRekeningFlowState.toVerifikasiBiometrikUiState(): VerifikasiBiometrikUiState {
    val liveness = livenessState
    return VerifikasiBiometrikUiState(
        phase = liveness.phase,
        isFaceReady = liveness.guidance == LivenessGuidance.NONE &&
            liveness.phase != LivenessPhase.IDLE,
        guidanceRes = liveness.guidance.textRes(),
        instructionRes = when {
            liveness.phase != LivenessPhase.CHALLENGE -> null
            // Kembali ke netral adalah instruksi tersendiri, bukan ketiadaan instruksi.
            liveness.awaitingNeutral -> R.string.buka_rekening_biometrik_action_neutral
            else -> liveness.currentAction?.textRes()
        },
        completedSteps = liveness.completedSteps,
        totalSteps = liveness.totalSteps,
        failureRes = liveness.failure?.textRes(),
        cooldownSeconds = livenessCooldownSeconds,
        isBlocked = isLivenessBlocked,
    )
}

@StringRes
private fun LivenessGuidance.textRes(): Int? = when (this) {
    LivenessGuidance.NONE -> null
    LivenessGuidance.NO_FACE -> R.string.buka_rekening_biometrik_guide_no_face
    LivenessGuidance.MULTIPLE_FACES -> R.string.buka_rekening_biometrik_guide_multiple_faces
    LivenessGuidance.MOVE_CLOSER -> R.string.buka_rekening_biometrik_guide_move_closer
    LivenessGuidance.MOVE_AWAY -> R.string.buka_rekening_biometrik_guide_move_away
    LivenessGuidance.CENTER_FACE -> R.string.buka_rekening_biometrik_guide_center_face
    LivenessGuidance.LOOK_STRAIGHT -> R.string.buka_rekening_biometrik_guide_look_straight
    LivenessGuidance.OPEN_EYES -> R.string.buka_rekening_biometrik_guide_open_eyes
}

@StringRes
private fun LivenessAction.textRes(): Int = when (this) {
    LivenessAction.TURN_LEFT -> R.string.buka_rekening_biometrik_action_turn_left
    LivenessAction.TURN_RIGHT -> R.string.buka_rekening_biometrik_action_turn_right
    LivenessAction.LOOK_UP -> R.string.buka_rekening_biometrik_action_look_up
    LivenessAction.LOOK_DOWN -> R.string.buka_rekening_biometrik_action_look_down
    LivenessAction.BLINK -> R.string.buka_rekening_biometrik_action_blink
}

@StringRes
private fun LivenessFailure.textRes(): Int = when (this) {
    LivenessFailure.STEP_TIMEOUT -> R.string.buka_rekening_biometrik_fail_step_timeout
    LivenessFailure.TOTAL_TIMEOUT -> R.string.buka_rekening_biometrik_fail_total_timeout
    LivenessFailure.WRONG_MOVE -> R.string.buka_rekening_biometrik_fail_wrong_move
    LivenessFailure.SERVER_REJECTED -> R.string.buka_rekening_biometrik_fail_server
    LivenessFailure.EVIDENCE_INCOMPLETE -> R.string.buka_rekening_biometrik_fail_incomplete
}
