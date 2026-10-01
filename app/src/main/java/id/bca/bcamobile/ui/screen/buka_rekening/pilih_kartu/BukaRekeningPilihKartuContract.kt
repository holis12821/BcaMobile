package id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu

/** Event layar Pilih Kartu Paspor. */
sealed interface PilihKartuEvent {

    /** Layar terbuka; katalog ditarik untuk produk yang dipilih. */
    data object CardCatalogRequested : PilihKartuEvent

    data class CardTypeSelected(val index: Int) : PilihKartuEvent

    /** Tombol lanjut. */
    data object CardConfirmed : PilihKartuEvent

    data object ErrorDismissed : PilihKartuEvent
}
