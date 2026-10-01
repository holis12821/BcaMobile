package id.bca.bcamobile.data.account.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.account.remote.dto.BalanceResponse
import id.bca.bcamobile.data.account.remote.dto.DashboardResponse
import id.bca.bcamobile.data.account.remote.dto.LimitStatusResponse
import id.bca.bcamobile.data.account.remote.dto.ProfileOtpResponse
import id.bca.bcamobile.data.account.remote.dto.ProfileResponse
import id.bca.bcamobile.data.account.remote.dto.PushTokenRequest
import id.bca.bcamobile.data.account.remote.dto.SettingsRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateLimitRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateProfileRequest
import id.bca.bcamobile.data.auth.remote.dto.MessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Endpoint rekening dan profil. Kontrak: `docs/backend/01-API-SPECIFICATION.md` §3. */
interface AccountApi {

    @GET("account/profile")
    suspend fun profile(): Response<ApiEnvelope<ProfileResponse>>

    /**
     * Menerbitkan OTP ke nomor HP **terdaftar**, bukan ke nomor di body.
     * Kodenya mengesahkan [updateProfile]. Tanpa body.
     */
    @POST("account/profile/otp")
    suspend fun requestProfileOtp(): Response<ApiEnvelope<ProfileOtpResponse>>

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

    /** Batas berlaku beserta pemakaian hari ini. Dipakai saat layar Atur Limit dibuka. */
    @GET("account/transaction-limit")
    suspend fun transactionLimits(): Response<ApiEnvelope<LimitStatusResponse>>

    @PUT("account/transaction-limit")
    suspend fun updateTransactionLimit(
        @Body request: UpdateLimitRequest,
    ): Response<ApiEnvelope<LimitStatusResponse>>

    /**
     * Mendaftarkan token push. Kolom `devices.push_token` sudah ada sejak
     * migrasi pertama tapi belum pernah diisi dari aplikasi.
     */
    @POST("account/device/push-token")
    suspend fun registerPushToken(
        @Body request: PushTokenRequest,
    ): Response<ApiEnvelope<MessageResponse>>
}
