package id.bca.bcamobile.data.account

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.data.account.remote.AccountApi
import id.bca.bcamobile.data.account.remote.dto.SettingsRequest
import id.bca.bcamobile.data.account.remote.dto.LimitValuesDto
import id.bca.bcamobile.data.account.remote.dto.PushTokenRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateLimitRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateProfileRequest
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.account.model.AccountBalance
import id.bca.bcamobile.domain.account.model.BalanceSummary
import id.bca.bcamobile.domain.account.model.BankAccount
import id.bca.bcamobile.domain.account.model.Dashboard
import id.bca.bcamobile.domain.account.model.ProfileOtpChallenge
import id.bca.bcamobile.domain.account.model.Promotion
import id.bca.bcamobile.domain.account.model.QuickAction
import id.bca.bcamobile.domain.account.model.TransactionLimitInput
import id.bca.bcamobile.domain.account.model.TransactionLimits
import id.bca.bcamobile.domain.account.model.UserProfile
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: AccountApi,
    private val caller: ApiCaller,
) : AccountRepository {

    override suspend fun dashboard(): DataResult<Dashboard> =
        caller.call { api.dashboard() }.map { dto ->
            Dashboard(
                displayName = dto.user?.displayName.orEmpty(),
                maskedAccount = dto.user?.maskedAccount.orEmpty(),
                totalBalance = dto.balance?.total.toRupiah(),
                primaryAccount = dto.balance?.primaryAccount.orEmpty(),
                unreadNotifications = dto.unreadNotifications,
                promotions = dto.promotions.map {
                    Promotion(it.id, it.title, it.imageUrl, it.deepLink)
                },
                quickActions = dto.quickActions.map {
                    QuickAction(it.id, it.label, it.enabled)
                },
            )
        }

    override suspend fun balance(): DataResult<BalanceSummary> =
        caller.call { api.balance() }.map { dto ->
            BalanceSummary(
                accounts = dto.accounts.map {
                    AccountBalance(
                        accountId = it.accountId,
                        accountNumber = it.accountNumber,
                        balance = it.balance.toRupiah(),
                        availableBalance = it.availableBalance.toRupiah(),
                        holdAmount = it.holdAmount.toRupiah(),
                        currency = it.currency,
                    )
                },
                totalBalance = dto.totalBalance.toRupiah(),
            )
        }

    override suspend fun profile(): DataResult<UserProfile> =
        caller.call { api.profile() }.map { dto ->
            UserProfile(
                userId = dto.userId,
                fullName = dto.fullName,
                displayName = dto.displayName,
                phoneNumber = dto.phoneNumber,
                email = dto.email,
                isBiometricEnabled = dto.isBiometricEnabled,
                // Kosong diperlakukan sama dengan absen: keduanya berarti
                // tidak ada badge tier untuk ditampilkan.
                tier = dto.tier?.takeIf { it.isNotBlank() },
                accounts = dto.accounts.map {
                    BankAccount(
                        id = it.accountId,
                        number = it.accountNumber,
                        type = it.accountType,
                        label = it.accountLabel,
                        isPrimary = it.isPrimary,
                    )
                },
            )
        }

    override suspend fun transactionLimits(): DataResult<TransactionLimits> =
        caller.call { api.transactionLimits() }.map { dto -> dto.limits.toDomain() }

    override suspend fun updateTransactionLimit(
        limits: TransactionLimitInput,
        verificationToken: String,
    ): DataResult<TransactionLimits> = caller.call(allowRetry = false) {
        api.updateTransactionLimit(
            UpdateLimitRequest(
                verificationToken = verificationToken,
                limits = LimitValuesDto(
                    transferInternalDaily = limits.transferInternalDaily,
                    transferExternalDaily = limits.transferExternalDaily,
                    ewalletDaily = limits.eWalletDaily,
                ),
            ),
        )
    }.map { dto -> dto.limits.toDomain() }

    override suspend fun requestProfileOtp(): DataResult<ProfileOtpChallenge> =
        caller.call(allowRetry = false) { api.requestProfileOtp() }.map { dto ->
            ProfileOtpChallenge(
                sentTo = dto.sentTo,
                expiresInSeconds = dto.expiresInSeconds,
            )
        }

    override suspend fun updateProfile(
        email: String?,
        phoneNumber: String?,
        otpCode: String,
    ): DataResult<Unit> = caller.call(allowRetry = false) {
        api.updateProfile(
            UpdateProfileRequest(
                email = email,
                phoneNumber = phoneNumber,
                otpCode = otpCode,
            ),
        )
    }.map { }

    override suspend fun registerPushToken(pushToken: String): DataResult<Unit> =
        caller.call(allowRetry = false) {
            api.registerPushToken(PushTokenRequest(pushToken = pushToken))
        }.map { }

    override suspend fun updateBiometricSetting(enabled: Boolean): DataResult<Unit> =
        caller.call(allowRetry = false) {
            api.updateSettings(SettingsRequest(biometricEnabled = enabled))
        }.map { }

    override suspend fun updateNotificationSetting(enabled: Boolean): DataResult<Unit> =
        caller.call(allowRetry = false) {
            api.updateSettings(SettingsRequest(pushNotificationEnabled = enabled))
        }.map { }

    override suspend fun updateEmailStatementSetting(enabled: Boolean): DataResult<Unit> =
        caller.call(allowRetry = false) {
            api.updateSettings(SettingsRequest(emailStatementEnabled = enabled))
        }.map { }
}

/** Rupiah tidak memakai sen; pembulatan dilakukan sekali di batas data. */
private fun Double?.toRupiah(): Long = this?.roundToLong() ?: 0L

/**
 * Peta `limits` dari server berisi desimal dalam bentuk string
 * (`"50000000.00"`), dengan kunci `<limit_type lowercase>_<akhiran>`.
 *
 * Kunci yang tidak dikenali diabaikan, bukan dianggap error: server boleh
 * menambah jenis limit baru tanpa memecah aplikasi lama.
 */
private fun Map<String, String>.toDomain(): TransactionLimits = TransactionLimits(
    transferInternalDaily = rupiah("transfer_internal_daily"),
    transferExternalDaily = rupiah("transfer_external_daily"),
    eWalletDaily = rupiah("ewallet_daily"),
    transferInternalUsedToday = rupiah("transfer_internal_used_today"),
    transferExternalUsedToday = rupiah("transfer_external_used_today"),
    eWalletUsedToday = rupiah("ewallet_used_today"),
)

private fun Map<String, String>.rupiah(key: String): Long =
    this[key]?.toDoubleOrNull()?.roundToLong() ?: 0L
