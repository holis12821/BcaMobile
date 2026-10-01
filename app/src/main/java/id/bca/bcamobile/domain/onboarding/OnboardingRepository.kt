package id.bca.bcamobile.domain.onboarding

import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.domain.onboarding.model.CredentialResult
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.SelectedCard
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import java.io.File

/**
 * Kontrak data layer untuk flow buka rekening.
 *
 * Implementasi memegang `session_id` sendiri — pemanggil tidak perlu mengopernya.
 * Semua PII hanya lewat di memory; yang dipersistensi hanya `session_id` (terenkripsi).
 */
interface OnboardingRepository {

    /** `session_id` aktif yang tersimpan, atau null bila belum ada draft. */
    fun savedSessionId(): String?

    /** Hapus jejak sesi lokal tanpa memanggil server. Dipakai saat sesi expired/not found. */
    fun clearLocalSession()

    /**
     * Katalog kartu Paspor untuk [productType]. Dipanggil sebelum sesi ada —
     * endpoint ini tidak butuh `session_id`.
     */
    suspend fun cardCatalog(productType: ProductType): DataResult<CardCatalog>

    /**
     * [cardType] ikut dikirim supaya server langsung menandai `card_selected`.
     * Kosong berarti server menjawab `current_step: CARD_SELECTION`.
     */
    suspend fun createSession(
        productType: ProductType,
        acceptedTncVersion: String,
        cardType: PasporCardType? = null,
        cardCatalogVersion: String? = null,
    ): DataResult<OnboardingSession>

    /** Ubah kartu pada sesi berjalan — Back dari S&K, lanjut draf, atau dari Ringkasan. */
    suspend fun selectCard(
        cardType: PasporCardType,
        cardCatalogVersion: String? = null,
    ): DataResult<SelectedCard>

    suspend fun getSession(sessionId: String): DataResult<OnboardingSession>

    suspend fun cancelSession(): DataResult<Unit>

    suspend fun uploadKtpPhoto(
        photo: File,
        flashUsed: Boolean,
        autoCaptured: Boolean,
        resolution: String,
    ): DataResult<KtpOcrResult>

    suspend fun getOcrResult(): DataResult<KtpOcrResult>

    suspend fun savePersonalData(
        ocrId: String,
        data: PersonalData,
    ): DataResult<PersonalDataResult>

    /**
     * Verifikasi kode OTP. Tidak pernah diulang otomatis: percobaan yang terbuang
     * memicu `OTP_BLOCKED`.
     */
    suspend fun verifyOtp(otpCode: String): DataResult<OtpVerification>

    /** Kirim ulang OTP. Kode sebelumnya langsung mati begitu yang baru terbit. */
    suspend fun resendOtp(): DataResult<OtpChallenge>

    suspend fun uploadBiometric(
        facePhoto: File,
        livenessFrames: List<File>,
        meta: LivenessMeta,
    ): DataResult<BiometricResult>

    suspend fun joinVideoCallQueue(): DataResult<QueueTicket>

    /**
     * Enkripsi kredensial dengan kunci publik server lalu simpan.
     * Pemanggil mengirim kode akses dan PIN apa adanya; enkripsi RSA-OAEP terjadi di dalam.
     */
    suspend fun saveCredentials(
        accessCode: String,
        pin: String,
    ): DataResult<CredentialResult>

    /**
     * Submit final. Idempotency key dibuat sekali lalu dipakai ulang untuk
     * setiap retry sesi ini, supaya rekening tidak dibuat dua kali.
     */
    suspend fun submitApplication(
        agreementVersion: String,
        idempotencyKey: String,
    ): DataResult<CreatedAccount>
}
