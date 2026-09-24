package id.bca.bcamobile.domain.ewallet.model

/**
 * Batas nominal dan preset datang dari server — validasi input memakai nilai ini,
 * bukan angka tetap di client.
 */
data class EWalletProvider(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val isActive: Boolean,
    val minAmount: Long,
    val maxAmount: Long,
    val adminFee: Long,
    val presetAmounts: List<Long>,
)

data class EWalletInquiry(
    val inquiryId: String,
    val provider: String,
    val destinationName: String,
    val destinationPhone: String,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
    val sourceAccount: String,
    val expiresInSeconds: Int,
)

data class EWalletReceipt(
    val transactionId: String,
    val referenceNumber: String,
    val status: String,
    val provider: String,
    val destinationPhone: String,
    val destinationName: String,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
    val sourceAccount: String,
    val sourceName: String,
    val createdAt: String,
)
