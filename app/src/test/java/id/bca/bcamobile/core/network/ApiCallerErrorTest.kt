package id.bca.bcamobile.core.network

import id.bca.bcamobile.domain.common.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Klasifikasi error [ApiCaller] untuk kasus yang perilaku UI-nya berbeda.
 *
 * Fokusnya satu jebakan: tidak semua 5xx adalah kegagalan transport.
 * `OTP_DELIVERY_FAILED` berstatus 503 tapi kodenya tetap terbit dan sah —
 * kalau disamarkan jadi [ApiFailure.Server], layar OTP menawarkan "coba lagi"
 * padahal yang benar "kirim ulang" (skill `frontend-otp-verification` §4).
 */
class ApiCallerErrorTest {

    private val caller = ApiCaller(Json { ignoreUnknownKeys = true })

    @Test
    fun `503 OTP_DELIVERY_FAILED sampai ke pemanggil sebagai error bisnis`() = runTest {
        var attempts = 0
        val result = caller.call<Unit> {
            attempts += 1
            errorResponse(503, "OTP_DELIVERY_FAILED", "Kode OTP gagal dikirim.")
        }

        assertEquals(
            ApiFailure.Business("OTP_DELIVERY_FAILED", "Kode OTP gagal dikirim."),
            (result as DataResult.Failure).error,
        )
        // Mengulang pengiriman SMS yang gagal tidak memperbaiki apa pun.
        assertEquals(1, attempts)
    }

    @Test
    fun `503 tanpa kode final tetap diperlakukan sebagai kegagalan server`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            errorResponse(503, "PROVIDER_NOT_CONFIGURED", "Layanan belum tersedia.")
        }

        assertEquals(ApiFailure.Server, (result as DataResult.Failure).error)
    }

    @Test
    fun `500 masih diulang sesuai kebijakan retry`() = runTest {
        var attempts = 0
        val result = caller.call<Unit> {
            attempts += 1
            errorResponse(500, "INTERNAL_ERROR", "Terjadi kesalahan.")
        }

        assertEquals(ApiFailure.Server, (result as DataResult.Failure).error)
        assertTrue("percobaan = $attempts", attempts > 1)
    }

    @Test
    fun `429 OTP_BLOCKED memakai retry_after_seconds dari body`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            Response.error(
                429,
                """
                {"status":"error","error":{"code":"OTP_BLOCKED","message":"Terlalu banyak percobaan.",
                "details":{"retry_after_seconds":1800}}}
                """.trimIndent().toResponseBody(JSON),
            )
        }

        val failure = (result as DataResult.Failure).error as ApiFailure.RateLimited
        assertEquals(1_800, failure.retryAfterSeconds)
        assertTrue(failure.isOtpBlocked)
    }

    @Test
    fun `429 kuota kirim ulang tidak menandai blokir OTP`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            Response.error(
                429,
                """
                {"status":"error","error":{"code":"RATE_LIMIT_EXCEEDED","message":"Batas tercapai.",
                "details":{"retry_after_seconds":900}}}
                """.trimIndent().toResponseBody(JSON),
            )
        }

        val failure = (result as DataResult.Failure).error as ApiFailure.RateLimited
        assertEquals(900, failure.retryAfterSeconds)
        // Kode terakhir masih sah, jadi input tidak boleh ikut dimatikan.
        assertFalse(failure.isOtpBlocked)
    }

    @Test
    fun `409 TNC_VERSION_OUTDATED membawa current_version dari details`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            Response.error(
                409,
                """
                {"status":"error","error":{"code":"TNC_VERSION_OUTDATED",
                "message":"Syarat & Ketentuan telah diperbarui. Mohon baca dan setujui versi terbaru.",
                "details":{"sent_version":"2026-09-01","current_version":"2026-12-01"}}}
                """.trimIndent().toResponseBody(JSON),
            )
        }

        // Sebagai ApiFailure.Business, `details` hilang dan layar hanya bisa menawarkan
        // "coba lagi" — yang mengirim versi lama yang sama dan dijamin gagal lagi.
        val failure = (result as DataResult.Failure).error as ApiFailure.TncOutdated
        assertEquals("2026-12-01", failure.currentVersion)
        assertTrue(failure.message.startsWith("Syarat & Ketentuan telah diperbarui"))
    }

    @Test
    fun `409 TNC_VERSION_OUTDATED tanpa details tetap terklasifikasi`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            errorResponse(409, "TNC_VERSION_OUTDATED", "S&K telah diperbarui.")
        }

        // Versi tujuan yang tidak terbaca tidak boleh menjatuhkannya kembali ke Business:
        // `GET /onboarding/tnc` tanpa parameter memang selalu menjawab versi aktif, jadi
        // layar tetap bisa memuat ulang tanpa tahu tujuannya.
        val failure = (result as DataResult.Failure).error as ApiFailure.TncOutdated
        assertEquals("", failure.currentVersion)
    }

    @Test
    fun `422 TNC_VERSION_UNKNOWN tetap error bisnis biasa`() = runTest {
        val result = caller.call<Unit>(allowRetry = false) {
            errorResponse(422, "TNC_VERSION_UNKNOWN", "Versi Syarat & Ketentuan tidak dikenal.")
        }

        // Versi yang tidak pernah ada adalah bug client, bukan keadaan yang nasabah bisa
        // pulihkan — jangan ikut jalur muat ulang milik 409.
        assertEquals(
            ApiFailure.Business("TNC_VERSION_UNKNOWN", "Versi Syarat & Ketentuan tidak dikenal."),
            (result as DataResult.Failure).error,
        )
    }

    private fun errorResponse(code: Int, errorCode: String, message: String) =
        Response.error<ApiEnvelope<Unit>>(
            code,
            """{"status":"error","error":{"code":"$errorCode","message":"$message"}}"""
                .toResponseBody(JSON),
        )

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
