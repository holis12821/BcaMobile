package id.bca.bcamobile.data.transfer.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.transfer.remote.dto.RecentTransferResponse
import id.bca.bcamobile.data.transfer.remote.dto.TransferExecuteRequest
import id.bca.bcamobile.data.transfer.remote.dto.TransferExecuteResponse
import id.bca.bcamobile.data.transfer.remote.dto.TransferInquiryRequest
import id.bca.bcamobile.data.transfer.remote.dto.TransferInquiryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Transfer mengikuti pola tiga langkah: inquiry -> verifikasi PIN -> execute.
 * Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §5 dan skill `bca-mobile-api` §8.1.
 *
 * Verifikasi PIN memakai `AuthApi.verifyPin`; token hasilnya dioper ke [execute].
 */
interface TransferApi {

    @GET("transfer/recent")
    suspend fun recent(): Response<ApiEnvelope<RecentTransferResponse>>

    @POST("transfer/inquiry")
    suspend fun inquiry(
        @Body request: TransferInquiryRequest,
    ): Response<ApiEnvelope<TransferInquiryResponse>>

    @POST("transfer/execute")
    suspend fun execute(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: TransferExecuteRequest,
    ): Response<ApiEnvelope<TransferExecuteResponse>>
}
