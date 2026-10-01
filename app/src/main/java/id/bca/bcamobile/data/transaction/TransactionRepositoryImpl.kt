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
import id.bca.bcamobile.domain.transaction.model.MutationType
import id.bca.bcamobile.domain.transaction.model.Page
import id.bca.bcamobile.domain.transaction.model.Receipt
import id.bca.bcamobile.domain.transaction.model.TransactionPeriod
import retrofit2.Response
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
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
        period: TransactionPeriod,
        startDate: String?,
        endDate: String?,
        cursor: String?,
    ): DataResult<MutationPage> {
        // Cursor ada di envelope, bukan di data, jadi respons mentah dibaca dua kali.
        var nextCursor: String? = null
        val result = caller.call {
            // `resolvePeriod` mengenal CUSTOM dan mewajibkan from+to saat
            // dipilih, jadi nilainya dikirim apa adanya. Sebelumnya client
            // mengosongkan period untuk rentang kustom — masih diterima server
            // (cabang `case ""`), tapi menyamarkan rentang tak lengkap sebagai
            // "tanpa filter" alih-alih VALIDATION_ERROR yang jelas.
            val isCustom = period == TransactionPeriod.CUSTOM
            api.mutations(
                accountId = accountId,
                period = period.wireValue,
                from = if (isCustom) startDate else null,
                to = if (isCustom) endDate else null,
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
        period: TransactionPeriod?,
        startDate: String?,
        endDate: String?,
        cursor: String?,
    ): DataResult<Page<HistoryItem>> {
        var nextCursor: String? = null
        val result = caller.call {
            val isCustom = period == TransactionPeriod.CUSTOM
            api.history(
                // null = semua jenis. "ALL" akan disaring apa adanya oleh
                // server dan mengembalikan daftar kosong.
                type = type,
                period = period?.wireValue,
                from = if (isCustom) startDate else null,
                to = if (isCustom) endDate else null,
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

    /**
     * Tidak lewat [ApiCaller]: balasannya berkas, bukan `ApiEnvelope`, jadi
     * tidak ada `status`/`error` untuk diklasifikasi. Retry juga tidak dipasang
     * — mengunduh ulang struk yang gagal adalah keputusan pengguna, bukan
     * keputusan diam-diam yang menggandakan lalu lintas berkas.
     */
    override suspend fun receiptPdf(transactionId: String): DataResult<ByteArray> =
        withContext(Dispatchers.IO) {
            val response = runCatching { api.receiptPdf(transactionId) }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                return@withContext DataResult.Failure(
                    when (throwable) {
                        is SocketTimeoutException -> ApiFailure.Timeout
                        is IOException -> ApiFailure.Network
                        else -> ApiFailure.Unknown
                    },
                )
            }

            val body = response.body()
            when {
                !response.isSuccessful -> DataResult.Failure(
                    when (response.code()) {
                        401 -> ApiFailure.Unauthorized
                        in 500..599 -> ApiFailure.Server
                        else -> ApiFailure.Unknown
                    },
                )

                body == null -> DataResult.Failure(ApiFailure.Unknown)
                else -> runCatching { body.use { it.bytes() } }
                    .fold(
                        onSuccess = { DataResult.Success(it) },
                        onFailure = { DataResult.Failure(ApiFailure.Network) },
                    )
            }
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
