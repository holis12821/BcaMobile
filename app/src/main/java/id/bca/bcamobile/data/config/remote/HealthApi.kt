package id.bca.bcamobile.data.config.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.config.remote.dto.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Health check dan konfigurasi aplikasi — dipakai layar Splash.
 * Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §1.
 */
interface HealthApi {

    @GET("health")
    suspend fun health(): Response<ApiEnvelope<HealthResponse>>
}
