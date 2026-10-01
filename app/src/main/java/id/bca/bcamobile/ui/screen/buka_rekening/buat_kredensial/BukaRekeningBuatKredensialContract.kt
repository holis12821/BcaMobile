package id.bca.bcamobile.ui.screen.buka_rekening.buat_kredensial

/** Event layar Buat Kredensial (kode akses + PIN). */
sealed interface BuatKredensialEvent {

    data class AccessCodeChanged(val value: String) : BuatKredensialEvent
    data class ConfirmAccessCodeChanged(val value: String) : BuatKredensialEvent
    data object AccessCodeVisibilityToggled : BuatKredensialEvent
    data object ConfirmAccessCodeVisibilityToggled : BuatKredensialEvent
    data class PinChanged(val value: String) : BuatKredensialEvent
    data class ConfirmPinChanged(val value: String) : BuatKredensialEvent

    /** Kredensial dienkripsi RSA-OAEP lalu dikirim; state lokalnya langsung dikosongkan. */
    data object CredentialsSubmitted : BuatKredensialEvent
}
