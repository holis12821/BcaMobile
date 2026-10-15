package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

/** Event layar Hasil Foto & OCR e-KTP. */
sealed interface HasilFotoEvent {

    /** Foto ditolak pengguna atau akurasi terlalu rendah; berkas cache ikut dibuang. */
    data object PhotoDiscarded : HasilFotoEvent

    /**
     * Layar ditampilkan. ViewModel yang memutuskan apakah hasil OCR perlu diambil
     * dari server, dan menjaga agar itu terjadi **sekali** per masuk layar.
     *
     * Dulu bernama `OcrResultRequested` dan dipicu `LaunchedEffect` yang
     * dikunci ke `ocr`/`ktpPhoto`. Keduanya justru dikosongkan oleh "Ambil Ulang",
     * jadi menekan tombol itu menyalakan probe resume tepat saat nasabah keluar —
     * dan jawaban 404-nya dulu membuang seluruh sesi.
     */
    data object ScreenShown : HasilFotoEvent

    /** Tombol "Gunakan Foto". */
    data object OcrConfirmed : HasilFotoEvent
}
