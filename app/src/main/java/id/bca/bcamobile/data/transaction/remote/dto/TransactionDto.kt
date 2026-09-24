package id.bca.bcamobile.data.transaction.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MutationAccountDto(
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_label") val accountLabel: String = "",
    val balance: Double = 0.0,
)

@Serializable
data class MutationDto(
    val id: String = "",
    val date: String = "",
    val time: String = "",
    val description: String = "",
    val detail: String = "",
    /** Negatif untuk DEBIT, positif untuk CREDIT. */
    val amount: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val icon: String? = null,
    @SerialName("reference_number") val referenceNumber: String = "",
)

@Serializable
data class MutationsResponse(
    val account: MutationAccountDto? = null,
    val transactions: List<MutationDto> = emptyList(),
)

@Serializable
data class HistoryItemDto(
    val id: String = "",
    val type: String = "",
    val status: String = "",
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    val description: String = "",
    val destination: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    @SerialName("reference_number") val referenceNumber: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val icon: String? = null,
)

@Serializable
data class HistoryResponse(
    val transactions: List<HistoryItemDto> = emptyList(),
)

@Serializable
data class ReceiptResponse(
    @SerialName("transaction_id") val transactionId: String = "",
    val type: String = "",
    val status: String = "",
    val date: String = "",
    val time: String = "",
    @SerialName("reference_number") val referenceNumber: String = "",
    @SerialName("source_account") val sourceAccount: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("destination_number") val destinationNumber: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    val provider: String? = null,
    val amount: Double = 0.0,
    @SerialName("admin_fee") val adminFee: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "IDR",
    @SerialName("receipt_url") val receiptUrl: String? = null,
)
