package id.bca.bcamobile.data.notification.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.notification.remote.dto.MessageResponse
import id.bca.bcamobile.data.notification.remote.dto.NotificationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Notifikasi nasabah.
 * Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §7.
 */
interface NotificationApi {

    /**
     * [types] menyaring **di server**: satu nilai (`PROMO`) atau beberapa
     * dipisah koma (`PROMO,SECURITY`); urutannya tidak berpengaruh.
     *
     * **Tab "Semua" mengirim null, bukan `"ALL"`.** `ALL` bukan nilai yang
     * dikenal dan dijawab `400 VALIDATION_ERROR` berisi
     * `details.allowed_values`. Nilai sah: `TRANSACTION`, `PROMO`, `SECURITY`,
     * `SYSTEM`, `INFO`.
     *
     * Karena penyaringan pindah ke server, paginasi per tab akhirnya benar —
     * cursor wajib direset saat tab berganti.
     */
    @GET("notifications")
    suspend fun notifications(
        @Query("type") types: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): Response<ApiEnvelope<NotificationsResponse>>

    @PUT("notifications/{notification_id}/read")
    suspend fun markRead(
        @Path("notification_id") notificationId: String,
    ): Response<ApiEnvelope<MessageResponse>>

    @PUT("notifications/read-all")
    suspend fun markAllRead(): Response<ApiEnvelope<MessageResponse>>

    companion object {
        const val DEFAULT_LIMIT = 20
    }
}
