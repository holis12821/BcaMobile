package id.bca.bcamobile.ui.screen.buka_rekening.video_call

/**
 * Tahap panggilan yang perilaku layarnya berbeda.
 *
 * Dulu hanya ada `statusTerhubung: Boolean` yang nilainya `true` secara hardcode, jadi
 * layar selalu mengaku "Terhubung" di atas area hitam yang tidak tersambung ke apa pun.
 * Boolean tidak cukup: menunggu petugas, menegosiasi, dan putus sementara adalah tiga
 * keadaan berbeda yang tidak boleh terlihat sama bagi nasabah.
 */
enum class VideoCallConnectionState {
    /**
     * Kamera atau mikrofon belum diizinkan, jadi panggilan belum boleh dimulai sama sekali.
     *
     * Keadaan pertama yang mungkin, bukan keadaan kesalahan: layar ini bisa dicapai ulang
     * setelah proses mati, dan izin bisa sudah dicabut OS di sela itu.
     */
    IZIN_DIBUTUHKAN,

    /** Socket signaling hidup, antrean masih berjalan — belum ada petugas. */
    MENUNGGU_PETUGAS,

    /** Petugas ditugaskan; SDP dan ICE sedang dipertukarkan. */
    MENGHUBUNGKAN,

    /** Media mengalir dua arah. */
    TERHUBUNG,

    /** Sambungan putus sementara dan sedang disambung ulang. */
    MENYAMBUNG_ULANG,

    /** Tidak bisa dipulihkan sendiri; nasabah perlu bergabung ke antrean lagi. */
    GAGAL,

    /** Panggilan diakhiri petugas. */
    SELESAI,
}

data class VideoCallUiState(
    // Awalnya izin, bukan menunggu petugas: tidak ada yang boleh berjalan sebelum kamera
    // dan mikrofon dipastikan ada.
    val connectionState: VideoCallConnectionState = VideoCallConnectionState.IZIN_DIBUTUHKAN,
    val namaPetugas: String = "",
    val idPetugas: String = "",
    val durasiPanggilan: String = "00:00",
    val isMuted: Boolean = false,
    val isFrontCamera: Boolean = true,
    /** Instruksi terakhir dari petugas; kosong berarti belum ada. */
    val instruksiPetugas: String = "",
    val error: String? = null,
) {
    /** Timer hanya berarti setelah media mengalir. */
    val isDurasiTampil: Boolean get() = connectionState == VideoCallConnectionState.TERHUBUNG

    /**
     * Kontrol mati sebelum ada panggilan: mute dan ganti kamera di keadaan menunggu hanya
     * mengirim `media_control` ke ruang yang belum berisi siapa pun.
     */
    val isKontrolAktif: Boolean
        get() = connectionState == VideoCallConnectionState.TERHUBUNG ||
            connectionState == VideoCallConnectionState.MENGHUBUNGKAN

    /** Keadaan yang hanya bisa dipulihkan dengan bergabung ke antrean lagi. */
    val isPerluAntreanUlang: Boolean
        get() = connectionState == VideoCallConnectionState.GAGAL

    /** Keadaan yang pulih dengan memberi izin, bukan dengan antrean baru. */
    val isPerluIzin: Boolean
        get() = connectionState == VideoCallConnectionState.IZIN_DIBUTUHKAN
}
