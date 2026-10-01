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
    /**
     * `PRIORITAS` / `SOLITAIRE`, atau null untuk nasabah reguler — server tidak
     * mengirim field ini untuk mereka. Null berarti **badge disembunyikan**;
     * jangan menggantinya dengan `"REGULER"` lalu membandingkan.
     */
    val tier: String?,
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

/**
 * Batas transaksi harian beserta pemakaian hari ini.
 *
 * Hanya dibalas `PUT /account/transaction-limit`. **Belum ada endpoint GET**-nya di
 * `01-API-SPECIFICATION.md`, jadi layar Atur Limit tidak bisa menampilkan batas
 * yang berlaku sebelum nasabah menyimpan perubahan — dilaporkan ke backend.
 */
data class TransactionLimits(
    val transferInternalDaily: Long,
    val transferExternalDaily: Long,
    val eWalletDaily: Long,
    val transferInternalUsedToday: Long,
    val transferExternalUsedToday: Long,
    val eWalletUsedToday: Long,
)

/**
 * Tantangan OTP untuk mengubah email atau nomor HP.
 *
 * Kode dikirim ke nomor **terdaftar**, bukan ke nomor baru yang diajukan —
 * kalau tidak, pemeriksaannya bisa dipenuhi sendiri oleh penyerang.
 */
data class ProfileOtpChallenge(
    val sentTo: String,
    val expiresInSeconds: Int,
)

/** Nilai yang diubah; null berarti batas itu dibiarkan seperti sebelumnya. */
data class TransactionLimitInput(
    val transferInternalDaily: Long? = null,
    val transferExternalDaily: Long? = null,
    val eWalletDaily: Long? = null,
)
