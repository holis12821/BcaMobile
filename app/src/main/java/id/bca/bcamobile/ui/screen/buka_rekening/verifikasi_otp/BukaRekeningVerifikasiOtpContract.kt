package id.bca.bcamobile.ui.screen.buka_rekening.verifikasi_otp

/** Event layar Verifikasi OTP. */
sealed interface VerifikasiOtpEvent {

    data class OtpCodeChanged(val code: String) : VerifikasiOtpEvent

    /** Kode dikirim dari state, bukan dari parameter — layar tidak menyimpan kodenya sendiri. */
    data object OtpSubmitted : VerifikasiOtpEvent

    data object OtpResendRequested : VerifikasiOtpEvent

    /** Layar ditinggalkan; kodenya dibuang dari memory. */
    data object OtpCodeCleared : VerifikasiOtpEvent
}
