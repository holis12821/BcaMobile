package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Syarat & Ketentuan buka rekening.
 * Kontrak: `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` §0b.
 *
 * Objek bersarang nullable dengan default `null` mengikuti [CardDto]: respons yang cacat
 * jadi keadaan yang bisa ditangani mapper, bukan `SerializationException` di layar
 * pertama buka rekening.
 */
@Serializable
data class TncTrustBannerDto(
    val title: String = "",
    val subtitle: String = "",
)

@Serializable
data class TncSectionDto(
    /** Kunci, bukan path drawable. Dipetakan di layar; kunci asing jatuh ke ikon cadangan. */
    @SerialName("icon_key") val iconKey: String = "",
    val title: String = "",
    val body: String = "",
)

@Serializable
data class TncNoticeDto(
    val label: String = "",
    val body: String = "",
)

/**
 * Tiga potongan, bukan satu kalimat: [link] dicetak tebal dan berwarna lewat
 * `buildAnnotatedString`. Menyatukannya lalu mencari substring akan pecah pada setiap
 * perbaikan kata di sisi server.
 */
@Serializable
data class TncConsentDto(
    val prefix: String = "",
    val link: String = "",
    val suffix: String = "",
)

@Serializable
data class TncResponse(
    val version: String = "",
    val heading: String = "",
    val subtitle: String = "",
    @SerialName("trust_banner") val trustBanner: TncTrustBannerDto? = null,
    val sections: List<TncSectionDto> = emptyList(),
    val notice: TncNoticeDto? = null,
    val consent: TncConsentDto? = null,
    @SerialName("agree_cta") val agreeCta: String = "",
    @SerialName("effective_from") val effectiveFrom: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
)
