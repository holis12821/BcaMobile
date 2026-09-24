package id.bca.bcamobile.domain.transfer

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.transfer.model.RecentTransfer
import id.bca.bcamobile.domain.transfer.model.TransferInquiry
import id.bca.bcamobile.domain.transfer.model.TransferReceipt
import id.bca.bcamobile.domain.transfer.model.TransferType

/**
 * Transfer tiga langkah: inquiry -> verifikasi PIN -> execute.
 * Kontrak: `docs/backend/01-API-SPECIFICATION.md` §5.
 *
 * `verificationToken` didapat dari `AuthRepository.verifyPin` dan hanya berlaku
 * 120 detik. Implementasi yang mengurus idempotency key, jadi retry [execute]
 * untuk inquiry yang sama tidak akan membuat transfer kedua.
 */
interface TransferRepository {

    suspend fun recent(): DataResult<List<RecentTransfer>>

    suspend fun inquiry(
        destinationAccount: String,
        destinationBank: String,
        bankCode: String,
        type: TransferType,
        amount: Long,
        notes: String?,
    ): DataResult<TransferInquiry>

    suspend fun execute(
        inquiryId: String,
        sourceAccountId: String,
        amount: Long,
        notes: String?,
        verificationToken: String,
    ): DataResult<TransferReceipt>
}
