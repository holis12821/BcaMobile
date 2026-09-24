package id.bca.bcamobile.data.onboarding.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.onboarding.remote.dto.BiometricResponse
import id.bca.bcamobile.data.onboarding.remote.dto.CreateSessionRequest
import id.bca.bcamobile.data.onboarding.remote.dto.CreateSessionResponse
import id.bca.bcamobile.data.onboarding.remote.dto.DeleteResponse
import id.bca.bcamobile.data.onboarding.remote.dto.GetSessionResponse
import id.bca.bcamobile.data.onboarding.remote.dto.JoinQueueRequest
import id.bca.bcamobile.data.onboarding.remote.dto.JoinQueueResponse
import id.bca.bcamobile.data.onboarding.remote.dto.OcrResponse
import id.bca.bcamobile.data.onboarding.remote.dto.PublicKeyResponse
import id.bca.bcamobile.data.onboarding.remote.dto.ResendOtpRequest
import id.bca.bcamobile.data.onboarding.remote.dto.ResendOtpResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SavePersonalDataRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SavePersonalDataResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SubmitRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SubmitResponse
import id.bca.bcamobile.data.onboarding.remote.dto.VerifyOtpRequest
import id.bca.bcamobile.data.onboarding.remote.dto.VerifyOtpResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Endpoint onboarding buka rekening.
 *
 * Base URL sudah memuat prefix `v1/onboarding/`, jadi path di sini relatif.
 * Semua method mengembalikan [Response] mentah supaya HTTP status masih terbaca
 * saat mengklasifikasi error: 429 butuh header `Retry-After`, 422 butuh error code.
 *
 * Kontrak lengkap: docs/backend/06-BUKA-REKENING-API-SPEC.md
 */
interface OnboardingApi {

    // -- Session ---------------------------------------------------------------

    @POST("sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest,
    ): Response<ApiEnvelope<CreateSessionResponse>>

    @GET("sessions/{session_id}")
    suspend fun getSession(
        @Path("session_id") sessionId: String,
    ): Response<ApiEnvelope<GetSessionResponse>>

    @DELETE("sessions/{session_id}")
    suspend fun cancelSession(
        @Path("session_id") sessionId: String,
    ): Response<ApiEnvelope<DeleteResponse>>

    // -- OCR -------------------------------------------------------------------

    @Multipart
    @POST("ocr")
    suspend fun processOcr(
        @Part("session_id") sessionId: RequestBody,
        @Part ktpPhoto: MultipartBody.Part,
        @Part("flash_used") flashUsed: RequestBody,
        @Part("auto_captured") autoCaptured: RequestBody,
        @Part("resolution") resolution: RequestBody,
    ): Response<ApiEnvelope<OcrResponse>>

    @GET("ocr/{session_id}")
    suspend fun getOcrResult(
        @Path("session_id") sessionId: String,
    ): Response<ApiEnvelope<OcrResponse>>

    // -- Data pribadi dan OTP --------------------------------------------------

    @POST("personal-data")
    suspend fun savePersonalData(
        @Body request: SavePersonalDataRequest,
    ): Response<ApiEnvelope<SavePersonalDataResponse>>

    @POST("verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest,
    ): Response<ApiEnvelope<VerifyOtpResponse>>

    @POST("resend-otp")
    suspend fun resendOtp(
        @Body request: ResendOtpRequest,
    ): Response<ApiEnvelope<ResendOtpResponse>>

    // -- Biometrik -------------------------------------------------------------

    @Multipart
    @POST("biometric")
    suspend fun processBiometric(
        @Part("session_id") sessionId: RequestBody,
        @Part facePhoto: MultipartBody.Part,
        @Part livenessFrames: List<MultipartBody.Part>,
        @Part("liveness_meta") livenessMeta: RequestBody,
    ): Response<ApiEnvelope<BiometricResponse>>

    // -- Video call ------------------------------------------------------------

    @POST("video-call/queue")
    suspend fun joinQueue(
        @Body request: JoinQueueRequest,
    ): Response<ApiEnvelope<JoinQueueResponse>>

    // -- Kredensial ------------------------------------------------------------

    @GET("credentials/public-key")
    suspend fun getPublicKey(): Response<ApiEnvelope<PublicKeyResponse>>

    @POST("credentials")
    suspend fun setCredentials(
        @Body request: SetCredentialsRequest,
    ): Response<ApiEnvelope<SetCredentialsResponse>>

    // -- Submit ----------------------------------------------------------------

    @POST("submit")
    suspend fun submit(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: SubmitRequest,
    ): Response<ApiEnvelope<SubmitResponse>>
}
