package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_biometrik

import id.bca.bcamobile.core.liveness.LivenessFrame
import id.bca.bcamobile.core.liveness.LivenessState

/**
 * Event layar Verifikasi Biometrik Wajah.
 *
 * `LivenessStarted` sudah tidak ada: tidak ada lagi tombol yang memulai perekaman.
 * Tantangan dimulai sendiri begitu gerbang kualitas wajah bertahan — tombol Mulai
 * hanya menunda pekerjaan yang sudah bisa diputuskan dari frame kamera.
 */
sealed interface VerifikasiBiometrikEvent {

    /** Kamera sudah terikat dan frame mulai mengalir. */
    data object CameraReady : VerifikasiBiometrikEvent

    /** Mesin status melaporkan keadaan baru setelah menilai satu frame. */
    data class LivenessStateChanged(val state: LivenessState) : VerifikasiBiometrikEvent

    /** Mesin status meminta tantangan baru; hanya server yang boleh menerbitkannya. */
    data object ChallengeNeeded : VerifikasiBiometrikEvent

    /** Frame bukti lengkap. Belum berarti lulus — server yang menilai. */
    data class FramesReady(val frames: List<LivenessFrame>) : VerifikasiBiometrikEvent

    /** Nasabah menekan Coba Lagi setelah satu percobaan gagal. */
    data object RetryRequested : VerifikasiBiometrikEvent
}
