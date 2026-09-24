package id.bca.bcamobile.data.account

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.data.account.remote.AccountApi
import id.bca.bcamobile.data.account.remote.dto.SettingsRequest
import id.bca.bcamobile.data.account.remote.dto.UpdateLimitRequest
import id.bca.bcamobile.domain.account.AccountRepository
import id.bca.bcamobile.domain.account.model.AccountBalance
import id.bca.bcamobile.domain.account.model.BalanceSummary
import id.bca.bcamobile.domain.account.model.BankAccount
import id.bca.bcamobile.domain.account.model.Dashboard
import id.bca.bcamobile.domain.account.model.Promotion
import id.bca.bcamobile.domain.account.model.QuickAction
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

    override suspend fun updateTransactionLimit(
        accountId: String,
        transferLimit: Long?,
        ewalletLimit: Long?,
    ): DataResult<Unit> = caller.call(allowRetry = false) {
        api.updateTransactionLimit(UpdateLimitRequest(accountId, transferLimit, ewalletLimit))
    }.map { }

    override suspend fun updateBiometricSetting(enabled: Boolean): DataResult<Unit> =
        caller.call(allowRetry = false) {
            api.updateSettings(SettingsRequest(isBiometricEnabled = enabled))
        }.map { }
}

/** Rupiah tidak memakai sen; pembulatan dilakukan sekali di batas data. */
private fun Double?.toRupiah(): Long = this?.roundToLong() ?: 0L
