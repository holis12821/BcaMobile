package id.bca.bcamobile.domain.transfer.model

enum class TransferType(val wireValue: String) {
    INTERNAL("INTERNAL"),
    EXTERNAL("EXTERNAL"),
    VIRTUAL_ACCOUNT("VIRTUAL_ACCOUNT"),
}

data class RecentTransfer(
    val accountNumber: String,
    val accountName: String,
    val bankCode: String,
    val bankName: String,
)

/**
 * Hasil inquiry berumur pendek — kontrak menyebut 5 menit. Setelah [expiresInSeconds]
 * habis, transfer harus diulang dari langkah inquiry, bukan langsung execute.
 */
data class TransferInquiry(
    val inquiryId: String,
    val destinationAccount: String,
    val destinationName: String,
    val destinationBank: String,
    val bankCode: String,
    val adminFee: Long,
    val expiresInSeconds: Int,
)

data class TransferReceipt(
    val transactionId: String,
    val referenceNumber: String,
    val status: String,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
    val sourceAccount: String,
    val sourceName: String,
    val destinationAccount: String,
    val destinationName: String,
    val destinationBank: String,
    val notes: String?,
    val createdAt: String,
)
