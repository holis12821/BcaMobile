package id.bca.bcamobile.data.onboarding.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.onboarding.remote.dto.BiometricResponse
import id.bca.bcamobile.data.onboarding.remote.dto.LivenessChallengeRequest
import id.bca.bcamobile.data.onboarding.remote.dto.LivenessChallengeResponse
import id.bca.bcamobile.data.onboarding.remote.dto.CardCatalogResponse
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
import id.bca.bcamobile.data.onboarding.remote.dto.SavingsProductCatalogResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SetCardRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SetCardResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SetCredentialsResponse
import id.bca.bcamobile.data.onboarding.remote.dto.SubmitRequest
import id.bca.bcamobile.data.onboarding.remote.dto.SubmitResponse
import id.bca.bcamobile.data.onboarding.remote.dto.TncResponse
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
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoint onboarding buka rekening.
 *
 * Base URL sudah memuat prefix `v1/onboarding/`, jadi path di sini relatif.
 * Semua method mengembalikan [Response] mentah supaya HTTP status masih terbaca
 * saat mengklasifikasi error: 429 butuh header `Retry-After`, 422 butuh error code.
 *
 * Kontrak lengkap: bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md
 */
interface OnboardingApi {

    // -- Katalog produk, S&K, katalog kartu ------------------------------------
    //
    // Ketiganya dipanggil sebelum sesi ada, jadi tidak ada `session_id` dan tidak ada
    // `Authorization` pada satu pun di antaranya.

    /**
     * Katalog jenis rekening tabungan beserta copy layarnya — layar **pertama** flow.
     *
     * `503 ONBOARDING_CATALOG_UNAVAILABLE` berarti katalognya dimatikan di server, bukan
     * permintaan yang salah: layar jatuh ke daftar bawaan `strings.xml` dan flow tetap
     * jalan, karena pembuatan sesi tidak menuntut katalog ini ada.
     */
    @GET("products")
    suspend fun savingsProducts(): Response<ApiEnvelope<SavingsProductCatalogResponse>>

    /**
     * Teks Syarat & Ketentuan beserta nomor versinya. **Tanpa `session_id` dan tanpa
     * `Authorization`**: layar S&K tampil sebelum sesi dibuat.
     *
     * [version] kosong berarti versi yang sedang berlaku — itu yang dipakai layar. Mengisi
     * parameternya hanya untuk membuka versi tertentu (termasuk yang sudah dicabut), dan
     * versi karangan dijawab `422 TNC_VERSION_UNKNOWN`.
     */
    @GET("tnc")
    suspend fun tnc(
        @Query("version") version: String? = null,
    ): Response<ApiEnvelope<TncResponse>>

    /**
     * Katalog kartu Paspor per produk. **Tanpa `session_id`**: layar Pilih Kartu
     * tampil sebelum sesi dibuat (`bca-mobile-api/docs/08-PILIH-KARTU-API-SPEC.md` §2).
     */
    @GET("products/{product_type}/cards")
    suspend fun cardCatalog(
        @Path("product_type") productType: String,
    ): Response<ApiEnvelope<CardCatalogResponse>>

    /** Ubah kartu pada sesi yang sudah ada — Back dari S&K, lanjut draf, atau dari Ringkasan. */
    @PUT("sessions/{session_id}/card")
    suspend fun setCard(
        @Path("session_id") sessionId: String,
        @Body request: SetCardRequest,
    ): Response<ApiEnvelope<SetCardResponse>>

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
        @Part("client_ocr_text") clientOcrText: RequestBody,
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

    /**
     * Menerbitkan tantangan liveness: nonce sekali pakai, aksi yang sudah diacak
     * server, dan batas waktunya. Terikat `device_id` di header `X-Device-ID`.
     */
    @POST("liveness/challenge")
    suspend fun requestLivenessChallenge(
        @Body request: LivenessChallengeRequest,
    ): Response<ApiEnvelope<LivenessChallengeResponse>>

    /**
     * Mengirim bukti liveness untuk diverifikasi server.
     *
     * Tidak ada part yang menyatakan hasil. `step_meta` hanya mengurutkan frame dan
     * mencatat waktunya; server mendeteksi ulang pose di setiap frame itu sendiri dan
     * tidak memercayai klaim langkah dari client.
     */
    @Multipart
    @POST("biometric")
    suspend fun processBiometric(
        @Part("session_id") sessionId: RequestBody,
        @Part("challenge_id") challengeId: RequestBody,
        @Part("nonce") nonce: RequestBody,
        @Part("device_key_id") deviceKeyId: RequestBody,
        @Part("device_public_key") devicePublicKey: RequestBody,
        @Part("signature") signature: RequestBody,
        @Part("signature_algorithm") signatureAlgorithm: RequestBody,
        @Part("step_meta") stepMeta: RequestBody,
        @Part("risk_signals") riskSignals: RequestBody,
        @Part("integrity_token") integrityToken: RequestBody?,
        @Part neutralFrame: MultipartBody.Part,
        @Part stepFrames: List<MultipartBody.Part>,
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
