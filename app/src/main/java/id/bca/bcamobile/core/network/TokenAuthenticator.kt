package id.bca.bcamobile.core.network

import id.bca.bcamobile.core.device.DeviceIdProvider
import id.bca.bcamobile.data.auth.local.TokenManager
import id.bca.bcamobile.data.auth.remote.AuthApi
import id.bca.bcamobile.data.auth.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Menyegarkan access token sekali saat server membalas 401, lalu mengulang request.
 *
 * [AuthApi] diambil lewat [Provider] untuk memutus lingkaran dependensi:
 * OkHttp butuh authenticator, authenticator butuh Retrofit, Retrofit butuh OkHttp.
 *
 * Beberapa request yang gagal bersamaan akan antre di [mutex]; yang pertama
 * menyegarkan token, sisanya memakai token baru tanpa memanggil refresh lagi.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: Provider<AuthApi>,
    private val deviceIdProvider: DeviceIdProvider,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != HTTP_UNAUTHORIZED) return null

        // Endpoint refresh berjalan di klien yang authenticator-nya kelas ini sendiri.
        // Tanpa penjagaan ini, 401 dari refresh memanggil authenticate() lagi di thread
        // OkHttp lain, yang lalu menunggu `mutex` milik runBlocking di bawah — sementara
        // runBlocking itu menunggu refresh selesai. Keduanya saling kunci permanen.
        if (response.request.url.encodedPath.endsWith(REFRESH_PATH)) {
            tokenManager.clear()
            return null
        }

        // Lebih dari satu kali 401 untuk request yang sama berarti refresh tidak menolong.
        if (response.priorResponseCount() >= MAX_RETRY) {
            tokenManager.clear()
            return null
        }

        val failedToken = response.request.header(HEADER)?.removePrefix(BEARER_PREFIX)

        return runBlocking {
            mutex.withLock {
                val current = tokenManager.accessToken
                // Thread lain sudah menyegarkan lebih dulu.
                if (current != null && current != failedToken) {
                    return@withLock response.request.withToken(current)
                }

                val refresh = tokenManager.refreshToken ?: return@withLock null
                val refreshed = runCatching {
                    authApi.get().refreshToken(
                        RefreshTokenRequest(
                            refreshToken = refresh,
                            deviceId = deviceIdProvider.deviceId(),
                        ),
                    )
                }.getOrNull()

                val body = refreshed?.body()
                val data = body?.data
                if (refreshed?.isSuccessful == true && body?.isSuccess == true && data != null) {
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    response.request.withToken(data.accessToken)
                } else {
                    tokenManager.clear()
                    null
                }
            }
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(HEADER, BEARER_PREFIX + token).build()

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_RETRY = 1

        /** Sama dengan entri di [AuthInterceptor.PUBLIC_PATHS]; keduanya harus sejalan. */
        const val REFRESH_PATH = "auth/token/refresh"
    }
}
