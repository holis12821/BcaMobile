package id.bca.bcamobile.ui.screen.buka_rekening.common

import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.BiometricResult
import id.bca.bcamobile.domain.onboarding.model.CardCatalog
import id.bca.bcamobile.domain.onboarding.model.CreatedAccount
import id.bca.bcamobile.core.liveness.LivenessState
import id.bca.bcamobile.core.ocr.KtpTextRecognizer
import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.KtpOcrResult
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep
import id.bca.bcamobile.domain.onboarding.model.PasporCardType
import id.bca.bcamobile.domain.onboarding.model.Product
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.domain.onboarding.model.QueueTicket
import id.bca.bcamobile.domain.onboarding.model.SavingsProduct
import id.bca.bcamobile.domain.onboarding.model.SavingsProductCatalog
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
    /**
     * Katalog jenis rekening dari server. `null` berarti belum dimuat, gagal dimuat, atau
     * katalognya dimatikan server — layar memakai daftar bawaan `strings.xml` dan flow
     * tetap jalan.
     */
    val productCatalog: SavingsProductCatalog? = null,
    /**
     * Kapan [productCatalog] diterima, epoch milli. Dipakai [isProductCatalogFresh].
     *
     * Ada karena katalog ini **bisa basi**: server menandainya `max-age=300`, sementara
     * state ini hidup selama graph buka rekening ada di back stack. Tanpa penanda umur,
     * flow yang dibiarkan terbuka satu jam menampilkan setoran awal yang sudah berubah —
     * dan angka yang tampil itulah yang dicatat server sebagai yang dilihat nasabah.
     */
    val productCatalogFetchedAt: Long? = null,
    /**
     * Muat ulang katalog yang diminta nasabah sedang berjalan.
     *
     * Terpisah dari [isLoading] dengan sengaja: `isLoading` membuat layar mengganti
     * seluruh isinya dengan spinner, dan muat ulang justru dilakukan **sambil** daftar
     * bawaan dibaca. Mengosongkannya di tengah pembacaan adalah kerusakan yang lebih
     * besar daripada yang diperbaiki.
     */
    val isRefreshingProductCatalog: Boolean = false,
    /**
     * Produk yang dipilih nasabah, sebagai **kode**.
     *
     * Dulu indeks (`selectedProductIndex`), dan itu salah: indeks dipetakan ke ordinal
     * [ProductType] sementara urutan tampil datang dari `display_order` server dan produk
     * bisa disembunyikan. Satu perubahan urutan di database sudah cukup membuat nasabah
     * membuka rekening yang bukan pilihannya, tanpa error di mana pun.
     */
    val selectedProductType: ProductType? = null,
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
    /**
     * Keadaan mesin status liveness. Tidak memuat sudut, probabilitas mata, atau
     * frame — hanya fase, langkah, dan panduan (aturan PII #5).
     */
    val livenessState: LivenessState = LivenessState(),
    /**
     * Sisa detik masa tunggu, **dihitung server**.
     *
     * Client tidak punya hitungan percobaan sendiri: kalau ada dua hitungan, yang
     * ditampilkan di layar akan menyimpang dari yang sebenarnya berlaku. Angka di sini
     * selalu datang dari `details.retry_after_seconds` (keputusan Q6).
     */
    val livenessCooldownSeconds: Int = 0,
    /** Liveness mandiri dihentikan untuk sesi ini; nasabah diarahkan ke video call. */
    val isLivenessBlocked: Boolean = false,

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
    /**
     * NIK benar-benar dicocokkan ke registri kependudukan **dan** cocok.
     *
     * Dua syarat, bukan satu: server dulu mengirim `dukcapil_match: true` untuk
     * sesi yang tidak punya registri sama sekali, sehingga lencana
     * "terverifikasi" muncul tanpa ada yang memverifikasi. Tanpa registri,
     * kartunya tetap divalidasi server (tata letak e-KTP, bentuk NIK, dan NIK
     * dicocokkan dengan tanggal lahir serta jenis kelamin) — yang tidak boleh
     * diklaim hanyalah pencocokan ke Dukcapil.
     */
    val isOcrVerified: Boolean
        get() = ocr?.dukcapilChecked == true && ocr.dukcapilMatch
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

/**
 * Produk terpilih beserta atributnya, atau null bila katalognya tidak ada.
 *
 * Null **bukan** berarti nasabah belum memilih — saat katalog gagal dimuat,
 * [BukaRekeningFlowState.selectedProductType] tetap terisi dari daftar bawaan. Yang hilang
 * hanya atribut tambahan seperti `is_popular`.
 */
fun BukaRekeningFlowState.selectedProduct(): SavingsProduct? =
    productCatalog?.find(selectedProductType)

/**
 * true bila katalog produk masih layak dipakai tanpa menanyakannya lagi.
 *
 * Mengikuti `Cache-Control: max-age=300` pada endpoint, sama seperti `CachedKey.isFresh()`
 * di `PinKeyProvider`. Penanda umur ini satu-satunya cache katalog yang benar-benar ada di
 * client — tidak ada `Cache` OkHttp terpasang, jadi `ETag` dan `304` tidak pernah terpakai.
 *
 * Katalog tanpa [BukaRekeningFlowState.productCatalogFetchedAt] dianggap **basi**: itu
 * hanya mungkin terjadi kalau ada yang mengisi katalog tanpa mencatat waktunya, dan menarik
 * ulang sekali lebih murah daripada memajang angka yang umurnya tidak diketahui.
 */
fun BukaRekeningFlowState.isProductCatalogFresh(): Boolean {
    val fetchedAt = productCatalogFetchedAt ?: return false
    if (productCatalog == null) return false
    return System.currentTimeMillis() - fetchedAt < PRODUCT_CATALOG_TTL_MILLIS
}

private const val PRODUCT_CATALOG_TTL_MILLIS = 5 * 60 * 1000L
