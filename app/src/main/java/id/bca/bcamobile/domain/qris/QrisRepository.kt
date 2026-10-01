package id.bca.bcamobile.domain.qris

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.qris.model.QrisPayload
import id.bca.bcamobile.domain.qris.model.QrisReceipt

/**
 * Pembayaran QRIS.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §8.
 *
 * Urutannya sama dengan transaksi lain: decode → verifikasi PIN → bayar.
 */
interface QrisRepository {

    /** [qrData] adalah payload EMVCo apa adanya dari pemindai. */
    suspend fun decode(qrData: String): DataResult<QrisPayload>

    /**
     * Idempotency key terikat ke [qrisId] dan dilepas setelah jawaban final,
     * supaya retry karena timeout tidak menghasilkan dua pembayaran.
     */
    suspend fun pay(
        qrisId: String,
        sourceAccountId: String,
        amount: Long,
        verificationToken: String,
    ): DataResult<QrisReceipt>
}
