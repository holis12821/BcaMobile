package id.bca.bcamobile.data.ewallet.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EWalletProviderDto(
    val id: String = "",
    val name: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("min_amount") val minAmount: Long = 0,
    @SerialName("max_amount") val maxAmount: Long = 0,
    @SerialName("admin_fee") val adminFee: Long = 0,
    @SerialName("preset_amounts") val presetAmounts: List<Long> = emptyList(),
)

@Serializable
data class EWalletProvidersResponse(
    val providers: List<EWalletProviderDto> = emptyList(),
)

@Serializable
data class EWalletInquiryRequest(
    @SerialName("provider_id") val providerId: String,
    @SerialName("phone_number") val phoneNumber: String,
    val amount: Long,
    @SerialName("source_account_id") val sourceAccountId: String,
)

@Serializable
data class EWalletInquiryResponse(
    @SerialName("inquiry_id") val inquiryId: String = "",
    val provider: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    @SerialName("destination_phone") val destinationPhone: String = "",
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("source_account") val sourceAccount: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
)

@Serializable
data class EWalletTopUpRequest(
    @SerialName("inquiry_id") val inquiryId: String,
    @SerialName("verification_token") val verificationToken: String,
)

@Serializable
data class EWalletTopUpResponse(
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("reference_number") val referenceNumber: String = "",
    val status: String = "",
    val provider: String = "",
    @SerialName("destination_phone") val destinationPhone: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("source_account") val sourceAccount: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("created_at") val createdAt: String = "",
)
