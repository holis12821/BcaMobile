package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.network.ApiFailure
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.onboarding.OnboardingRepository
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.domain.onboarding.model.CredentialResult
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.core.liveness.LivenessChallenge
import id.bca.bcamobile.domain.onboarding.model.LivenessSubmission
import id.bca.bcamobile.domain.onboarding.model.OnboardingSession
import id.bca.bcamobile.domain.onboarding.model.OtpChallenge
import id.bca.bcamobile.domain.onboarding.model.OtpVerification
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.PersonalData
import id.bca.bcamobile.domain.onboarding.model.PersonalDataResult
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.domain.onboarding.model.SavingsProductCatalog
import id.bca.bcamobile.domain.onboarding.model.SelectedCard
import id.bca.bcamobile.domain.onboarding.model.TncDocument
import java.io.File

/**
 * Pengganti [OnboardingRepository] untuk test ViewModel.
 *
 * Setiap hasil bisa diatur per test, dan pemanggilan yang penting ikut dicatat —
 * beberapa aturan OTP justru soal request yang **tidak boleh** berangkat
 * (kode belum enam digit, kirim ulang saat terblokir, `resend-otp` setelah
 * `OTP_EXPIRED`), jadi jumlah panggilan ikut diuji.
 *
 * Method yang tidak dipakai jalur OTP sengaja melempar: kalau test menyentuhnya,
 * itu tanda testnya salah sasaran, bukan sesuatu yang boleh lewat diam-diam.
 */
class FakeOnboardingRepository : OnboardingRepository {

    var savedSessionId: String? = null

    var createSessionResult: DataResult<OnboardingSession> = failure()
    var cardCatalogResult: DataResult<CardCatalog> = failure()
    var tncResult: DataResult<TncDocument> = failure()
    var selectCardResult: DataResult<SelectedCard> = failure()
    var getSessionResult: DataResult<OnboardingSession> = failure()
    var ocrResult: DataResult<KtpOcrResult> = failure()
    var personalDataResult: DataResult<PersonalDataResult> = failure()
    var verifyOtpResult: DataResult<OtpVerification> = failure()
    var resendOtpResult: DataResult<OtpChallenge> = failure()

    /** Kartu yang ikut terkirim saat sesi dibuat; null berarti client belum memilih. */
    val createSessionCardTypes = mutableListOf<PasporCardType?>()

    /**
     * Versi S&K yang ikut setiap `createSession`.
     *
     * Dicatat, bukan diabaikan: inti kontrak S&K adalah versi yang terkirim harus berasal
     * dari dokumen yang sedang terpampang, dan satu-satunya cara menguji itu adalah
     * memeriksa nilai yang benar-benar sampai ke repository.
     */
    val createSessionTncVersions = mutableListOf<String>()
    var tncCount = 0
    val verifyOtpCodes = mutableListOf<String>()
    var resendOtpCount = 0
    var getSessionCount = 0
    var clearLocalSessionCount = 0
    var savingsProductsCount = 0

    /**
     * Gagal secara bawaan: itu keadaan yang paling sering dilalui test lain, dan katalog
     * yang gagal tidak boleh menghentikan apa pun.
     */
    var savingsProductsResult: DataResult<SavingsProductCatalog> = failure()

    override fun savedSessionId(): String? = savedSessionId

    override fun clearLocalSession() {
        clearLocalSessionCount += 1
        savedSessionId = null
    }

    override suspend fun cardCatalog(productType: ProductType): DataResult<CardCatalog> =
        cardCatalogResult

    override suspend fun savingsProducts(): DataResult<SavingsProductCatalog> {
        savingsProductsCount += 1
        return savingsProductsResult
    }

    override suspend fun tnc(): DataResult<TncDocument> {
        tncCount += 1
        return tncResult
    }

    override suspend fun createSession(
        productType: ProductType,
        acceptedTncVersion: String,
        cardType: PasporCardType?,
        cardCatalogVersion: String?,
    ): DataResult<OnboardingSession> {
        createSessionCardTypes += cardType
        createSessionTncVersions += acceptedTncVersion
        return createSessionResult
    }

    override suspend fun selectCard(
        cardType: PasporCardType,
        cardCatalogVersion: String?,
    ): DataResult<SelectedCard> = selectCardResult

    override suspend fun getSession(sessionId: String): DataResult<OnboardingSession> {
        getSessionCount += 1
        return getSessionResult
    }

    override suspend fun cancelSession(): DataResult<Unit> = DataResult.Success(Unit)

    /** Teks OCR yang diterima panggilan terakhir, untuk memastikan ia benar dikirim. */
    var lastClientOcrText: String? = null

    override suspend fun uploadKtpPhoto(
        photo: File,
        flashUsed: Boolean,
        autoCaptured: Boolean,
        resolution: String,
        clientOcrText: String,
    ): DataResult<KtpOcrResult> {
        lastClientOcrText = clientOcrText
        return ocrResult
    }

    override suspend fun getOcrResult(): DataResult<KtpOcrResult> = ocrResult

    override suspend fun savePersonalData(
        ocrId: String,
        data: PersonalData,
    ): DataResult<PersonalDataResult> = personalDataResult

    override suspend fun verifyOtp(otpCode: String): DataResult<OtpVerification> {
        verifyOtpCodes += otpCode
        return verifyOtpResult
    }

    override suspend fun resendOtp(): DataResult<OtpChallenge> {
        resendOtpCount += 1
        return resendOtpResult
    }

    var livenessChallengeResult: DataResult<LivenessChallenge> = failure()
    var livenessChallengeCount = 0

    override suspend fun requestLivenessChallenge(): DataResult<LivenessChallenge> {
        livenessChallengeCount += 1
        return livenessChallengeResult
    }

    var submitLivenessResult: DataResult<BiometricResult> = failure()
    val submittedLiveness = mutableListOf<LivenessSubmission>()

    override suspend fun submitLiveness(
        submission: LivenessSubmission,
    ): DataResult<BiometricResult> {
        submittedLiveness += submission
        return submitLivenessResult
    }

    var joinQueueResult: DataResult<QueueTicket> = failure()

    /**
     * Berapa kali antrean diminta.
     *
     * Dihitung karena penyambungan ulang signaling **wajib** mengambil tiket baru: token di
     * `signaling_url` sekali pakai, jadi memakai URL yang sama dijamin `401`.
     */
    var joinQueueCount = 0

    override suspend fun joinVideoCallQueue(): DataResult<QueueTicket> {
        joinQueueCount += 1
        return joinQueueResult
    }

    override suspend fun saveCredentials(
        accessCode: String,
        pin: String,
    ): DataResult<CredentialResult> = error("saveCredentials tidak dipakai di test OTP")

    override suspend fun submitApplication(
        agreementVersion: String,
        idempotencyKey: String,
    ): DataResult<CreatedAccount> = error("submitApplication tidak dipakai di test OTP")

    private companion object {
        fun <T> failure(): DataResult<T> = DataResult.Failure(ApiFailure.Unknown)
    }
}
