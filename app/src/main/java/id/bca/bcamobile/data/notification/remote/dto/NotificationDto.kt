package id.bca.bcamobile.data.notification.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("deep_link") val deepLink: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class NotificationsResponse(
    /**
     * **Tidak pernah dikirim `GET /notifications`.** `NotificationListResponse`
     * di backend hanya memuat `notifications`; hitungan belum dibaca yang
     * dilayani server ada di `GET /account/dashboard`
     * (`unread_notifications`), yang dipakai lencana Beranda. Field ini
     * dipertahankan supaya tetap terbaca kalau backend menambahkannya, tapi
     * layar Notifikasi tidak boleh bergantung padanya.
     */
    @SerialName("unread_count") val unreadCount: Int = 0,
    val notifications: List<NotificationDto> = emptyList(),
)

@Serializable
data class MessageResponse(
    val message: String = "",
)
