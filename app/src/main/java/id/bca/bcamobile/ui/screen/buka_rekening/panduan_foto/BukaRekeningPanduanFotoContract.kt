package id.bca.bcamobile.ui.screen.buka_rekening.panduan_foto

/**
 * Layar Panduan Foto belum memancarkan event apa pun ke ViewModel: tombolnya
 * hanya membuka layar kamera, dan itu navigasi — urusan graph, bukan state.
 *
 * Kontraknya tetap ada sebagai satu tempat kalau nanti ada event (mis. "jangan
 * tampilkan panduan ini lagi"), supaya tidak ada yang menambahkannya ke kontrak
 * layar lain.
 */
sealed interface PanduanFotoEvent {

    /** Membuang sisa pesan error dari langkah sebelumnya. */
    data object ErrorDismissed : PanduanFotoEvent
}
