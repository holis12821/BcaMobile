package id.bca.bcamobile.domain.notification.model

/**
 * Jenis notifikasi menentukan ikon dan tujuan `deepLink` saat dibuka.
 * Nilai yang tidak dikenal jatuh ke [UNKNOWN] supaya versi server yang lebih baru
 * tidak menjatuhkan daftar.
 */
enum class NotificationType(val wireValue: String) {
    TRANSACTION("TRANSACTION"),
    PROMO("PROMO"),
    SECURITY("SECURITY"),

    /**
     * Ada di daftar tertutup server (`account.NotificationTypes`) tapi dulu
     * tidak dikenal client, jadi notifikasi sistem jatuh ke [UNKNOWN] dan
     * hilang dari setiap tab.
     */
    SYSTEM("SYSTEM"),
    INFO("INFO"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromWire(value: String?): NotificationType =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: UNKNOWN
    }
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    /** `bcamobile://…` dari server; client hanya meneruskan, tidak merakit sendiri. */
    val deepLink: String?,
    val createdAt: String,
)

/**
 * Satu halaman notifikasi.
 *
 * [unreadCount] datang dari server dan tidak dihitung ulang di client — nilai yang
 * sama juga dipakai lencana di Beranda, jadi keduanya tidak boleh berbeda sumber.
 */
data class NotificationPage(
    val unreadCount: Int,
    val items: List<AppNotification>,
    val nextCursor: String?,
)
