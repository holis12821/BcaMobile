package id.bca.bcamobile.data.notification

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.notification.remote.NotificationApi
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.notification.NotificationRepository
import id.bca.bcamobile.domain.notification.model.AppNotification
import id.bca.bcamobile.domain.notification.model.NotificationPage
import id.bca.bcamobile.domain.notification.model.NotificationType
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApi,
    private val caller: ApiCaller,
) : NotificationRepository {

    override suspend fun notifications(
        types: Set<NotificationType>,
        cursor: String?,
    ): DataResult<NotificationPage> {
        // Cursor tinggal di envelope, bukan di `data`, jadi respons mentah dibaca dua kali.
        var nextCursor: String? = null
        val result = caller.call {
            api.notifications(
                // UNKNOWN tidak pernah dikirim: ia penanda lokal untuk jenis
                // yang belum dikenal build ini, bukan nilai yang dikenal server.
                types = types
                    .filter { it != NotificationType.UNKNOWN }
                    .joinToString(",") { it.wireValue }
                    .takeIf { it.isNotEmpty() },
                cursor = cursor,
            ).also { nextCursor = it.nextCursor() }
        }
        return result.map { dto ->
            NotificationPage(
                unreadCount = dto.unreadCount,
                items = dto.notifications.map {
                    AppNotification(
                        id = it.id,
                        type = NotificationType.fromWire(it.type),
                        title = it.title,
                        body = it.body,
                        isRead = it.isRead,
                        deepLink = it.deepLink?.takeIf(String::isNotBlank),
                        createdAt = it.createdAt,
                    )
                },
                nextCursor = nextCursor,
            )
        }
    }

    override suspend fun markRead(notificationId: String): DataResult<Unit> =
        caller.call { api.markRead(notificationId) }.map { }

    override suspend fun markAllRead(): DataResult<Unit> =
        caller.call { api.markAllRead() }.map { }
}

private fun <T> Response<ApiEnvelope<T>>.nextCursor(): String? =
    body()?.pagination?.takeIf { it.hasMore }?.cursor
