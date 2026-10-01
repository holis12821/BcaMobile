package id.bca.bcamobile.data.qris.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QrisDecodeRequest(
    @SerialName("qr_data") val qrData: String,
)

@Serializable
data class QrisDecodeResponse(
    @SerialName("qris_id") val qrisId: String = "",
    @SerialName("merchant_name") val merchantName: String = "",
    @SerialName("merchant_city") val merchantCity: String = "",
    val amount: Double = 0.0,
    @SerialName("is_amount_fixed") val isAmountFixed: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class QrisPayRequest(
    @SerialName("qris_id") val qrisId: String,
    @SerialName("source_account_id") val sourceAccountId: String,
    val amount: Long,
    @SerialName("verification_token") val verificationToken: String,
)

@Serializable
data class QrisPayResponse(
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("reference_number") val referenceNumber: String = "",
    val status: String = "",
    @SerialName("merchant_name") val merchantName: String = "",
    @SerialName("merchant_city") val merchantCity: String = "",
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("source_account") val sourceAccount: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("created_at") val createdAt: String = "",
)
