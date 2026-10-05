package id.bca.bcamobile.domain.ewallet

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.ewallet.model.EWalletInquiry
import id.bca.bcamobile.domain.ewallet.model.EWalletProvider
import id.bca.bcamobile.domain.ewallet.model.EWalletReceipt

/**
 * Top up e-wallet: providers -> inquiry -> verifikasi PIN -> topup.
 * Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §6.
 */
interface EWalletRepository {

    suspend fun providers(): DataResult<List<EWalletProvider>>

    suspend fun inquiry(
        providerId: String,
        phoneNumber: String,
        amount: Long,
        sourceAccountId: String,
    ): DataResult<EWalletInquiry>

    suspend fun topUp(
        inquiryId: String,
        verificationToken: String,
    ): DataResult<EWalletReceipt>
}
