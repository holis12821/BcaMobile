package id.bca.bcamobile.data.transfer.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentTransferDto(
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_name") val accountName: String = "",
    @SerialName("bank_code") val bankCode: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("last_transfer_at") val lastTransferAt: String? = null,
)

@Serializable
data class RecentTransferResponse(
    val recents: List<RecentTransferDto> = emptyList(),
)

@Serializable
data class TransferInquiryRequest(
    @SerialName("destination_account") val destinationAccount: String,
    @SerialName("destination_bank") val destinationBank: String,
    @SerialName("bank_code") val bankCode: String,
    @SerialName("transfer_type") val transferType: String,
    val amount: Long,
    val notes: String? = null,
)

@Serializable
data class TransferInquiryResponse(
    @SerialName("inquiry_id") val inquiryId: String = "",
    @SerialName("destination_account") val destinationAccount: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    @SerialName("destination_bank") val destinationBank: String = "",
    @SerialName("bank_code") val bankCode: String = "",
    @SerialName("transfer_type") val transferType: String = "",
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    /** Detik sampai hasil inquiry kedaluwarsa; kontrak menyebut 5 menit. */
    @SerialName("expires_in") val expiresIn: Int = 0,
)

@Serializable
data class TransferExecuteRequest(
    @SerialName("inquiry_id") val inquiryId: String,
    @SerialName("source_account_id") val sourceAccountId: String,
    val amount: Long,
    val notes: String? = null,
    @SerialName("verification_token") val verificationToken: String,
)

@Serializable
data class TransferPartyDto(
    @SerialName("account_number") val accountNumber: String = "",
    val name: String = "",
    val bank: String? = null,
)

@Serializable
data class TransferExecuteResponse(
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("reference_number") val referenceNumber: String = "",
    val status: String = "",
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    val source: TransferPartyDto? = null,
    val destination: TransferPartyDto? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
