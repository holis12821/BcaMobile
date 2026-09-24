package id.bca.bcamobile.data.transfer

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.IdempotencyKeyProvider
import id.bca.bcamobile.data.transfer.remote.TransferApi
import id.bca.bcamobile.data.transfer.remote.dto.TransferExecuteRequest
import id.bca.bcamobile.data.transfer.remote.dto.TransferInquiryRequest
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.transfer.TransferRepository
import id.bca.bcamobile.domain.transfer.model.RecentTransfer
import id.bca.bcamobile.domain.transfer.model.TransferInquiry
import id.bca.bcamobile.domain.transfer.model.TransferReceipt
import id.bca.bcamobile.domain.transfer.model.TransferType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val api: TransferApi,
    private val caller: ApiCaller,
    private val idempotencyKeys: IdempotencyKeyProvider,
) : TransferRepository {

    override suspend fun recent(): DataResult<List<RecentTransfer>> =
        caller.call { api.recent() }.map { dto ->
            dto.recents.map {
                RecentTransfer(it.accountNumber, it.accountName, it.bankCode, it.bankName)
            }
        }

    override suspend fun inquiry(
        destinationAccount: String,
        destinationBank: String,
        bankCode: String,
        type: TransferType,
        amount: Long,
        notes: String?,
    ): DataResult<TransferInquiry> = caller.call(allowRetry = false) {
        api.inquiry(
            TransferInquiryRequest(
                destinationAccount = destinationAccount,
                destinationBank = destinationBank,
                bankCode = bankCode,
                transferType = type.wireValue,
                amount = amount,
                notes = notes,
            ),
        )
    }.map { dto ->
        TransferInquiry(
            inquiryId = dto.inquiryId,
            destinationAccount = dto.destinationAccount,
            destinationName = dto.destinationName,
            destinationBank = dto.destinationBank,
            bankCode = dto.bankCode,
            adminFee = dto.adminFee.roundToLong(),
            expiresInSeconds = dto.expiresIn,
        )
    }

    /**
     * Kunci idempotensi terikat ke `inquiryId`, jadi retry karena timeout memakai
     * kunci yang sama dan server tidak membuat transfer kedua. Kunci baru dilepas
     * setelah server memberi jawaban final — sukses maupun ditolak.
     */
    override suspend fun execute(
        inquiryId: String,
        sourceAccountId: String,
        amount: Long,
        notes: String?,
        verificationToken: String,
    ): DataResult<TransferReceipt> {
        val scope = "$SCOPE_PREFIX$inquiryId"
        val result = caller.call(allowRetry = false) {
            api.execute(
                idempotencyKey = idempotencyKeys.keyFor(scope),
                request = TransferExecuteRequest(
                    inquiryId = inquiryId,
                    sourceAccountId = sourceAccountId,
                    amount = amount,
                    notes = notes,
                    verificationToken = verificationToken,
                ),
            )
        }

        val failure = result as? DataResult.Failure
        if (failure == null || !failure.error.isRetryable) {
            idempotencyKeys.release(scope)
        }

        return result.map { dto ->
            TransferReceipt(
                transactionId = dto.transactionId,
                referenceNumber = dto.referenceNumber,
                status = dto.status,
                amount = dto.amount.roundToLong(),
                adminFee = dto.adminFee.roundToLong(),
                total = dto.total.roundToLong(),
                sourceAccount = dto.source?.accountNumber.orEmpty(),
                sourceName = dto.source?.name.orEmpty(),
                destinationAccount = dto.destination?.accountNumber.orEmpty(),
                destinationName = dto.destination?.name.orEmpty(),
                destinationBank = dto.destination?.bank.orEmpty(),
                notes = dto.notes,
                createdAt = dto.createdAt,
            )
        }
    }

    private companion object {
        const val SCOPE_PREFIX = "transfer:"
    }
}
