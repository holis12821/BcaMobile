package id.bca.bcamobile.domain.onboarding

import id.bca.bcamobile.domain.common.DataResult
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
     * Katalog jenis rekening beserta copy layar Pilih Jenis Rekening.
     *
     * Dipanggil sebelum sesi ada, dan **tidak wajib** berhasil: layar jatuh ke daftar
     * bawaan `strings.xml` dan flow tetap berjalan, karena [createSession] tidak menuntut
     * katalog ini ada. `503 ONBOARDING_CATALOG_UNAVAILABLE` adalah keadaan normal — itu
     * jawaban server saat katalognya dimatikan lewat feature flag.
     *
     * [DataResult.Failure] juga dipakai untuk katalog tanpa produk yang layak dipilih,
     * karena hasilnya sama bagi pemanggil: pakai daftar bawaan.
     */
    suspend fun savingsProducts(): DataResult<SavingsProductCatalog>

    /**
     * Teks Syarat & Ketentuan yang sedang berlaku beserta nomor versinya.
     *
     * Dipanggil sebelum sesi ada, dan **wajib** sebelum [createSession]: versi yang
     * dikirim sebagai `acceptedTncVersion` harus versi dokumen yang benar-benar
     * terpampang di layar, bukan konstanta. Selisihnya dijawab `409 TNC_VERSION_OUTDATED`.
     *
     * `null` di dalam [DataResult.Success] tidak mungkin — dokumen tanpa versi atau tanpa
     * pasal dilaporkan sebagai [DataResult.Failure], karena tidak ada yang bisa disetujui.
     */
    suspend fun tnc(): DataResult<TncDocument>

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

    /**
     * Unggah foto e-KTP beserta teks yang sudah dibaca ML Kit di perangkat.
     *
     * [clientOcrText] dipakai server sebagai sumber teks selama belum ada mesin
     * OCR sisi server. Server tetap yang memvalidasi: tata letak e-KTP, bentuk
     * NIK, dan kecocokan NIK dengan tanggal lahir serta jenis kelamin. Mengirim
     * teks kosong berarti tidak ada yang bisa dibaca, dan server menolak dengan
     * `OCR_NOT_KTP` alih-alih mengarang identitas.
     */
    suspend fun uploadKtpPhoto(
        photo: File,
        flashUsed: Boolean,
        autoCaptured: Boolean,
        resolution: String,
        clientOcrText: String,
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

    /**
     * Meminta tantangan liveness baru dari server.
     *
     * Aksi dan urutannya **selalu** datang dari sini. Tidak ada jalur lain yang boleh
     * membuat tantangan: urutan yang dipilih client bisa ditebak, dan urutan yang bisa
     * ditebak bisa dilewati dengan satu video rekaman.
     */
    suspend fun requestLivenessChallenge(): DataResult<LivenessChallenge>

    /**
     * Mengirim bukti liveness untuk diverifikasi server.
     *
     * Hasil [BiometricResult] adalah satu-satunya sumber keputusan lulus. Pemanggil
     * wajib menimpa frame di [LivenessSubmission] setelah panggilan ini selesai,
     * berhasil maupun gagal.
     */
    suspend fun submitLiveness(submission: LivenessSubmission): DataResult<BiometricResult>

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
