package id.bca.bcamobile.domain.transaction.model

/** Satu halaman hasil; [nextCursor] null berarti sudah halaman terakhir. */
data class Page<T>(
    val items: List<T>,
    val nextCursor: String?,
) {
    val hasMore: Boolean get() = nextCursor != null
}

enum class MutationType { DEBIT, CREDIT, UNKNOWN }

data class Mutation(
    val id: String,
    val date: String,
    val time: String,
    val description: String,
    val detail: String,
    /** Selalu positif; arah uang ada di [type]. */
    val amount: Long,
    val balanceAfter: Long,
    val type: MutationType,
    val category: String,
    val referenceNumber: String,
)

data class MutationPage(
    val accountNumber: String,
    val accountLabel: String,
    val balance: Long,
    val page: Page<Mutation>,
)

data class HistoryItem(
    val id: String,
    val type: String,
    val status: String,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
    val description: String,
    val destination: String,
    val destinationName: String,
    val referenceNumber: String,
    val createdAt: String,
)

data class Receipt(
    val transactionId: String,
    val type: String,
    val status: String,
    val date: String,
    val time: String,
    val referenceNumber: String,
    val sourceAccount: String,
    val sourceName: String,
    val destinationNumber: String,
    val destinationName: String,
    val provider: String?,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
)

/** Rentang waktu mutasi. `CUSTOM` mewajibkan tanggal mulai dan akhir. */
enum class MutationPeriod(val wireValue: String) {
    LAST_7_DAYS("LAST_7_DAYS"),
    THIS_MONTH("THIS_MONTH"),
    LAST_MONTH("LAST_MONTH"),
    CUSTOM("CUSTOM"),
}
