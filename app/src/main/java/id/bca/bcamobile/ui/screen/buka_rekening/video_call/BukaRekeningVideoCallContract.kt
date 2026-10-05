package id.bca.bcamobile.ui.screen.buka_rekening.video_call

/** Event layar Video Call eKYC. */
sealed interface VideoCallEvent {

    /**
     * Status izin kamera dan mikrofon berubah — dan inilah satu-satunya pemicu panggilan.
     *
     * Satu event melayani tiga keadaan yang dulu tidak tertangani sama sekali: masuk
     * pertama kali, pulih setelah proses mati (yang memulihkan langsung ke layar ini dan
     * melewati gerbang di layar Antrean), dan pencabutan izin di tengah panggilan.
     *
     * [allowBluetooth] bukan prasyarat: `false` hanya membuat audio tetap di speaker.
     */
    data class MediaPermissionsChanged(
        val granted: Boolean,
        val allowBluetooth: Boolean,
    ) : VideoCallEvent

    /** Nasabah menekan tombol bisu. Track audio lokal dimatikan **dan** agent diberi tahu. */
    data object MuteToggled : VideoCallEvent

    data object CameraSwitched : VideoCallEvent

    /**
     * Nasabah menutup panggilan dari sisinya.
     *
     * Hasil verifikasinya tetap ditulis CS lewat endpoint sisi mereka (`§5c`); client hanya
     * menutup media lalu menanyakan langkah berikutnya ke server.
     */
    data object EndCallRequested : VideoCallEvent

    /** Tombol pada keadaan gagal: kembali ke antrean untuk mengambil tiket baru. */
    data object RejoinRequested : VideoCallEvent
}
