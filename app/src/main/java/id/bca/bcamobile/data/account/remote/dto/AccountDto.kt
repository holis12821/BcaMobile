package id.bca.bcamobile.data.account.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Nominal dikirim server sebagai desimal (15750000.00), jadi DTO memakai Double.
// Konversi ke rupiah bulat terjadi di mapper domain.

@Serializable
data class AccountSummaryDto(
    @SerialName("account_id") val accountId: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_type") val accountType: String = "",
    @SerialName("account_label") val accountLabel: String = "",
    @SerialName("is_primary") val isPrimary: Boolean = false,
)

@Serializable
data class ProfileResponse(
    @SerialName("user_id") val userId: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    val email: String = "",
    @SerialName("is_biometric_enabled") val isBiometricEnabled: Boolean = false,
    @SerialName("biometric_type") val biometricType: String? = null,
    val accounts: List<AccountSummaryDto> = emptyList(),
    @SerialName("last_login") val lastLogin: String? = null,
)

@Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("otp_code") val otpCode: String,
)

@Serializable
data class BalanceAccountDto(
    @SerialName("account_id") val accountId: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("account_type") val accountType: String = "",
    val balance: Double = 0.0,
    val currency: String = "IDR",
    @SerialName("available_balance") val availableBalance: Double = 0.0,
    @SerialName("hold_amount") val holdAmount: Double = 0.0,
)

@Serializable
data class BalanceResponse(
    val accounts: List<BalanceAccountDto> = emptyList(),
    @SerialName("total_balance") val totalBalance: Double = 0.0,
)

@Serializable
data class DashboardUserDto(
    @SerialName("display_name") val displayName: String = "",
    @SerialName("masked_account") val maskedAccount: String = "",
)

@Serializable
data class DashboardBalanceDto(
    val total: Double = 0.0,
    val currency: String = "IDR",
    @SerialName("primary_account") val primaryAccount: String = "",
)

@Serializable
data class PromotionDto(
    val id: String = "",
    val title: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("deep_link") val deepLink: String? = null,
    @SerialName("valid_until") val validUntil: String? = null,
)

@Serializable
data class QuickActionDto(
    val id: String = "",
    val label: String = "",
    val icon: String = "",
    val enabled: Boolean = true,
)

@Serializable
data class DashboardResponse(
    val user: DashboardUserDto? = null,
    val balance: DashboardBalanceDto? = null,
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
    val promotions: List<PromotionDto> = emptyList(),
    @SerialName("quick_actions") val quickActions: List<QuickActionDto> = emptyList(),
)

@Serializable
data class UpdateLimitRequest(
    @SerialName("account_id") val accountId: String,
    @SerialName("transfer_limit") val transferLimit: Long? = null,
    @SerialName("ewallet_limit") val ewalletLimit: Long? = null,
)

@Serializable
data class SettingsRequest(
    @SerialName("is_biometric_enabled") val isBiometricEnabled: Boolean? = null,
    @SerialName("notification_enabled") val notificationEnabled: Boolean? = null,
)
