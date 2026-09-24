package id.bca.bcamobile.data.transaction

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.data.transaction.remote.TransactionApi
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.transaction.TransactionRepository
import id.bca.bcamobile.domain.transaction.model.HistoryItem
import id.bca.bcamobile.domain.transaction.model.Mutation
import id.bca.bcamobile.domain.transaction.model.MutationPage
import id.bca.bcamobile.domain.transaction.model.MutationPeriod
import id.bca.bcamobile.domain.transaction.model.MutationType
import id.bca.bcamobile.domain.transaction.model.Page
import id.bca.bcamobile.domain.transaction.model.Receipt
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToLong

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: TransactionApi,
    private val caller: ApiCaller,
) : TransactionRepository {

    override suspend fun mutations(
        accountId: String,
        period: MutationPeriod,
        startDate: String?,
        endDate: String?,
        cursor: String?,
    ): DataResult<MutationPage> {
        // Cursor ada di envelope, bukan di data, jadi respons mentah dibaca dua kali.
        var nextCursor: String? = null
        val result = caller.call {
            api.mutations(
                accountId = accountId,
                period = period.wireValue,
                startDate = startDate,
                endDate = endDate,
                cursor = cursor,
            ).also { nextCursor = it.nextCursor() }
        }
        return result.map { dto ->
            MutationPage(
                accountNumber = dto.account?.accountNumber.orEmpty(),
                accountLabel = dto.account?.accountLabel.orEmpty(),
                balance = dto.account?.balance.toRupiah(),
                page = Page(
                    items = dto.transactions.map {
                        Mutation(
                            id = it.id,
                            date = it.date,
                            time = it.time,
                            description = it.description,
                            detail = it.detail,
                            amount = abs(it.amount).roundToLong(),
                            balanceAfter = it.balanceAfter.toRupiah(),
                            type = it.type.toMutationType(),
                            category = it.category,
                            referenceNumber = it.referenceNumber,
                        )
                    },
                    nextCursor = nextCursor,
                ),
            )
        }
    }

    override suspend fun history(
        type: String?,
        cursor: String?,
    ): DataResult<Page<HistoryItem>> {
        var nextCursor: String? = null
        val result = caller.call {
            api.history(
                type = type ?: TransactionApi.TYPE_ALL,
                cursor = cursor,
            ).also { nextCursor = it.nextCursor() }
        }
        return result.map { dto ->
            Page(
                items = dto.transactions.map {
                    HistoryItem(
                        id = it.id,
                        type = it.type,
                        status = it.status,
                        amount = it.amount.toRupiah(),
                        adminFee = it.adminFee.toRupiah(),
                        total = it.total.toRupiah(),
                        description = it.description,
                        destination = it.destination,
                        destinationName = it.destinationName,
                        referenceNumber = it.referenceNumber,
                        createdAt = it.createdAt,
                    )
                },
                nextCursor = nextCursor,
            )
        }
    }

    override suspend fun receipt(transactionId: String): DataResult<Receipt> =
        caller.call(notFoundAs = ApiFailure.Unknown) { api.receipt(transactionId) }.map { dto ->
            Receipt(
                transactionId = dto.transactionId,
                type = dto.type,
                status = dto.status,
                date = dto.date,
                time = dto.time,
                referenceNumber = dto.referenceNumber,
                sourceAccount = dto.sourceAccount,
                sourceName = dto.sourceName,
                destinationNumber = dto.destinationNumber,
                destinationName = dto.destinationName,
                provider = dto.provider,
                amount = dto.amount.toRupiah(),
                adminFee = dto.adminFee.toRupiah(),
                total = dto.total.toRupiah(),
            )
        }
}

private fun <T> Response<ApiEnvelope<T>>.nextCursor(): String? =
    body()?.pagination?.takeIf { it.hasMore }?.cursor

private fun String.toMutationType(): MutationType = when (uppercase()) {
    "DEBIT" -> MutationType.DEBIT
    "CREDIT" -> MutationType.CREDIT
    else -> MutationType.UNKNOWN
}

private fun Double?.toRupiah(): Long = this?.roundToLong() ?: 0L
