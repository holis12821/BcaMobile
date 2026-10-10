package id.bca.bcamobile.domain.onboarding.model

import id.bca.bcamobile.core.liveness.LivenessStepFrame
import id.bca.bcamobile.core.security.DeviceRiskSignals

/** Jenis produk rekening. Nilai wire-nya dipakai di request `product_type`. */
enum class ProductType(val wireValue: String) {
    TAHAPAN_BCA("TAHAPAN_BCA"),
    TAHAPAN_XPRESI("TAHAPAN_XPRESI"),
    TABUNGANKU("TABUNGANKU"),
    ;

    companion object {
        /**
         * Tidak ada `fromIndex`. Pemetaan posisi layar ke ordinal enum pernah ada di sini
         * dan harus tetap tidak ada: urutan tampil ditentukan server lewat `display_order`
         * dan produk bisa disembunyikan, jadi indeks ke-N tidak berarti produk ke-N.
         * Identitas selalu ikut di dalam item sebagai [ProductType], tidak pernah
         * disimpulkan dari tempatnya.
         */
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

// -- Katalog jenis rekening ----------------------------------------------------

/** Gaya visual kartu produk; pemetaan ke token warna terjadi di layer UI, bukan di sini. */
enum class ProductStyle(val wireValue: String) {
    PRIMARY("PRIMARY"),
    SECONDARY("SECONDARY"),
    NEUTRAL("NEUTRAL"),
    ;

    companion object {
        /** Gaya asing jatuh ke [NEUTRAL] — netral tidak menonjolkan produk yang salah. */
        fun fromWire(value: String?): ProductStyle =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: NEUTRAL
    }
}

/** Label promosi dari server; teksnya sendiri ada di `strings.xml`. */
enum class ProductBadge(val wireValue: String) {
    MOST_POPULAR("MOST_POPULAR"),
    NONE(""),
    ;

    companion object {
        fun fromWire(value: String?): ProductBadge =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: NONE
    }
}

enum class ProductAvailabilityStatus(val wireValue: String) {
    AVAILABLE("AVAILABLE"),
    DISABLED("DISABLED"),
    COMING_SOON("COMING_SOON"),
    ;

    companion object {
        /**
         * Status yang tidak dikenal diperlakukan sebagai tidak tersedia, sama seperti
         * [CardAvailabilityStatus]: menebak "tersedia" berarti nasabah memilih produk yang
         * lalu ditolak `422 ONBOARDING_PRODUCT_UNAVAILABLE` saat sesi dibuat — dua layar
         * setelah pilihannya.
         */
        fun fromWire(value: String?): ProductAvailabilityStatus =
            entries.firstOrNull { it.wireValue == value?.uppercase() } ?: DISABLED
    }
}

enum class ProductUnavailableReason(val wireValue: String) {
    TEMPORARILY_DISABLED("TEMPORARILY_DISABLED"),
    MAINTENANCE("MAINTENANCE"),
    COMING_SOON("COMING_SOON"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromWire(value: String?): ProductUnavailableReason? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.wireValue == value.uppercase() } ?: UNKNOWN
        }
    }
}

data class ProductAvailability(
    val status: ProductAvailabilityStatus,
    val reason: ProductUnavailableReason?,
) {
    val isSelectable: Boolean get() = status == ProductAvailabilityStatus.AVAILABLE
}

/**
 * Satu jenis rekening di katalog.
 *
 * [type] adalah identitasnya; [displayOrder] hanya urutan tampil. [iconKey] masih kunci
 * server — pemetaan ke drawable terjadi di layar, seperti [TncSection.iconKey].
 */
data class SavingsProduct(
    val type: ProductType,
    val name: String,
    val description: String,
    val minInitialDeposit: Long,
    val currency: String,
    val iconKey: String,
    val style: ProductStyle,
    val features: List<String>,
    val isPopular: Boolean,
    val badge: ProductBadge,
    val isDefault: Boolean,
    val displayOrder: Int,
    val availability: ProductAvailability,
)

/** Kotak persiapan dokumen di bawah daftar produk. */
data class ProductNotice(
    val iconKey: String,
    val title: String,
    val body: String,
)

/**
 * Kalimat S&K di bawah kotak persiapan, terpotong tiga karena [link] dicetak tebal dan
 * berwarna. Dibiarkan terpisah sampai ke layar supaya tidak ada pencarian substring.
 */
data class ProductConsent(
    val prefix: String,
    val link: String,
    val suffix: String,
)

/** Copy layar Pilih Jenis Rekening; bagian yang kosong disembunyikan layar. */
data class ProductPage(
    val heading: String,
    val subtitle: String,
    val depositLabel: String,
    val ctaLabel: String,
    val notice: ProductNotice?,
    val consent: ProductConsent?,
)

/**
 * Katalog jenis rekening.
 *
 * [catalogVersion] **tidak** dikirim kembali saat sesi dibuat — berbeda dari
 * `card_catalog_version`. Server mencatatnya sendiri beserta setoran awal yang tampil.
 */
data class SavingsProductCatalog(
    val catalogVersion: String,
    val page: ProductPage?,
    val products: List<SavingsProduct>,
) {
    /** Urutan tampil ditentukan server lewat `display_order`. */
    val sortedProducts: List<SavingsProduct> get() = products.sortedBy { it.displayOrder }

    fun find(type: ProductType?): SavingsProduct? =
        type?.let { wanted -> products.firstOrNull { it.type == wanted } }
}

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
 * Dipakai untuk dua hal: menampilkan hasil seketika sebelum unggahan, dan — lewat
 * [rawText] — **menjadi sumber teks yang dibaca server**. Yang berwenang atas
 * hasil akhir tetap [KtpOcrResult] dari backend: server yang memvalidasi tata
 * letak e-KTP, bentuk NIK, dan kecocokan NIK dengan tanggal lahir serta jenis
 * kelamin. Client tidak pernah memutuskan kartunya sah.
 */
data class LocalKtpScan(
    val data: KtpData,
    val accuracyPercent: Double,
    /** Field wajib yang gagal terbaca; jadi alasan saat foto disarankan diulang. */
    val missingFields: List<String> = emptyList(),
    /**
     * Teks mentah hasil ML Kit, dikirim bersama foto.
     *
     * Dulu hasil pengenalan ini ditampilkan lalu dibuang, sementara server
     * menjawab dari teks hardcoded — jadi identitas yang masuk ke form data
     * pribadi adalah milik orang lain, bukan milik kartu yang difoto.
     *
     * Tidak pernah ditulis ke disk dan tidak masuk state yang dipersistensi:
     * isinya NIK, nama, dan alamat.
     */
    val rawText: String = "",
)

/** Hasil OCR e-KTP beserta metrik kualitas foto. */
data class KtpOcrResult(
    val ocrId: String,
    val accuracyPercent: Double,
    val extracted: KtpData,
    val dukcapilMatch: Boolean,
    /** Registri kependudukan dihubungi. [dukcapilMatch] hanya berarti bila ini true. */
    val dukcapilChecked: Boolean,
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

/**
 * Bukti satu percobaan liveness, siap dikirim ke server.
 *
 * Penggantinya yang lama, `LivenessMeta`, hanya berisi `challenge_type`,
 * `completed_actions`, dan `precision_score` — ketiganya diisi konstanta oleh client
 * dan dipercaya server sebagai bukti kelulusan. Yang dikirim sekarang adalah bahan
 * mentah untuk diverifikasi ulang: nonce yang diterbitkan server, frame per langkah
 * beserta waktunya, tanda tangan kunci perangkat atas isi frame, dan token
 * integritas. Tidak ada satu pun field di sini yang menyatakan "lulus".
 *
 * [neutralFrame] dan [stepFrames] hidup **hanya di memori** dan wajib ditimpa nol
 * oleh pemanggil setelah unggahan, berhasil maupun gagal.
 */
data class LivenessSubmission(
    val challengeId: String,
    val nonce: String,
    val neutralFrame: ByteArray,
    val stepFrames: List<LivenessStepFrame>,
    /** String kanonis yang ditandatangani; server merekonstruksinya sendiri. */
    val signedPayload: String,
    val signature: String,
    val signatureAlgorithm: String,
    val deviceKeyId: String,
    val devicePublicKey: String,
    /** Null bila `PLAY_INTEGRITY_CLOUD_PROJECT` belum dikonfigurasi. */
    val integrityToken: String?,
    val riskSignals: DeviceRiskSignals,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LivenessSubmission) return false
        return challengeId == other.challengeId &&
            nonce == other.nonce &&
            neutralFrame.contentEquals(other.neutralFrame) &&
            stepFrames == other.stepFrames &&
            signedPayload == other.signedPayload &&
            signature == other.signature
    }

    override fun hashCode(): Int {
        var result = challengeId.hashCode()
        result = 31 * result + nonce.hashCode()
        result = 31 * result + neutralFrame.contentHashCode()
        result = 31 * result + stepFrames.hashCode()
        result = 31 * result + signedPayload.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }
}

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

// -- Syarat & Ketentuan --------------------------------------------------------

/** Satu pasal S&K. [iconKey] masih kunci server; pemetaan ke drawable terjadi di layar. */
data class TncSection(
    val iconKey: String,
    val title: String,
    val body: String,
)

/** Kotak PENTING di bawah daftar pasal. */
data class TncNotice(
    val label: String,
    val body: String,
)

/**
 * Kalimat di samping checkbox, terpotong tiga karena [link] dicetak tebal dan berwarna.
 * Dibiarkan terpisah sampai ke layar supaya tidak ada pencarian substring.
 */
data class TncConsent(
    val prefix: String,
    val link: String,
    val suffix: String,
)

/**
 * Dokumen Syarat & Ketentuan yang sedang tayang.
 *
 * [version] wajib dikirim kembali apa adanya sebagai `accepted_tnc_version` saat sesi
 * dibuat, dan harus berasal dari dokumen yang **benar-benar terpampang** saat nasabah
 * menekan setuju — bukan konstanta, bukan nilai yang di-cache dari pembukaan layar
 * sebelumnya. Itu yang diperiksa server, dan selisihnya dijawab `409`.
 *
 * Umurnya hanya selama layar S&K tampil; langkah berikutnya tidak membacanya, jadi
 * dokumen ini tidak ikut ke `BukaRekeningFlowState`.
 */
data class TncDocument(
    val version: String,
    val heading: String,
    val subtitle: String,
    val trustTitle: String,
    val trustSubtitle: String,
    val sections: List<TncSection>,
    val notice: TncNotice?,
    val consent: TncConsent?,
    val agreeCta: String,
    val effectiveFrom: String,
    /**
     * `false` hanya mungkin pada respons `?version=`. Dokumen yang sudah dicabut tidak
     * boleh menawarkan tombol setuju — persetujuannya pasti ditolak `409`.
     */
    val isActive: Boolean,
)
