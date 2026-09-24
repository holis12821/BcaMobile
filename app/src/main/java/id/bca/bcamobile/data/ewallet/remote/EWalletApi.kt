package id.bca.bcamobile.data.ewallet.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletInquiryRequest
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletInquiryResponse
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletProvidersResponse
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletTopUpRequest
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletTopUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Top up e-wallet, pola sama dengan transfer: providers -> inquiry -> PIN -> topup.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §6.
 *
 * Batas nominal dan preset datang dari [providers] — jangan ditanam di client.
 */
interface EWalletApi {

    @GET("ewallet/providers")
    suspend fun providers(): Response<ApiEnvelope<EWalletProvidersResponse>>

    @POST("ewallet/inquiry")
    suspend fun inquiry(
        @Body request: EWalletInquiryRequest,
    ): Response<ApiEnvelope<EWalletInquiryResponse>>

    @POST("ewallet/topup")
    suspend fun topUp(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: EWalletTopUpRequest,
    ): Response<ApiEnvelope<EWalletTopUpResponse>>
}
