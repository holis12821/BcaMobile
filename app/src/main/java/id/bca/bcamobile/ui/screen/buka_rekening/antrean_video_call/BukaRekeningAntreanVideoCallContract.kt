package id.bca.bcamobile.ui.screen.buka_rekening.antrean_video_call

/** Event layar Antrean Video Call. */
sealed interface AntreanVideoCallEvent {

    /** Layar terbuka dan sesi sudah ada; ambil nomor antrean. */
    data object QueueJoinRequested : AntreanVideoCallEvent
}
