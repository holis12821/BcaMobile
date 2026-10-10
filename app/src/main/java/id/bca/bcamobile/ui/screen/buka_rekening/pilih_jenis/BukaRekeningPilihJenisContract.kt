package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import id.bca.bcamobile.domain.onboarding.model.ProductType

/** Event layar Pilih Jenis Rekening — pintu masuk flow. */
sealed interface PilihJenisEvent {

    /** Katalog diminta saat layar dibuka; endpoint-nya tidak butuh sesi. */
    data object ProductCatalogRequested : PilihJenisEvent

    /**
     * Nasabah menekan "Muat ulang" pada penanda daftar bawaan.
     *
     * Event sendiri, bukan [ProductCatalogRequested] lagi, karena jalurnya berbeda dalam
     * satu hal yang penting: ia **tidak** menyentuh `isLoading` bersama. Spinner layar
     * penuh akan mengosongkan daftar yang sedang dibaca nasabah — persis layar yang mau
     * diperbaiki.
     */
    data object ProductCatalogReloadRequested : PilihJenisEvent

    /**
     * Membawa **kode** produk, bukan indeks baris.
     *
     * Indeks pernah dipakai di sini dan dipetakan ke ordinal [ProductType]. Urutan tampil
     * datang dari `display_order` server, jadi satu perubahan urutan membuat pilihan
     * nasabah menunjuk produk lain tanpa error di mana pun.
     */
    data class ProductSelected(val type: ProductType) : PilihJenisEvent

    /** Melanjutkan pendaftaran yang tertinggal dari sesi sebelumnya. */
    data object DraftResumeRequested : PilihJenisEvent

    /** Keluar dari flow: sesi di server dibatalkan dan state lokal dibuang. */
    data object FlowAbandoned : PilihJenisEvent

    data object ErrorDismissed : PilihJenisEvent
}
