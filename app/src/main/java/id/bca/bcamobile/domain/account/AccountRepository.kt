package id.bca.bcamobile.domain.account

import id.bca.bcamobile.domain.account.model.BalanceSummary
import id.bca.bcamobile.domain.account.model.Dashboard
import id.bca.bcamobile.domain.account.model.ProfileOtpChallenge
import id.bca.bcamobile.domain.account.model.TransactionLimitInput
import id.bca.bcamobile.domain.account.model.TransactionLimits
import id.bca.bcamobile.domain.account.model.UserProfile
import id.bca.bcamobile.domain.common.DataResult

/** Rekening dan profil. Kontrak: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §3. */
interface AccountRepository {

    suspend fun dashboard(): DataResult<Dashboard>

    /** Dipanggil hanya saat pengguna membuka saldo — jangan dimuat di latar. */
    suspend fun balance(): DataResult<BalanceSummary>

    suspend fun profile(): DataResult<UserProfile>

    /** Batas berlaku beserta pemakaian hari ini. `GET /account/transaction-limit`. */
    suspend fun transactionLimits(): DataResult<TransactionLimits>

    /**
     * Mengubah batas transaksi harian. Butuh [verificationToken] dari
     * `POST /auth/pin/verify` — server menolak tanpa verifikasi PIN.
     */
    suspend fun updateTransactionLimit(
        limits: TransactionLimitInput,
        verificationToken: String,
    ): DataResult<TransactionLimits>

    /** Menerbitkan OTP ke nomor terdaftar; mengesahkan [updateProfile]. */
    suspend fun requestProfileOtp(): DataResult<ProfileOtpChallenge>

    /** Mengubah email dan/atau nomor HP. `otpCode` dari [requestProfileOtp]. */
    suspend fun updateProfile(
        email: String?,
        phoneNumber: String?,
        otpCode: String,
    ): DataResult<Unit>

    /** Mendaftarkan token push perangkat ini. Perangkat diambil dari access token. */
    suspend fun registerPushToken(pushToken: String): DataResult<Unit>

    suspend fun updateBiometricSetting(enabled: Boolean): DataResult<Unit>

    /** `PUT /account/settings` field `push_notification_enabled`. */
    suspend fun updateNotificationSetting(enabled: Boolean): DataResult<Unit>

    /** `PUT /account/settings` field `email_statement_enabled`. */
    suspend fun updateEmailStatementSetting(enabled: Boolean): DataResult<Unit>
}
