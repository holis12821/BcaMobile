package id.bca.bcamobile.ui.screen.buka_rekening.common

import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.core.liveness.LivenessProgress
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.Product
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.domain.onboarding.model.SelectedCard
import java.io.File

/**
 * State bersama seluruh flow buka rekening.
 *
 * Hidup selama navigation graph buka rekening masih ada di back stack, lalu
 * ikut hilang saat graph dilepas. PII di sini hanya ada di memory — tidak ada
 * yang ditulis ke disk (aturan wajib #1 skill `buka-rekening-api`).
 */
data class BukaRekeningFlowState(
    val isLoading: Boolean = false,
    val error: ErrorText? = null,

    // Sesi
    val sessionId: String? = null,
    val currentStep: OnboardingStep? = null,
    val hasResumableDraft: Boolean = false,

    // Step 1-2: produk, kartu Paspor, dan S&K
    val selectedProductIndex: Int? = null,
    val product: Product? = null,
    /**
     * Katalog kartu dari server. Biaya dan limit yang tampil **selalu** dari sini —
     * angka di `strings.xml` hanya dipakai pratinjau.
     */
    val cardCatalog: CardCatalog? = null,
    /** Indeks pada [CardCatalog.sortedCards]; default mengikuti `default_card_type` server. */
    val selectedCardIndex: Int = 0,
    /** Kartu yang sudah tercatat di sesi, dibalas `sessions` dan `card`. */
    val selectedCard: SelectedCard? = null,

    // Step 3-5: foto e-KTP dan OCR
    val ocr: KtpOcrResult? = null,
    /** Hasil OCR di perangkat; tampil lebih dulu sambil menunggu jawaban server. */
    val localScan: LocalKtpScan? = null,
    /** Berkas foto e-KTP di cache, dihapus begitu unggahan selesai. */
    val ktpPhoto: File? = null,
    /** Resolusi tangkapan yang dikirim ke server; dipakai kembali di ringkasan hasil foto. */
    val captureResolution: String = "",
    val isProcessingPhoto: Boolean = false,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isAutoCaptureEnabled: Boolean = true,

    // Step 6: data pribadi
    val jenisKelamin: JenisKelamin? = null,
    val alamatDomisiliSama: Boolean = true,
    /**
     * Isian form yang bisa disunting nasabah. `null` berarti belum di-prefill
     * dari hasil OCR — dipakai [DataPribadiForm.from] saat layar pertama tampil.
     *
     * Nilai di sini **mentah**, bukan nilai tampilan: `rt_rw` tersimpan `003/005`
     * dan tanggal lahir `1995-04-21`, persis seperti yang dikirim ke server.
     * Mapper layar yang mengubahnya jadi bentuk baca.
     */
    val dataPribadi: DataPribadiForm? = null,
    /** Error per-field baru ditampilkan setelah nasabah menekan Lanjut sekali. */
    val showDataPribadiErrors: Boolean = false,

    // Step 6b: verifikasi OTP
    /** Nomor tujuan yang sudah tersamar server; dipakai apa adanya. */
    val otpSentTo: String = "",
    /**
     * `otp_expires_at` dari response, RFC3339 UTC.
     *
     * Disimpan di state karena yang menerbitkan OTP adalah layar Data Pribadi,
     * sedangkan yang menghitung mundur adalah layar OTP — dua ViewModel berbeda.
     */
    val otpExpiresAt: String? = null,
    /**
     * Kode yang sedang diketik. Hanya hidup di memory selama layar OTP tampil —
     * tidak pernah masuk `SavedStateHandle`, log, atau disk (aturan PII #5).
     */
    val otpCode: String = "",
    /**
     * Sisa detik sebelum tombol kirim ulang boleh ditekan. Saat terblokir, ini
     * berisi sisa `details.retry_after_seconds`. Nol berarti tidak ada hitung
     * mundur yang berjalan — bukan berarti OTP sudah kedaluwarsa.
     */
    val otpCountdownSeconds: Int = 0,
    /** `OTP_BLOCKED`: input **dan** tombol kirim ulang mati sampai hitung mundur habis. */
    val isOtpInputBlocked: Boolean = false,
    /** Kuota kirim ulang habis: hanya tombolnya mati, kode terakhir masih sah. */
    val isOtpResendBlocked: Boolean = false,
    val isOtpVerified: Boolean = false,

    // Step 7: biometrik
    val biometric: BiometricResult? = null,
    val liveness: LivenessProgress = LivenessProgress(),
    val isLivenessRunning: Boolean = false,

    // Step 8: video call
    val queue: QueueTicket? = null,
    val isVideoCallVerified: Boolean = false,
    val videoCallCsName: String = "",

    // Step 9: kredensial
    val accessCode: String = "",
    val confirmAccessCode: String = "",
    val isAccessCodeVisible: Boolean = false,
    val isConfirmAccessCodeVisible: Boolean = false,
    val pin: String = "",
    val confirmPin: String = "",
    val isCredentialsSaved: Boolean = false,

    // Step 10-11: persetujuan dan hasil
    val isAgreed: Boolean = false,
    val account: CreatedAccount? = null,
) {
    val isOcrVerified: Boolean get() = ocr?.dukcapilMatch == true
    val isBiometrikVerified: Boolean get() = biometric?.livenessVerified == true

    /** Data e-KTP yang layak ditampilkan: milik server bila sudah ada, kalau belum hasil lokal. */
    val ktpData: KtpData? get() = ocr?.extracted ?: localScan?.data

    /** Akurasi yang ditampilkan mengikuti sumber data yang sedang dipakai. */
    val ktpAccuracy: Double get() = ocr?.accuracyPercent ?: localScan?.accuracyPercent ?: 0.0

    /** true saat hasil baca lokal terlalu buruk untuk diunggah. */
    val needsRetake: Boolean
        get() = ocr == null && localScan != null &&
            localScan.accuracyPercent < KtpTextRecognizer.MIN_ACCEPTABLE_ACCURACY
}

/** Kartu yang sedang dipilih, atau null bila katalog belum ada. */
fun BukaRekeningFlowState.selectedCardType(): PasporCardType? =
    cardCatalog?.sortedCards?.getOrNull(selectedCardIndex)?.cardType
