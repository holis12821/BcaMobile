package id.bca.bcamobile.ui.screen.buka_rekening.kamera_foto

import java.io.File

/** Event layar Kamera Foto e-KTP. */
sealed interface KameraFotoEvent {

    data class KtpPhotoCaptured(
        val photo: File,
        val flashUsed: Boolean,
        val autoCaptured: Boolean,
        val resolution: String,
    ) : KameraFotoEvent

    /**
     * Gambar dari galeri tidak bisa dibaca jadi berkas foto.
     *
     * Dilaporkan sebagai event, bukan diabaikan: kegagalan yang diam membuat
     * nasabah menekan "Dari Galeri", memilih gambar, dan kembali ke layar yang
     * tidak berubah tanpa tahu apa yang terjadi.
     */
    data object GalleryImportFailed : KameraFotoEvent

    data object FlashModeToggled : KameraFotoEvent

    data class AutoCaptureToggled(val enabled: Boolean) : KameraFotoEvent
}
