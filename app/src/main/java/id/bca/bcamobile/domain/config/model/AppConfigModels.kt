package id.bca.bcamobile.domain.config.model

/**
 * Konfigurasi aplikasi dari `GET /health`, dibaca sekali saat splash.
 *
 * Dua hal di sini memblokir pemakaian aplikasi — [maintenanceMode] dan
 * [forceUpdate] — dan keduanya keputusan server. Client tidak boleh menawarkan
 * jalan lain saat salah satunya menyala.
 */
data class AppConfig(
    val isServiceHealthy: Boolean,
    val maintenanceMode: Boolean,
    /** Pesan pemeliharaan dari server; dipakai apa adanya karena sudah berbahasa Indonesia. */
    val maintenanceMessage: String?,
    val minimumAppVersion: String,
    val forceUpdate: Boolean,
    val featureFlags: FeatureFlags,
) {
    companion object {
        /**
         * Nilai saat konfigurasi tidak bisa diambil (mis. jaringan mati).
         *
         * Semua flag menyala dan tidak ada blokir: kegagalan membaca config bukan
         * alasan mengunci nasabah keluar dari aplikasi, dan endpoint sebenarnya
         * tetap menolak sendiri kalau fiturnya memang dimatikan.
         */
        val Permissive = AppConfig(
            isServiceHealthy = true,
            maintenanceMode = false,
            maintenanceMessage = null,
            minimumAppVersion = "",
            forceUpdate = false,
            featureFlags = FeatureFlags(),
        )
    }
}

/** Saklar fitur per lingkungan; menyembunyikan menu yang server memang tolak. */
data class FeatureFlags(
    val eWalletEnabled: Boolean = true,
    val qrisEnabled: Boolean = true,
    val interbankTransferEnabled: Boolean = true,
    val virtualAccountEnabled: Boolean = true,
)
