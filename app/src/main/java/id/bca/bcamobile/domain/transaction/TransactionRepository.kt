package id.bca.bcamobile.domain.transaction

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transaction.model.HistoryItem
import id.bca.bcamobile.domain.transaction.model.MutationPage
import id.bca.bcamobile.domain.transaction.model.MutationPeriod
import id.bca.bcamobile.domain.transaction.model.Page
import id.bca.bcamobile.domain.transaction.model.Receipt

/**
 * Mutasi dan riwayat. Kontrak: `docs/backend/01-API-SPECIFICATION.md` §4.
 *
 * Paginasi memakai cursor: kirim `cursor` dari halaman sebelumnya, bukan nomor halaman.
 */
interface TransactionRepository {

    suspend fun mutations(
        accountId: String,
        period: MutationPeriod,
        startDate: String? = null,
        endDate: String? = null,
        cursor: String? = null,
    ): DataResult<MutationPage>

    suspend fun history(
        type: String? = null,
        cursor: String? = null,
    ): DataResult<Page<HistoryItem>>

    suspend fun receipt(transactionId: String): DataResult<Receipt>
}
