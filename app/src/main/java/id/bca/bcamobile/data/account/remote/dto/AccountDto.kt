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

/**
 * Balasan `GET /account/profile`.
 *
 * Nama field mengikuti **yang benar-benar dikirim handler backend**
 * (`account_handler.go` §Profile), bukan contoh di spec:
 * `id` bukan `user_id`, `phone` bukan `phone_number`, `last_login_at` bukan
 * `last_login`. Sebelumnya ketiganya meleset, jadi id dan nomor HP selalu kosong
 * di layar Profil Saya.
 *
 * `is_biometric_enabled` ada di spec tapi **tidak dikirim** handler. Selama itu
 * belum diperbaiki di backend, keadaan awal sakelar biometrik tidak bisa dimuat
 * dan field ini tetap `false`.
 *
 * `tier` **absen untuk nasabah REGULER** — handler sengaja tidak mengirimnya
 * (`account_handler.go`: `if profile.Tier != "" && profile.Tier != TierReguler`).
 * Karena itu nullable dan **tidak** di-default ke `"REGULER"`: absen berarti
 * tanpa badge, bukan badge bertuliskan Reguler.
 */
@Serializable
data class ProfileResponse(
    @SerialName("id") val userId: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("phone") val phoneNumber: String = "",
    val email: String = "",
    @SerialName("is_biometric_enabled") val isBiometricEnabled: Boolean = false,
    @SerialName("biometric_type") val biometricType: String? = null,
    /** `PRIORITAS` atau `SOLITAIRE`; absen untuk `REGULER`. */
    val tier: String? = null,
    val accounts: List<AccountSummaryDto> = emptyList(),
    @SerialName("last_login_at") val lastLogin: String? = null,
)

/**
 * Balasan `POST /account/profile/otp`. Request-nya tanpa body.
 *
 * `otp_debug` hanya terisi saat `APP_ENV=development` — gateway SMS di sana
 * berupa stub. Jangan pernah menampilkannya di build release.
 */
@Serializable
data class ProfileOtpResponse(
    @SerialName("sent_to") val sentTo: String = "",
    @SerialName("expires_in") val expiresInSeconds: Int = 0,
    @SerialName("otp_debug") val otpDebug: String? = null,
)

/**
 * Body `POST /account/device/push-token`.
 *
 * Hanya `push_token`. Identitas perangkat diambil backend dari klaim `did` di
 * access token, bukan dari body — client tidak boleh menempelkan token ke
 * perangkat lain.
 */
@Serializable
data class PushTokenRequest(
    @SerialName("push_token") val pushToken: String,
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

/**
 * Bentuk request mengikuti `bca-mobile-api/docs/01-API-SPECIFICATION.md` §5:
 * `verification_token` di akar, batas harian dibungkus objek `limits`.
 *
 * Client sebelumnya mengirim `account_id` + `transfer_limit` + `ewallet_limit`,
 * dan skill `bca-mobile-api` menyebut bentuk ketiga (`limits.TRANSFER_INTERNAL.daily_limit`).
 * Spec yang dipakai; selisihnya dilaporkan.
 */
@Serializable
data class UpdateLimitRequest(
    @SerialName("verification_token") val verificationToken: String,
    val limits: LimitValuesDto,
)

@Serializable
data class LimitValuesDto(
    @SerialName("transfer_internal_daily") val transferInternalDaily: Long? = null,
    @SerialName("transfer_external_daily") val transferExternalDaily: Long? = null,
    @SerialName("ewallet_daily") val ewalletDaily: Long? = null,
)

/**
 * Balasan `GET` **dan** `PUT /account/transaction-limit`.
 *
 * Backend (`LimitStatusResponse`) mengirim `limits` sebagai **peta
 * string→string berisi desimal**, kuncinya dirakit dari `limit_type` yang
 * di-lowercase plus akhiran:
 *
 * ```json
 * { "limits": {
 *     "transfer_internal_daily": "50000000.00",
 *     "transfer_internal_used_today": "1250000.00",
 *     "transfer_internal_remaining_today": "48750000.00",
 *     "transfer_internal_per_transaction": "25000000.00"
 * } }
 * ```
 *
 * Client sebelumnya memodelkannya sebagai objek berisi `Long`. Nilai seperti
 * `"50000000.00"` gagal diurai menjadi `Long`, jadi penyimpanan batas yang
 * **berhasil** di server tetap terbaca sebagai kegagalan di layar.
 */
@Serializable
data class LimitStatusResponse(
    val limits: Map<String, String> = emptyMap(),
)

/**
 * Body `PUT /account/settings`.
 *
 * Nama field **wajib** `biometric_enabled` / `push_notification_enabled` /
 * `email_statement_enabled`. Ketiganya `*bool` di backend; nama yang tidak cocok
 * terurai jadi `nil`, dan `Service.UpdateSettings` membalas `VALIDATION_ERROR`
 * kalau ketiganya `nil`.
 *
 * Client sebelumnya mengirim `is_biometric_enabled` dan `notification_enabled`
 * — nama dari response `GET /account/profile`, bukan dari body endpoint ini —
 * sehingga kedua sakelar di layar Profil Saya selalu gagal dan membalik sendiri.
 *
 * `encodeDefaults = true` di `NetworkModule`, tapi `explicitNulls = false`
 * membuang field bernilai null, jadi hanya sakelar yang diubah yang terkirim.
 */
@Serializable
data class SettingsRequest(
    @SerialName("biometric_enabled") val biometricEnabled: Boolean? = null,
    @SerialName("push_notification_enabled") val pushNotificationEnabled: Boolean? = null,
    @SerialName("email_statement_enabled") val emailStatementEnabled: Boolean? = null,
)
