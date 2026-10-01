package id.bca.bcamobile.session

/**
 * Gerbang aplikasi berdasarkan `GET /health` (`01-API-SPECIFICATION.md` §1).
 *
 * Dua keadaan di sini memblokir seluruh aplikasi dan keduanya keputusan server —
 * client tidak menyediakan jalan lain saat salah satunya menyala. Kegagalan
 * membaca config **bukan** blokir: [Open] tetap dipakai, dan endpoint sebenarnya
 * yang menolak kalau fiturnya memang mati.
 */
sealed interface AppGate {

    /** Config belum terbaca; splash masih tampil. */
    data object Checking : AppGate

    data object Open : AppGate

    /** `maintenance_mode` menyala. [message] dari server bila ada. */
    data class Maintenance(val message: String?) : AppGate

    /** `force_update` menyala; versi minimum dari server. */
    data class UpdateRequired(val minimumVersion: String) : AppGate
}
