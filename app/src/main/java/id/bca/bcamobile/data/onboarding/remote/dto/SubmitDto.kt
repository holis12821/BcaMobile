package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("agreement_accepted") val agreementAccepted: Boolean,
    @SerialName("agreement_version") val agreementVersion: String,
)

@Serializable
data class AccountDto(
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_type") val accountType: String = "",
    @SerialName("account_holder") val accountHolder: String = "",
    val branch: String = "",
    @SerialName("branch_code") val branchCode: String = "",
    val currency: String = "",
    val status: String = "",
    @SerialName("min_initial_deposit") val minInitialDeposit: Long = 0,
    @SerialName("initial_deposit_deadline") val initialDepositDeadline: String? = null,
)

@Serializable
data class MBcaDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("access_code_set") val accessCodeSet: Boolean = false,
    @SerialName("pin_set") val pinSet: Boolean = false,
)

@Serializable
data class SubmitResponse(
    val account: AccountDto,
    @SerialName("m_bca") val mBca: MBcaDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
