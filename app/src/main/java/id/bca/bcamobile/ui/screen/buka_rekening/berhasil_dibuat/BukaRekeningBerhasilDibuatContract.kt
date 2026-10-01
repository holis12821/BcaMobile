package id.bca.bcamobile.ui.screen.buka_rekening.berhasil_dibuat

/**
 * Layar terakhir flow. Rekening sudah jadi dan sesi sudah dibersihkan, jadi tidak
 * ada lagi event yang mengubah sesi — yang tersisa hanya aksi tampilan.
 */
sealed interface BerhasilDibuatEvent {

    /** Nomor rekening disalin; ViewModel hanya memunculkan pesan konfirmasi. */
    data class NomorRekeningDisalin(val nomor: String) : BerhasilDibuatEvent
}
