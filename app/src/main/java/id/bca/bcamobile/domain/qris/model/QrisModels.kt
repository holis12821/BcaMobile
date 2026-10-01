package id.bca.bcamobile.domain.qris.model

/**
 * Hasil decode QR EMVCo oleh server.
 *
 * Client **tidak** mengurai payload QRIS sendiri: nominal, nama merchant, dan masa
 * berlaku semuanya ditentukan server. [isAmountFixed] menentukan apakah nasabah
 * boleh mengisi nominal — QR statis membolehkannya, QR dinamis tidak.
 */
data class QrisPayload(
    val qrisId: String,
    val merchantName: String,
    val merchantCity: String,
    val amount: Long,
    val isAmountFixed: Boolean,
    val expiresAt: String?,
)

data class QrisReceipt(
    val transactionId: String,
    val referenceNumber: String,
    val status: String,
    val merchantName: String,
    val merchantCity: String,
    val amount: Long,
    val adminFee: Long,
    val total: Long,
    val sourceAccount: String,
    val sourceName: String,
    val createdAt: String,
)
