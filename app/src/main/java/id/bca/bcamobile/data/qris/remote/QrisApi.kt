package id.bca.bcamobile.data.qris.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.qris.remote.dto.QrisDecodeRequest
import id.bca.bcamobile.data.qris.remote.dto.QrisDecodeResponse
import id.bca.bcamobile.data.qris.remote.dto.QrisPayRequest
import id.bca.bcamobile.data.qris.remote.dto.QrisPayResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * QRIS: decode QR lalu bayar.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §8.
 *
 * Idempotency dikirim lewat header `X-Idempotency-Key`, sama seperti
 * `transfer/execute` dan `ewallet/topup`. Spec memuat juga field `idempotency_key`
 * di body, tapi dua sumber kebenaran untuk satu kunci justru berisiko — lihat
 * catatan selisih di §Integrasi API `CLAUDE.md`.
 */
interface QrisApi {

    @POST("qris/decode")
    suspend fun decode(
        @Body request: QrisDecodeRequest,
    ): Response<ApiEnvelope<QrisDecodeResponse>>

    @POST("qris/pay")
    suspend fun pay(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: QrisPayRequest,
    ): Response<ApiEnvelope<QrisPayResponse>>
}
