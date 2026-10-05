package id.bca.bcamobile.ui.screen.buka_rekening.syarat_ketentuan

/** Event yang bisa dipancarkan layar Syarat & Ketentuan. */
sealed interface SyaratKetentuanEvent {

    /**
     * Nasabah mencentang S&K lalu menekan lanjut.
     *
     * Sesi onboarding lahir di sini: server butuh versi S&K yang disetujui, dan
     * kartu yang sudah dipilih ikut terkirim pada request yang sama.
     */
    data object TncAccepted : SyaratKetentuanEvent

    /**
     * Muat ulang teks S&K — tombol "Coba Lagi" pada keadaan gagal.
     *
     * Muatan pertama terjadi di `init`, bukan lewat event: nasabah harus bisa membaca
     * teksnya sebelum menyetujui, dan memuatnya saat tombol ditekan berarti dia
     * menyetujui teks yang belum diunduh.
     */
    data object TncReloadRequested : SyaratKetentuanEvent
}
