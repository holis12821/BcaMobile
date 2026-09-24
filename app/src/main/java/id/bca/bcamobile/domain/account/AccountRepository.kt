package id.bca.bcamobile.domain.account

import id.bca.bcamobile.domain.account.model.BalanceSummary
import id.bca.bcamobile.domain.account.model.Dashboard
import id.bca.bcamobile.domain.account.model.UserProfile
import id.bca.bcamobile.domain.common.DataResult

/** Rekening dan profil. Kontrak: `docs/backend/01-API-SPECIFICATION.md` §3. */
interface AccountRepository {

    suspend fun dashboard(): DataResult<Dashboard>

    /** Dipanggil hanya saat pengguna membuka saldo — jangan dimuat di latar. */
    suspend fun balance(): DataResult<BalanceSummary>

    suspend fun profile(): DataResult<UserProfile>

    suspend fun updateTransactionLimit(
        accountId: String,
        transferLimit: Long?,
        ewalletLimit: Long?,
    ): DataResult<Unit>

    suspend fun updateBiometricSetting(enabled: Boolean): DataResult<Unit>
}
