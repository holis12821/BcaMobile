package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

/** Event layar Pilih Jenis Rekening — pintu masuk flow. */
sealed interface PilihJenisEvent {

    data class ProductSelected(val index: Int) : PilihJenisEvent

    /** Melanjutkan pendaftaran yang tertinggal dari sesi sebelumnya. */
    data object DraftResumeRequested : PilihJenisEvent

    /** Keluar dari flow: sesi di server dibatalkan dan state lokal dibuang. */
    data object FlowAbandoned : PilihJenisEvent

    data object ErrorDismissed : PilihJenisEvent
}
