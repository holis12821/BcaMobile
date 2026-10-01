package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

/** Event layar Hasil Foto & OCR e-KTP. */
sealed interface HasilFotoEvent {

    /** Foto ditolak pengguna atau akurasi terlalu rendah; berkas cache ikut dibuang. */
    data object PhotoDiscarded : HasilFotoEvent

    /** Dipakai saat resume: hasil OCR sudah ada di server, tinggal diambil. */
    data object OcrResultRequested : HasilFotoEvent

    /** Tombol "Gunakan Foto". */
    data object OcrConfirmed : HasilFotoEvent
}
