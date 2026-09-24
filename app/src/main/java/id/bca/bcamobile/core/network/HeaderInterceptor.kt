package id.bca.bcamobile.core.network

import id.bca.bcamobile.BuildConfig
import id.bca.bcamobile.core.device.DeviceIdProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Header standar untuk semua request, onboarding maupun bernasabah.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §Authentication Header dan
 * `docs/backend/06-BUKA-REKENING-API-SPEC.md` §0.
 *
 * `X-Device-ID` wajib sama dengan `device_id` yang dikirim di body saat sesi
 * onboarding dibuat — keduanya bersumber dari [DeviceIdProvider].
 *
 * Interceptor ini **tidak** menyisipkan Authorization; itu tugas [AuthInterceptor],
 * yang hanya terpasang pada klien bernasabah.
 */
@Singleton
class HeaderInterceptor @Inject constructor(
    private val deviceIdProvider: DeviceIdProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Device-ID", deviceIdProvider.deviceId())
            .header("X-Request-ID", UUID.randomUUID().toString())
            .header("X-Client-Platform", "android")
            .header("X-Client-Version", BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }
}
