package id.bca.bcamobile.domain.account.model

/**
 * Nominal disimpan sebagai rupiah bulat. Server mengirim desimal (`15750000.00`)
 * tetapi rupiah tidak memakai sen, jadi dibulatkan sekali di mapper.
 */
data class BankAccount(
    val id: String,
    val number: String,
    val type: String,
    val label: String,
    val isPrimary: Boolean,
)

data class AccountBalance(
    val accountId: String,
    val accountNumber: String,
    val balance: Long,
    val availableBalance: Long,
    val holdAmount: Long,
    val currency: String,
)

data class BalanceSummary(
    val accounts: List<AccountBalance>,
    val totalBalance: Long,
)

data class UserProfile(
    val userId: String,
    val fullName: String,
    val displayName: String,
    val phoneNumber: String,
    val email: String,
    val isBiometricEnabled: Boolean,
    val accounts: List<BankAccount>,
)

data class Promotion(
    val id: String,
    val title: String,
    val imageUrl: String,
    val deepLink: String?,
)

data class QuickAction(
    val id: String,
    val label: String,
    val enabled: Boolean,
)

data class Dashboard(
    val displayName: String,
    val maskedAccount: String,
    val totalBalance: Long,
    val primaryAccount: String,
    val unreadNotifications: Int,
    val promotions: List<Promotion>,
    val quickActions: List<QuickAction>,
)
