package id.bca.bcamobile.domain.notification

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.notification.model.NotificationPage
import id.bca.bcamobile.domain.notification.model.NotificationType

/**
 * Notifikasi nasabah.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §7.
 */
interface NotificationRepository {

    /**
     * [cursor] null berarti halaman pertama; halaman berikutnya dari
     * `pagination.cursor`.
     *
     * [types] kosong berarti **semua jenis** — jangan mengirim `ALL`.
     * Penyaringannya terjadi di server, jadi cursor lama milik filter lama:
     * setiap pergantian filter mulai dari halaman pertama.
     */
    suspend fun notifications(
        types: Set<NotificationType> = emptySet(),
        cursor: String? = null,
    ): DataResult<NotificationPage>

    suspend fun markRead(notificationId: String): DataResult<Unit>

    suspend fun markAllRead(): DataResult<Unit>
}
