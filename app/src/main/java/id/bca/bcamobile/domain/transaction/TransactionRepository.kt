package id.bca.bcamobile.domain.transaction

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transaction.model.HistoryItem
import id.bca.bcamobile.domain.transaction.model.MutationPage
import id.bca.bcamobile.domain.transaction.model.Page
import id.bca.bcamobile.domain.transaction.model.Receipt
import id.bca.bcamobile.domain.transaction.model.TransactionPeriod

/**
 * Mutasi dan riwayat. Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §4.
 *
 * Paginasi memakai cursor: kirim `cursor` dari halaman sebelumnya, bukan nomor halaman.
 */
interface TransactionRepository {

    suspend fun mutations(
        accountId: String,
        period: TransactionPeriod,
        startDate: String? = null,
        endDate: String? = null,
        cursor: String? = null,
    ): DataResult<MutationPage>

    /**
     * Riwayat transaksi.
     *
     * [type] null berarti **semua jenis** — jangan mengirim `"ALL"`, itu bukan
     * jenis yang dikenal server dan hasilnya daftar kosong.
     *
     * [period] memakai kosakata yang sama dengan [mutations]; null berarti
     * tanpa filter tanggal. [startDate] dan [endDate] hanya dibaca saat
     * [period] `CUSTOM`, formatnya `YYYY-MM-DD`.
     */
    suspend fun history(
        type: String? = null,
        period: TransactionPeriod? = null,
        startDate: String? = null,
        endDate: String? = null,
        cursor: String? = null,
    ): DataResult<Page<HistoryItem>>

    suspend fun receipt(transactionId: String): DataResult<Receipt>

    /**
     * Struk versi PDF, siap ditulis ke storage.
     *
     * Balasannya berkas mentah, bukan `ApiEnvelope`, jadi klasifikasi error-nya
     * lebih kasar daripada endpoint lain: yang bisa dibedakan hanya gagal
     * jaringan dan penolakan server.
     */
    suspend fun receiptPdf(transactionId: String): DataResult<ByteArray>
}
