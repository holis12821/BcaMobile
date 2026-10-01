package id.bca.bcamobile.ui.screen.buka_rekening.video_call

/** Event layar Video Call eKYC. */
sealed interface VideoCallEvent {

    /**
     * Panggilan selesai. Hasil verifikasinya ditulis CS lewat endpoint sisi mereka;
     * client hanya menandai selesai lalu menanyakan langkah berikutnya ke server.
     */
    data class VideoCallCompleted(val csName: String) : VideoCallEvent
}
