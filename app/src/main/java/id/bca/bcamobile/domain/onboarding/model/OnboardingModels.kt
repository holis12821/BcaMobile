package id.bca.bcamobile.domain.onboarding.model

/** Jenis produk rekening. Nilai wire-nya dipakai di request `product_type`. */
enum class ProductType(val wireValue: String) {
    TAHAPAN_BCA("TAHAPAN_BCA"),
    TAHAPAN_XPRESI("TAHAPAN_XPRESI"),
    TABUNGANKU("TABUNGANKU"),
    ;

    companion object {
        /** Urutan ini harus sama dengan urutan kartu di layar Pilih Jenis Rekening. */
        fun fromIndex(index: Int): ProductType? = entries.getOrNull(index)

        fun fromWire(value: String?): ProductType? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * Step yang sedang aktif menurut server. Client selalu navigate berdasarkan nilai ini,
 * bukan berdasarkan tebakan lokal (aturan wajib #8 di skill buka-rekening-api).
 */
enum class OnboardingStep(val wireValue: String) {
    TNC("TNC"),
    /** Sisipan pilih kartu Paspor, tepat sesudah S&K (`08-PILIH-KARTU-API-SPEC.md` §6). */
    CARD_SELECTION("CARD_SELECTION"),
    OCR("OCR"),
    PERSONAL_DATA("PERSONAL_DATA"),
    OTP_VERIFY("OTP_VERIFY"),
    BIOMETRIC("BIOMETRIC"),
    VIDEO_CALL("VIDEO_CALL"),
    CREDENTIALS("CREDENTIALS"),
    REVIEW("REVIEW"),
    COMPLETED("COMPLETED"),
    ;

    companion object {
        fun fromWire(value: String?): OnboardingStep? = entries.firstOrNull { it.wireValue == value }
    }
}

data class Product(
    val type: ProductType,
    val name: String,
    val currency: String,
    val minInitialDeposit: Long,
    val features: List<String>,
)

data class StepsCompleted(
    val tncAccepted: Boolean = false,
    val cardSelected: Boolean = false,
    val ocrVerified: Boolean = false,
    val personalDataSaved: Boolean = false,
    val otpVerified: Boolean = false,
    val biometricVerified: Boolean = false,
    val videoCallVerified: Boolean = false,
    val credentialsSet: Boolean = false,
    val submitted: Boolean = false,
)

data class OnboardingSession(
    val sessionId: String,
    val product: Product?,
    val card: SelectedCard? = null,
    val currentStep: OnboardingStep,
    val stepsCompleted: StepsCompleted = StepsCompleted(),
    val expiresAt: String? = null,
)

/**
 * Hasil OCR yang dijalankan di perangkat lewat ML Kit.
 *
 * Bukan pengganti OCR server: dipakai untuk menampilkan hasil seketika dan
 * menolak foto buruk sebelum diunggah. Yang berwenang tetap [KtpOcrResult]
 * dari backend, karena hanya server yang mencocokkan ke Dukcapil.
 */
data class LocalKtpScan(
    val data: KtpData,
    val accuracyPercent: Double,
    /** Field wajib yang gagal terbaca; jadi alasan saat foto disarankan diulang. */
    val missingFields: List<String> = emptyList(),
)

/** Hasil OCR e-KTP beserta metrik kualitas foto. */
data class KtpOcrResult(
    val ocrId: String,
    val accuracyPercent: Double,
    val extracted: KtpData,
    val dukcapilMatch: Boolean,
    val sharpness: String?,
    val glareDetected: Boolean,
    val allCornersVisible: Boolean,
)

data class KtpData(
    val nik: String,
    val namaLengkap: String,
    val tempatLahir: String,
    val tanggalLahir: String,
    val jenisKelamin: String,
    val alamat: String,
    val rtRw: String,
    val kelurahan: String,
    val kecamatan: String,
    val kota: String,
    val provinsi: String,
    val agama: String,
    val statusPerkawinan: String,
)

data class AlamatKtp(
    val alamatLengkap: String,
    val rtRw: String,
    val kodePos: String,
    val kelurahan: String,
    val kecamatan: String,
    val kota: String,
    val provinsi: String,
)

data class PersonalData(
    val nik: String,
    val namaLengkap: String,
    val tempatLahir: String,
    val tanggalLahir: String,
    val jenisKelamin: String,
    val alamatKtp: AlamatKtp,
    val alamatDomisiliSama: Boolean,
    val pekerjaan: String,
    val penghasilanPerBulan: String,
    val sumberDanaUtama: String,
    val nomorHp: String,
    val email: String,
)

/**
 * Hasil simpan data pribadi — server langsung mengirim OTP ke nomor terdaftar.
 *
 * [otpSentTo] sudah tersamar oleh server; tampilkan apa adanya, jangan menyusun
 * ulang penyamaran dari nomor yang diisi nasabah.
 */
data class PersonalDataResult(
    val personalDataId: String,
    val otpSentTo: String,
    val otpExpiresAt: String?,
    val currentStep: OnboardingStep,
)

/** Tantangan OTP yang sedang berlaku, dari `personal-data` maupun `resend-otp`. */
data class OtpChallenge(
    val otpSentTo: String,
    val otpExpiresAt: String?,
)

/** Hasil `verify-otp`. [currentStep] yang menentukan tujuan navigasi berikutnya. */
data class OtpVerification(
    val verified: Boolean,
    val currentStep: OnboardingStep,
)

data class LivenessMeta(
    val challengeType: String,
    val completedActions: Int,
    val precisionScore: Double,
)

data class BiometricResult(
    val biometricId: String,
    val livenessVerified: Boolean,
    val livenessScore: Double,
    val faceMatchWithKtp: Boolean,
    val faceMatchScore: Double,
)

data class OperatingHours(
    val start: String,
    val end: String,
    val timezone: String,
)

/**
 * Satu server STUN/TURN, sudah berbentuk `RTCIceServer`. Diteruskan apa adanya
 * ke `PeerConnection` begitu dependency WebRTC disetujui.
 */
data class IceServer(
    val urls: List<String>,
    val username: String?,
    val credential: String?,
)

data class QueueTicket(
    val queueId: String,
    val queueNumber: String,
    val position: Int,
    val estimatedWaitSeconds: Int,
    val operatingHours: OperatingHours?,
    /** Sekali pakai; sambungan yang putus harus join antrean lagi. */
    val signalingUrl: String,
    val signalingExpiresInSeconds: Int,
    /** Kosong = TURN belum dikonfigurasi, bukan kegagalan. */
    val iceServers: List<IceServer>,
)

/**
 * Kunci publik untuk enkripsi kredensial.
 * [isDevMode] true saat server belum mengonfigurasi kunci RSA — kredensial dikirim apa adanya.
 */
data class PublicKeyMaterial(
    val algorithm: String,
    val keyId: String,
    val publicKeyPem: String,
) {
    val isDevMode: Boolean get() = publicKeyPem.isBlank()
}

data class CredentialResult(
    val credentialId: String,
    val biometricLoginAvailable: Boolean,
)

data class CreatedAccount(
    val accountNumber: String,
    val accountType: String,
    val accountHolder: String,
    val branch: String,
    val branchCode: String,
    val currency: String,
    val status: String,
    val minInitialDeposit: Long,
    val initialDepositDeadline: String?,
)

// -- Kartu Paspor --------------------------------------------------------------

/** Jenis kartu Paspor. Nilai wire-nya dipakai `card_type` di request. */
enum class PasporCardType(val wireValue: String) {
    PASPOR_BLUE("PASPOR_BLUE"),
    PASPOR_GOLD("PASPOR_GOLD"),
    PASPOR_PLATINUM("PASPOR_PLATINUM"),
    ;

    companion object {
        fun fromWire(value: String?): PasporCardType? =
            entries.firstOrNull { it.wireValue == value }
    }
}

/** Gaya visual kartu; pemetaan ke token warna terjadi di layer UI, bukan di sini. */
enum class CardStyle(val wireValue: String) {
    BLUE("BLUE"),
    GOLD("GOLD"),
    PLATINUM("PLATINUM"),
    ;

    companion object {
        fun fromWire(value: String?): CardStyle =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: BLUE
    }
}

enum class CardTier(val wireValue: String) {
    DEBIT("DEBIT"),
    PLATINUM_DEBIT("PLATINUM_DEBIT"),
    ;

    companion object {
        fun fromWire(value: String?): CardTier =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: DEBIT
    }
}

/** Label promosi dari server; teksnya sendiri ada di `strings.xml`. */
enum class CardBadge(val wireValue: String) {
    RECOMMENDED_BEGINNER("RECOMMENDED_BEGINNER"),
    FLEXIBLE_TRANSACTION("FLEXIBLE_TRANSACTION"),
    MAX_LIMIT("MAX_LIMIT"),
    NONE(""),
    ;

    companion object {
        fun fromWire(value: String?): CardBadge =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: NONE
    }
}

enum class CardAvailabilityStatus(val wireValue: String) {
    AVAILABLE("AVAILABLE"),
    OUT_OF_STOCK("OUT_OF_STOCK"),
    DISABLED("DISABLED"),
    NOT_ELIGIBLE("NOT_ELIGIBLE"),
    ;

    companion object {
        /**
         * Status yang tidak dikenal diperlakukan sebagai tidak tersedia.
         *
         * Menebak "tersedia" berarti nasabah memilih kartu yang lalu ditolak
         * `409 CARD_TYPE_UNAVAILABLE` saat sesi dibuat — kegagalan yang muncul
         * terlambat, di layar yang berbeda.
         */
        fun fromWire(value: String?): CardAvailabilityStatus =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: DISABLED
    }
}

enum class CardUnavailableReason(val wireValue: String) {
    STOCK_EMPTY_IN_REGION("STOCK_EMPTY_IN_REGION"),
    TEMPORARILY_DISABLED("TEMPORARILY_DISABLED"),
    PRODUCT_MISMATCH("PRODUCT_MISMATCH"),
    AGE_REQUIREMENT("AGE_REQUIREMENT"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromWire(value: String?): CardUnavailableReason? = value
            ?.takeIf { it.isNotBlank() }
            ?.let { wire -> entries.firstOrNull { it.wireValue == wire.uppercase() } ?: UNKNOWN }
    }
}

data class CardFees(
    val monthlyAdmin: Long,
    val cardIssuance: Long,
    val cardReplacement: Long,
)

data class CardLimits(
    val cashWithdrawal: Long,
    val transferBca: Long,
    val transferInterbank: Long,
    val debitPurchase: Long,
)

data class CardAvailability(
    val status: CardAvailabilityStatus,
    val reason: CardUnavailableReason?,
) {
    val isSelectable: Boolean get() = status == CardAvailabilityStatus.AVAILABLE
}

data class CardDelivery(
    val physicalCardAvailable: Boolean,
    val estimatedDaysMin: Int?,
    val estimatedDaysMax: Int?,
    val branchPickupAvailable: Boolean,
)

data class CardEligibility(
    val minAge: Int,
    val minInitialDeposit: Long,
)

data class PasporCard(
    val cardType: PasporCardType,
    val name: String,
    val network: String,
    val tier: CardTier,
    val style: CardStyle,
    val badge: CardBadge,
    val isPopular: Boolean,
    val displayOrder: Int,
    val fees: CardFees,
    val limits: CardLimits,
    val availability: CardAvailability,
    val delivery: CardDelivery,
    val eligibility: CardEligibility,
)

/**
 * Katalog kartu untuk satu produk.
 *
 * [catalogVersion] ikut dikirim saat sesi dibuat supaya server bisa mencatat versi
 * biaya yang benar-benar dilihat nasabah.
 */
data class CardCatalog(
    val catalogVersion: String,
    val productType: ProductType?,
    val defaultCardType: PasporCardType?,
    val currency: String,
    val cards: List<PasporCard>,
) {
    /** Urutan tampil ditentukan server lewat `display_order`. */
    val sortedCards: List<PasporCard> get() = cards.sortedBy { it.displayOrder }
}

/** Kartu yang menempel pada sesi onboarding. */
data class SelectedCard(
    val cardType: PasporCardType?,
    val name: String,
    val style: CardStyle,
    val monthlyAdminFee: Long,
    val catalogVersion: String?,
)
