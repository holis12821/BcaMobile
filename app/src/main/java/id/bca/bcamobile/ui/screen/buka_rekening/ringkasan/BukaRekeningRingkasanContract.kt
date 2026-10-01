package id.bca.bcamobile.ui.screen.buka_rekening.ringkasan

/** Event layar Ringkasan & Konfirmasi. */
sealed interface RingkasanEvent {

    data class AgreementToggled(val agreed: Boolean) : RingkasanEvent

    /** Submit final; dikunci idempotency key supaya retry tidak membuat rekening kedua. */
    data object ApplicationSubmitted : RingkasanEvent

    data object DraftSaveRequested : RingkasanEvent
}
