package id.bca.bcamobile.data.account.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.account.remote.dto.BalanceResponse
import id.bca.bcamobile.data.account.remote.dto.DashboardResponse
import id.bca.bcamobile.data.account.remote.dto.ProfileResponse
import id.bca.bcamobile.data.account.remote.dto.SettingsRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateLimitRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateProfileRequest
import id.bca.bcamobile.data.auth.remote.dto.MessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Endpoint rekening dan profil. Kontrak: `docs/backend/01-API-SPECIFICATION.md` §3. */
interface AccountApi {

    @GET("account/profile")
    suspend fun profile(): Response<ApiEnvelope<ProfileResponse>>

    @PUT("account/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): Response<ApiEnvelope<MessageResponse>>

    @GET("account/balance")
    suspend fun balance(): Response<ApiEnvelope<BalanceResponse>>

    @GET("account/dashboard")
    suspend fun dashboard(): Response<ApiEnvelope<DashboardResponse>>

    @PUT("account/settings")
    suspend fun updateSettings(
        @Body request: SettingsRequest,
    ): Response<ApiEnvelope<MessageResponse>>

    @PUT("account/transaction-limit")
    suspend fun updateTransactionLimit(
        @Body request: UpdateLimitRequest,
    ): Response<ApiEnvelope<MessageResponse>>
}
