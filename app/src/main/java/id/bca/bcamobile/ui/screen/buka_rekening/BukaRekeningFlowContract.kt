package id.bca.bcamobile.ui.screen.buka_rekening

import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.core.liveness.LivenessProgress
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import id.bca.bcamobile.domain.onboarding.model.LivenessMeta
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.Product
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
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
    /** Pilihan jenis kartu Paspor BCA; default kartu pertama (Blue) seperti di desain. */
    val selectedCardIndex: Int = 0,

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

    // Step 6b: verifikasi OTP
    /** Nomor tujuan yang sudah tersamar server; dipakai apa adanya. */
    val otpSentTo: String = "",
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

/** Satu-satunya pintu masuk perubahan state — lihat pola MVI di `android-architecture-patterns`. */
sealed interface BukaRekeningEvent {

    data class ProductSelected(val index: Int) : BukaRekeningEvent
    data class CardTypeSelected(val index: Int) : BukaRekeningEvent
    data object TncAccepted : BukaRekeningEvent

    data class KtpPhotoCaptured(
        val photo: File,
        val flashUsed: Boolean,
        val autoCaptured: Boolean,
        val resolution: String,
    ) : BukaRekeningEvent

    data object FlashModeToggled : BukaRekeningEvent
    data class AutoCaptureToggled(val enabled: Boolean) : BukaRekeningEvent

    /** Foto ditolak pengguna atau akurasi terlalu rendah; berkas cache ikut dibuang. */
    data object PhotoDiscarded : BukaRekeningEvent

    /** Dipakai saat resume: hasil OCR sudah ada di server, tinggal diambil. */
    data object OcrResultRequested : BukaRekeningEvent
    data object OcrConfirmed : BukaRekeningEvent

    data class GenderSelected(val value: JenisKelamin) : BukaRekeningEvent
    data class DomicileSameToggled(val same: Boolean) : BukaRekeningEvent
    data object PersonalDataSubmitted : BukaRekeningEvent

    data class OtpCodeChanged(val code: String) : BukaRekeningEvent

    /** Kode dikirim dari state, bukan dari parameter — layar tidak menyimpan kodenya sendiri. */
    data object OtpSubmitted : BukaRekeningEvent
    data object OtpResendRequested : BukaRekeningEvent

    /** Layar OTP ditinggalkan; kodenya dibuang dari memory. */
    data object OtpCodeCleared : BukaRekeningEvent

    data class BiometricCaptured(
        val facePhoto: File,
        val livenessFrames: List<File>,
        val meta: LivenessMeta,
    ) : BukaRekeningEvent

    data class LivenessProgressed(val progress: LivenessProgress) : BukaRekeningEvent
    data object LivenessStarted : BukaRekeningEvent

    data object QueueJoinRequested : BukaRekeningEvent
    data class VideoCallCompleted(val csName: String) : BukaRekeningEvent

    data class AccessCodeChanged(val value: String) : BukaRekeningEvent
    data class ConfirmAccessCodeChanged(val value: String) : BukaRekeningEvent
    data object AccessCodeVisibilityToggled : BukaRekeningEvent
    data object ConfirmAccessCodeVisibilityToggled : BukaRekeningEvent
    data class PinChanged(val value: String) : BukaRekeningEvent
    data class ConfirmPinChanged(val value: String) : BukaRekeningEvent
    data object CredentialsSubmitted : BukaRekeningEvent

    data class AgreementToggled(val agreed: Boolean) : BukaRekeningEvent
    data object ApplicationSubmitted : BukaRekeningEvent

    data object DraftSaveRequested : BukaRekeningEvent
    data object DraftResumeRequested : BukaRekeningEvent
    data object FlowAbandoned : BukaRekeningEvent
    data object ErrorDismissed : BukaRekeningEvent
}

/** Kejadian sekali pakai: navigasi dan pesan. Tidak boleh disimpan di state. */
sealed interface BukaRekeningSideEffect {

    /** Server sudah memindahkan flow ke [step]; navigasi mengikuti nilai ini, bukan tebakan lokal. */
    data class AdvanceTo(val step: OnboardingStep) : BukaRekeningSideEffect

    /** Sesi habis atau hilang — kembali ke awal flow dan bersihkan state. */
    data object RestartFlow : BukaRekeningSideEffect

    data class ShowMessage(val text: ErrorText) : BukaRekeningSideEffect

    data object ExitFlow : BukaRekeningSideEffect

    /** Foto e-KTP selesai diproses di perangkat; lanjut ke layar pratinjau hasil. */
    data object ShowCaptureResult : BukaRekeningSideEffect
}
