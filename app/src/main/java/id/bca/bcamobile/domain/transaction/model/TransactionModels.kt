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

/**
 * Rentang waktu untuk **Mutasi dan Riwayat**.
 *
 * Satu enum untuk dua layar disengaja: keduanya menampilkan "7 hari terakhir"
 * dan pernah berbeda cara memintanya, yang membuat Riwayat mengembalikan
 * seluruh data seolah-olah itu hasil rentang yang dipilih. Nilai-nilainya
 * dibaca dari `resolvePeriod` (`internal/handler/transaction_handler.go`),
 * yang kini melayani `transactions/mutations` **dan** `transactions/history`.
 *
 * Batasnya tanggal WIB dan **kedua ujung inklusif** — transaksi pukul 23.59
 * hari ini tetap masuk `LAST_7_DAYS`.
 *
 * Nilai tak dikenal dijawab `400 VALIDATION_ERROR` berisi
 * `details.allowed_values`; itu bug client, bukan bahan empty state.
 *
 * `CUSTOM` mewajibkan `from` **dan** `to` berformat `YYYY-MM-DD`.
 */
enum class TransactionPeriod(val wireValue: String) {
    LAST_7_DAYS("LAST_7_DAYS"),
    LAST_30_DAYS("LAST_30_DAYS"),
    LAST_90_DAYS("LAST_90_DAYS"),
    THIS_MONTH("THIS_MONTH"),
    LAST_MONTH("LAST_MONTH"),
    CUSTOM("CUSTOM"),
}
