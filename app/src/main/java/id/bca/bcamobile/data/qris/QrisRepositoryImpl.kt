package id.bca.bcamobile.data.qris

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.core.network.IdempotencyKeyProvider
import id.bca.bcamobile.data.qris.remote.QrisApi
import id.bca.bcamobile.data.qris.remote.dto.QrisDecodeRequest
import id.bca.bcamobile.data.qris.remote.dto.QrisPayRequest
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.qris.QrisRepository
import id.bca.bcamobile.domain.qris.model.QrisPayload
import id.bca.bcamobile.domain.qris.model.QrisReceipt
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class QrisRepositoryImpl @Inject constructor(
    private val api: QrisApi,
    private val caller: ApiCaller,
    private val idempotencyKeys: IdempotencyKeyProvider,
) : QrisRepository {

    override suspend fun decode(qrData: String): DataResult<QrisPayload> =
        // Tidak diulang otomatis: QR dinamis punya masa berlaku pendek, dan
        // pemindaian ulang lebih murah daripada menahan layar.
        caller.call(allowRetry = false) { api.decode(QrisDecodeRequest(qrData = qrData)) }
            .map { dto ->
                QrisPayload(
                    qrisId = dto.qrisId,
                    merchantName = dto.merchantName,
                    merchantCity = dto.merchantCity,
                    amount = dto.amount.roundToLong(),
                    isAmountFixed = dto.isAmountFixed,
                    expiresAt = dto.expiresAt?.takeIf(String::isNotBlank),
                )
            }

    override suspend fun pay(
        qrisId: String,
        sourceAccountId: String,
        amount: Long,
        verificationToken: String,
    ): DataResult<QrisReceipt> {
        val scope = "$SCOPE_PREFIX$qrisId"
        val result = caller.call(allowRetry = false) {
            api.pay(
                idempotencyKey = idempotencyKeys.keyFor(scope),
                request = QrisPayRequest(
                    qrisId = qrisId,
                    sourceAccountId = sourceAccountId,
                    amount = amount,
                    verificationToken = verificationToken,
                ),
            )
        }

        // Kunci hanya dilepas setelah jawaban final. Kegagalan yang masih layak
        // di-retry harus memakai kunci yang sama, kalau tidak nasabah bisa terbayar dua kali.
        val failure = result as? DataResult.Failure
        if (failure == null || !failure.error.isRetryable) {
            idempotencyKeys.release(scope)
        }

        return result.map { dto ->
            QrisReceipt(
                transactionId = dto.transactionId,
                referenceNumber = dto.referenceNumber,
                status = dto.status,
                merchantName = dto.merchantName,
                merchantCity = dto.merchantCity,
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
        const val SCOPE_PREFIX = "qris:"
    }
}
