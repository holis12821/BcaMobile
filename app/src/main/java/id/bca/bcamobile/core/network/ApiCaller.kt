package id.bca.bcamobile.core.network

import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Menjalankan panggilan Retrofit lalu menerjemahkan hasilnya jadi [DataResult].
 *
 * Kebijakan retry mengikuti skill `buka-rekening-api`:
 *   - error jaringan  -> 3x percobaan ulang, jeda 1s, 3s, 5s
 *   - HTTP 5xx        -> 2x percobaan ulang
 *   - HTTP 429        -> hormati `details.retry_after_seconds` dari body (header
 *                        `Retry-After` hanya cadangan), tidak diulang otomatis
 *   - HTTP 401        -> [ApiFailure.Unauthorized]; refresh sudah dicoba di
 *                        [TokenAuthenticator] sebelum sampai ke sini
 *   - HTTP 4xx lain   -> tidak diulang, langsung tampilkan pesan server
 */
@Singleton
class ApiCaller @Inject constructor(
    private val json: Json,
) {

    /**
     * @param notFoundAs arti HTTP 404 tanpa error code di body. Onboarding memakai
     *   [ApiFailure.SessionNotFound]; domain lain [ApiFailure.Unknown].
     */
    suspend fun <T> call(
        allowRetry: Boolean = true,
        notFoundAs: ApiFailure = ApiFailure.Unknown,
        block: suspend () -> Response<ApiEnvelope<T>>,
    ): DataResult<T> {
        var lastError: ApiFailure = ApiFailure.Unknown

        for (attempt in 0..MAX_ATTEMPTS) {
            val outcome = runCatching { withTimeoutOrNull(CALL_TIMEOUT_MS.milliseconds) { block() } }

            val response = outcome.getOrElse { throwable ->
                // runCatching ikut menangkap CancellationException. Kalau ditelan,
                // coroutine yang sudah dibatalkan tetap menulis state error — layar
                // yang baru ditinggalkan memunculkan pesan gagal palsu.
                if (throwable is CancellationException) throw throwable

                lastError = when (throwable) {
                    is SocketTimeoutException -> ApiFailure.Timeout
                    is IOException -> ApiFailure.Network
                    else -> ApiFailure.Unknown
                }
                if (!allowRetry || lastError == ApiFailure.Unknown) {
                    return DataResult.Failure(lastError)
                }
                if (attempt < NETWORK_RETRIES) {
                    delay(BACKOFF_MS[attempt].milliseconds)
                    continue
                }
                return DataResult.Failure(lastError)
            }

            if (response == null) {
                lastError = ApiFailure.Timeout
                if (allowRetry && attempt < NETWORK_RETRIES) {
                    delay(BACKOFF_MS[attempt].milliseconds)
                    continue
                }
                return DataResult.Failure(lastError)
            }

            if (response.isSuccessful) {
                val envelope = response.body()
                    ?: return DataResult.Failure(ApiFailure.Unknown)
                val payload = envelope.data
                return when {
                    !envelope.isSuccess -> DataResult.Failure(classifyBody(envelope.error))
                    payload == null -> DataResult.Failure(ApiFailure.Unknown)
                    else -> DataResult.Success(payload)
                }
            }

            val code = response.code()
            val apiError = parseErrorBody(response)

            // 5xx layak diulang; sisanya keputusan final dari server.
            if (code >= 500) {
                // Kecuali yang membawa kode bisnis di bawah: itu jawaban final
                // yang kebetulan berstatus 5xx, bukan kegagalan transport.
                val businessCode = apiError?.code
                if (businessCode != null && businessCode in FINAL_5XX_CODES) {
                    return DataResult.Failure(
                        ApiFailure.Business(businessCode, apiError.message),
                    )
                }
                lastError = ApiFailure.Server
                if (allowRetry && attempt < SERVER_RETRIES) {
                    delay(BACKOFF_MS[attempt].milliseconds)
                    continue
                }
                return DataResult.Failure(lastError)
            }

            return DataResult.Failure(
                classifyHttp(code, apiError, response.headers()["Retry-After"], notFoundAs),
            )
        }

        return DataResult.Failure(lastError)
    }

    private fun <T> parseErrorBody(response: Response<ApiEnvelope<T>>): ApiError? {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val errorObj = obj["error"]?.jsonObject ?: return null
            ApiError(
                code = errorObj["code"]?.jsonPrimitive?.content.orEmpty(),
                message = errorObj["message"]?.jsonPrimitive?.content.orEmpty(),
                details = errorObj["details"],
            )
        }.getOrNull()
    }

    /** Envelope HTTP 200 tapi `status: "error"`. */
    private fun classifyBody(error: ApiError?): ApiFailure =
        classifyCode(error?.code, error?.message, retryAfterFrom(error), error)
            ?: ApiFailure.Unknown

    private fun classifyHttp(
        httpCode: Int,
        error: ApiError?,
        retryAfterHeader: String?,
        notFoundAs: ApiFailure,
    ): ApiFailure {
        val retryAfter = retryAfterFrom(error) ?: retryAfterHeader?.toIntOrNull()
        classifyCode(error?.code, error?.message, retryAfter, error)?.let { return it }

        return when (httpCode) {
            401 -> ApiFailure.Unauthorized
            404 -> notFoundAs
            429 -> ApiFailure.RateLimited(retryAfter)
            in 400..499 -> error?.let { ApiFailure.Business(it.code, it.message) }
                ?: ApiFailure.Unknown
            else -> ApiFailure.Unknown
        }
    }

    private fun classifyCode(
        code: String?,
        message: String?,
        retryAfter: Int?,
        error: ApiError?,
    ): ApiFailure? =
        when (code) {
            null, "" -> null
            CODE_SESSION_EXPIRED -> ApiFailure.SessionExpired
            CODE_NOT_FOUND -> ApiFailure.SessionNotFound
            CODE_INVALID_STEP -> ApiFailure.InvalidStep
            CODE_RATE_LIMIT -> ApiFailure.RateLimited(retryAfter, message = message.orEmpty())
            CODE_OTP_BLOCKED -> ApiFailure.RateLimited(
                retryAfterSeconds = retryAfter,
                isOtpBlocked = true,
                message = message.orEmpty(),
            )
            CODE_TNC_OUTDATED -> ApiFailure.TncOutdated(
                currentVersion = currentVersionFrom(error).orEmpty(),
                message = message.orEmpty(),
            )
            CODE_INTERNAL -> ApiFailure.Server
            else -> ApiFailure.Business(code, message.orEmpty())
        }

    /**
     * `details.current_version` pada `409 TNC_VERSION_OUTDATED`.
     *
     * Dibungkus `runCatching` seperti [retryAfterFrom]: `details` bertipe `JsonElement?`
     * dan bisa `null` atau bukan objek, dan versi yang tidak terbaca tidak boleh
     * menggagalkan klasifikasinya — layar tetap bisa memuat ulang tanpa tahu tujuannya.
     */
    private fun currentVersionFrom(error: ApiError?): String? = runCatching {
        error?.details?.jsonObject?.get("current_version")?.jsonPrimitive?.content
    }.getOrNull()

    private fun retryAfterFrom(error: ApiError?): Int? = runCatching {
        error?.details?.jsonObject?.get("retry_after_seconds")?.jsonPrimitive?.content?.toInt()
    }.getOrNull()

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val NETWORK_RETRIES = 3
        const val SERVER_RETRIES = 2
        const val CALL_TIMEOUT_MS = 60_000L
        val BACKOFF_MS = longArrayOf(1_000L, 3_000L, 5_000L)

        const val CODE_SESSION_EXPIRED = "ONBOARDING_SESSION_EXPIRED"
        const val CODE_NOT_FOUND = "ONBOARDING_NOT_FOUND"
        const val CODE_INVALID_STEP = "ONBOARDING_INVALID_STEP"
        const val CODE_RATE_LIMIT = "RATE_LIMIT_EXCEEDED"
        const val CODE_OTP_BLOCKED = "OTP_BLOCKED"
        const val CODE_INTERNAL = "INTERNAL_ERROR"
        const val CODE_TNC_OUTDATED = "TNC_VERSION_OUTDATED"
        const val CODE_OTP_DELIVERY_FAILED = "OTP_DELIVERY_FAILED"
        const val CODE_CATALOG_UNAVAILABLE = "ONBOARDING_CATALOG_UNAVAILABLE"

        /**
         * Kode 5xx yang **tidak** boleh diulang dan tidak boleh disamarkan jadi
         * [ApiFailure.Server].
         *
         * `OTP_DELIVERY_FAILED` berstatus 503, tapi kodenya tetap terbit dan sah —
         * yang gagal hanya pengiriman SMS-nya. Mengulang request tidak memperbaiki
         * apa pun, dan sebagai `Server` layar OTP akan menawarkan "coba lagi"
         * padahal yang benar adalah "kirim ulang".
         *
         * `ONBOARDING_CATALOG_UNAVAILABLE` juga 503, dan juga jawaban final: itu yang
         * server balas saat katalog jenis rekening dimatikan `FEATURE_ONBOARDING_PRODUCT_CATALOG`
         * atau tidak ada produk aktif. Tanpa baris ini layar **pertama** buka rekening
         * menahan loading ±4 detik (1s + 3s) sebelum menampilkan daftar bawaan yang sejak
         * awal sudah ada di APK — menunggu dua kali untuk jawaban yang tidak akan berubah.
         */
        val FINAL_5XX_CODES = setOf(CODE_OTP_DELIVERY_FAILED, CODE_CATALOG_UNAVAILABLE)
    }
}
