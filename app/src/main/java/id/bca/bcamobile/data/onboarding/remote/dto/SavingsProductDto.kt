package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Katalog jenis rekening tabungan untuk layar Pilih Jenis Rekening.
 * Kontrak: `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md`, skill backend
 * `buka-rekening-produk`.
 *
 * Objek bersarang nullable dengan default `null` mengikuti `TncDto` dan `CardDto`: respons
 * yang cacat jadi keadaan yang bisa ditangani mapper, bukan `SerializationException` di
 * layar **pertama** buka rekening.
 */
@Serializable
data class ProductNoticeDto(
    /** Kunci, bukan path drawable — dipetakan di layar. */
    @SerialName("icon_key") val iconKey: String = "",
    val title: String = "",
    val body: String = "",
)

/**
 * Tiga potongan, bukan satu kalimat: [link] dicetak tebal dan berwarna lewat
 * `buildAnnotatedString`. Alasan yang sama dengan [TncConsentDto].
 */
@Serializable
data class ProductConsentDto(
    val prefix: String = "",
    val link: String = "",
    val suffix: String = "",
)

/**
 * Copy halaman: judul, subjudul, label setoran, label tombol, kotak persiapan dokumen,
 * dan kalimat S&K. Dilayani server supaya kata-katanya bisa berubah tanpa rilis aplikasi.
 */
@Serializable
data class ProductPageDto(
    val heading: String = "",
    val subtitle: String = "",
    @SerialName("deposit_label") val depositLabel: String = "",
    @SerialName("cta_label") val ctaLabel: String = "",
    val notice: ProductNoticeDto? = null,
    val consent: ProductConsentDto? = null,
)

@Serializable
data class ProductOptionDto(
    /**
     * Identitas produk, dan satu-satunya yang boleh dipakai client untuk mengenalinya.
     * [displayOrder] hanya urutan tampilan.
     */
    @SerialName("product_type") val productType: String = "",
    val name: String = "",
    val description: String = "",
    /** Rupiah penuh, bukan string terformat: `500000`, bukan `"Rp 500.000"`. */
    @SerialName("min_initial_deposit") val minInitialDeposit: Long = 0,
    val currency: String = "IDR",
    /** Kunci ikon; kunci asing jatuh ke ikon cadangan di layar. */
    @SerialName("icon_key") val iconKey: String = "",
    val style: String = "",
    val features: List<String> = emptyList(),
    @SerialName("is_popular") val isPopular: Boolean = false,
    /** `null` berarti server memang tidak mengirim badge — bukan badge tanpa teks. */
    @SerialName("badge_key") val badgeKey: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("availability_status") val availabilityStatus: String = "",
    @SerialName("availability_reason_key") val availabilityReasonKey: String? = null,
)

/**
 * [catalogVersion] tidak dikirim kembali saat sesi dibuat — berbeda dari
 * `card_catalog_version`. Server mencatatnya sendiri dari katalog yang sedang berlaku,
 * beserta setoran awal yang ditampilkan.
 */
@Serializable
data class SavingsProductCatalogResponse(
    @SerialName("catalog_version") val catalogVersion: String = "",
    val page: ProductPageDto? = null,
    val products: List<ProductOptionDto> = emptyList(),
)
