package id.bca.bcamobile.data.transaction.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.transaction.remote.dto.HistoryResponse
import id.bca.bcamobile.data.transaction.remote.dto.MutationsResponse
import id.bca.bcamobile.data.transaction.remote.dto.ReceiptResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mutasi rekening dan riwayat transaksi.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §4.
 *
 * Halaman berikutnya ditentukan `pagination.cursor` di envelope, bukan nomor halaman.
 */
interface TransactionApi {

    @GET("transactions/mutations")
    suspend fun mutations(
        @Query("account_id") accountId: String,
        @Query("period") period: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): Response<ApiEnvelope<MutationsResponse>>

    @GET("transactions/history")
    suspend fun history(
        @Query("type") type: String = TYPE_ALL,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): Response<ApiEnvelope<HistoryResponse>>

    @GET("transactions/{transaction_id}/receipt")
    suspend fun receipt(
        @Path("transaction_id") transactionId: String,
    ): Response<ApiEnvelope<ReceiptResponse>>

    companion object {
        const val DEFAULT_LIMIT = 20
        const val TYPE_ALL = "ALL"
    }
}
