package id.bca.bcamobile.core.network

import id.bca.bcamobile.data.auth.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menyisipkan `Authorization: Bearer` pada klien bernasabah.
 *
 * Request yang sudah membawa header Authorization sendiri dibiarkan — itu jalur
 * yang dipakai [TokenAuthenticator] saat mengulang request setelah refresh.
 * Endpoint publik (login PIN, login biometrik, challenge, refresh) dilewati supaya token
 * kedaluwarsa tidak ikut terkirim dan memicu 401 beruntun.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER) != null || request.url.encodedPath.isPublicAuthPath()) {
            return chain.proceed(request)
        }
        val token = tokenManager.accessToken ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header(HEADER, "Bearer $token").build(),
        )
    }

    private fun String.isPublicAuthPath(): Boolean = PUBLIC_PATHS.any { endsWith(it) }

    private companion object {
        const val HEADER = "Authorization"
        val PUBLIC_PATHS = listOf(
            "auth/login/pin",
            "auth/login/biometric",
            "auth/biometric/challenge",
            "auth/token/refresh",
        )
    }
}
