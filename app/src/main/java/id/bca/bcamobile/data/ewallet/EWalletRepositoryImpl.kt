package id.bca.bcamobile.data.ewallet

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.IdempotencyKeyProvider
import id.bca.bcamobile.data.ewallet.remote.EWalletApi
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletInquiryRequest
import id.bca.bcamobile.data.ewallet.remote.dto.EWalletTopUpRequest
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.ewallet.EWalletRepository
import id.bca.bcamobile.domain.ewallet.model.EWalletInquiry
import id.bca.bcamobile.domain.ewallet.model.EWalletProvider
import id.bca.bcamobile.domain.ewallet.model.EWalletReceipt
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class EWalletRepositoryImpl @Inject constructor(
    private val api: EWalletApi,
    private val caller: ApiCaller,
    private val idempotencyKeys: IdempotencyKeyProvider,
) : EWalletRepository {

    override suspend fun providers(): DataResult<List<EWalletProvider>> =
        caller.call { api.providers() }.map { dto ->
            dto.providers.map {
                EWalletProvider(
                    id = it.id,
                    name = it.name,
                    iconUrl = it.iconUrl,
                    isActive = it.isActive,
                    minAmount = it.minAmount,
                    maxAmount = it.maxAmount,
                    adminFee = it.adminFee,
                    presetAmounts = it.presetAmounts,
                )
            }
        }

    override suspend fun inquiry(
        providerId: String,
        phoneNumber: String,
        amount: Long,
        sourceAccountId: String,
    ): DataResult<EWalletInquiry> = caller.call(allowRetry = false) {
        api.inquiry(EWalletInquiryRequest(providerId, phoneNumber, amount, sourceAccountId))
    }.map { dto ->
        EWalletInquiry(
            inquiryId = dto.inquiryId,
            provider = dto.provider,
            destinationName = dto.destinationName,
            destinationPhone = dto.destinationPhone,
            amount = dto.amount.roundToLong(),
            adminFee = dto.adminFee.roundToLong(),
            total = dto.total.roundToLong(),
            sourceAccount = dto.sourceAccount,
            expiresInSeconds = dto.expiresIn,
        )
    }

    /** Kunci idempotensi terikat ke `inquiryId`; lihat catatan di TransferRepositoryImpl. */
    override suspend fun topUp(
        inquiryId: String,
        verificationToken: String,
    ): DataResult<EWalletReceipt> {
        val scope = "$SCOPE_PREFIX$inquiryId"
        val result = caller.call(allowRetry = false) {
            api.topUp(
                idempotencyKey = idempotencyKeys.keyFor(scope),
                request = EWalletTopUpRequest(inquiryId, verificationToken),
            )
        }

        val failure = result as? DataResult.Failure
        if (failure == null || !failure.error.isRetryable) {
            idempotencyKeys.release(scope)
        }

        return result.map { dto ->
            EWalletReceipt(
                transactionId = dto.transactionId,
                referenceNumber = dto.referenceNumber,
                status = dto.status,
                provider = dto.provider,
                destinationPhone = dto.destinationPhone,
                destinationName = dto.destinationName,
                amount = dto.amount.roundToLong(),
                adminFee = dto.adminFee.roundToLong(),
                total = dto.total.roundToLong(),
                sourceAccount = dto.sourceAccount,
                sourceName = dto.sourceName,
                createdAt = dto.createdAt,
            )
        }
    }

    private companion object {
        const val SCOPE_PREFIX = "ewallet:"
    }
}
