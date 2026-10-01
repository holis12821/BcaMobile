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

    data object FlashModeToggled : KameraFotoEvent

    data class AutoCaptureToggled(val enabled: Boolean) : KameraFotoEvent
}
