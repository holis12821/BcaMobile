package id.bca.bcamobile.data.auth.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.auth.remote.dto.BiometricChallengeResponse
import id.bca.bcamobile.data.auth.remote.dto.ChangePinRequest
import id.bca.bcamobile.data.auth.remote.dto.LoginBiometricRequest
import id.bca.bcamobile.data.auth.remote.dto.LoginPinRequest
import id.bca.bcamobile.data.auth.remote.dto.LoginResponse
import id.bca.bcamobile.data.auth.remote.dto.LogoutRequest
import id.bca.bcamobile.data.auth.remote.dto.MessageResponse
import id.bca.bcamobile.data.auth.remote.dto.RefreshTokenRequest
import id.bca.bcamobile.data.auth.remote.dto.RegisterBiometricRequest
import id.bca.bcamobile.data.auth.remote.dto.RegisterBiometricResponse
import id.bca.bcamobile.data.auth.remote.dto.RefreshTokenResponse
import id.bca.bcamobile.data.auth.remote.dto.VerifyPinRequest
import id.bca.bcamobile.data.auth.remote.dto.VerifyPinResponse
import id.bca.bcamobile.data.onboarding.remote.dto.PublicKeyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoint autentikasi. Kontrak: `docs/backend/01-API-SPECIFICATION.md` §2.
 *
 * Base URL memuat prefix `v1/`, jadi path di sini relatif terhadap itu.
 */
interface AuthApi {

    /**
     * Kunci publik untuk `pin_encrypted`. Publik — tanpa Authorization.
     *
     * Bentuk response-nya sama persis dengan
     * `GET /onboarding/credentials/public-key` dan **kuncinya pun sama**, jadi
     * [PublicKeyResponse] milik onboarding dipakai ulang di sini.
     */
    @GET("auth/pin/public-key")
    suspend fun pinPublicKey(): Response<ApiEnvelope<PublicKeyResponse>>

    @POST("auth/login/pin")
    suspend fun loginWithPin(
        @Body request: LoginPinRequest,
    ): Response<ApiEnvelope<LoginResponse>>

    @GET("auth/biometric/challenge")
    suspend fun biometricChallenge(
        @Query("device_id") deviceId: String,
    ): Response<ApiEnvelope<BiometricChallengeResponse>>

    @POST("auth/login/biometric")
    suspend fun loginWithBiometric(
        @Body request: LoginBiometricRequest,
    ): Response<ApiEnvelope<LoginResponse>>

    @POST("auth/biometric/register")
    suspend fun registerBiometric(
        @Body request: RegisterBiometricRequest,
    ): Response<ApiEnvelope<RegisterBiometricResponse>>

    @POST("auth/token/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): Response<ApiEnvelope<RefreshTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): Response<ApiEnvelope<MessageResponse>>

    @POST("auth/pin/verify")
    suspend fun verifyPin(
        @Body request: VerifyPinRequest,
    ): Response<ApiEnvelope<VerifyPinResponse>>

    /** Memindahkan PIN **transaksi**. Bukan kredensial login. */
    @POST("auth/pin/change")
    suspend fun changePin(
        @Body request: ChangePinRequest,
    ): Response<ApiEnvelope<MessageResponse>>

    /**
     * Memindahkan **kode akses** (kredensial login).
     *
     * Kode akses dan PIN transaksi adalah dua rahasia berbeda dan punya
     * endpoint masing-masing. Layar Ubah Kode Akses sebelumnya memanggil
     * `auth/pin/change`, jadi yang berubah adalah PIN transaksi — sementara
     * kode akses login tetap seperti semula.
     *
     * Bentuk body-nya sama (`ChangePINRequest` di backend), jadi
     * [ChangePinRequest] dipakai ulang.
     */
    @POST("auth/access-code/change")
    suspend fun changeAccessCode(
        @Body request: ChangePinRequest,
    ): Response<ApiEnvelope<MessageResponse>>
}
