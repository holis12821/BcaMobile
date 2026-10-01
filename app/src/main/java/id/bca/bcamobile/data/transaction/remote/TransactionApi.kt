package id.bca.bcamobile.data.transaction.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.transaction.remote.dto.HistoryResponse
import id.bca.bcamobile.data.transaction.remote.dto.MutationsResponse
import id.bca.bcamobile.data.transaction.remote.dto.ReceiptResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Mutasi rekening dan riwayat transaksi.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §4.
 *
 * Halaman berikutnya ditentukan `pagination.cursor` di envelope, bukan nomor halaman.
 *
 * **Kedua endpoint memakai kosakata `period` yang sama.** Keduanya melewati
 * `resolvePeriod` di `internal/handler/transaction_handler.go`, jadi
 * `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_90_DAYS`, `THIS_MONTH`, `LAST_MONTH`,
 * dan `CUSTOM` berlaku di dua-duanya. Nilai di luar itu dijawab
 * `400 VALIDATION_ERROR` berisi `details.allowed_values` — dulu salah tulis
 * lolos sebagai "tanpa filter" dan layar menampilkan seluruh data seolah-olah
 * itu rentang yang diminta.
 */
interface TransactionApi {

    /**
     * Mutasi rekening.
     *
     * `CUSTOM` dikirim **bersama** `from` dan `to`; server yang memutuskan
     * keduanya wajib. `start_date`/`end_date` diterima sebagai nama lain, tapi
     * jangan kirim dua-duanya — pilih `from`/`to`.
     */
    @GET("transactions/mutations")
    suspend fun mutations(
        @Query("account_id") accountId: String,
        @Query("period") period: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): Response<ApiEnvelope<MutationsResponse>>

    /**
     * Riwayat transaksi.
     *
     * **[type] null berarti "semua jenis".** Handler menyaring `type` apa
     * adanya (`AND type = $n`), jadi mengirim `"ALL"` menghasilkan daftar
     * kosong — bukan seluruh riwayat. Tab "Semua" tidak boleh mengirim
     * parameter ini sama sekali.
     */
    @GET("transactions/history")
    suspend fun history(
        @Query("type") type: String? = null,
        @Query("period") period: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): Response<ApiEnvelope<HistoryResponse>>

    @GET("transactions/{transaction_id}/receipt")
    suspend fun receipt(
        @Path("transaction_id") transactionId: String,
    ): Response<ApiEnvelope<ReceiptResponse>>

    /**
     * Struk dalam bentuk PDF.
     *
     * Balasannya **berkas, bukan envelope**, jadi tidak lewat `ApiCaller`:
     * `@Streaming` menahan isinya di jaringan sampai ditulis ke storage,
     * supaya struk besar tidak dimuat utuh ke memori lebih dulu.
     */
    @Streaming
    @GET("transactions/{transaction_id}/receipt/pdf")
    suspend fun receiptPdf(
        @Path("transaction_id") transactionId: String,
    ): Response<ResponseBody>

    companion object {
        const val DEFAULT_LIMIT = 20
    }
}
