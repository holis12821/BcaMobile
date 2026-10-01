package id.bca.bcamobile.data.card.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bentuk kartu di kabel, dibaca dari `card.CardResponse`
 * (`internal/domain/card/entity.go`) — bukan dari contoh di spec.
 *
 * Tidak ada warna, hex, maupun URL gambar di sini dan tidak akan ada:
 * `style` dipetakan client ke token `CardArt`.
 */
@Serializable
data class CardDto(
    @SerialName("card_id") val cardId: String = "",
    @SerialName("masked_number") val maskedNumber: String = "",
    @SerialName("cardholder_name") val cardholderName: String = "",
    @SerialName("card_type") val cardType: String = "",
    @SerialName("product_name") val productName: String = "",
    val network: String = "",
    @SerialName("tier_key") val tierKey: String = "",
    val style: String = "",
    @SerialName("valid_thru") val validThru: String = "",
    val status: String = "",
    @SerialName("is_primary") val isPrimary: Boolean = false,
    val settings: CardSettingsDto = CardSettingsDto(),

    /**
     * `omitempty` di backend: field-nya **absen** kecuali `status == "BLOCKED"`.
     * Karena itu nullable, bukan string kosong.
     */
    @SerialName("blocked_reason") val blockedReason: String? = null,
)

@Serializable
data class CardSettingsDto(
    @SerialName("debit_online_enabled") val debitOnlineEnabled: Boolean = false,
    @SerialName("international_enabled") val internationalEnabled: Boolean = false,
)

/** Balasan `GET /account/cards`. Nasabah tanpa kartu dijawab array kosong. */
@Serializable
data class CardListResponse(
    val cards: List<CardDto> = emptyList(),
)

/** Balasan `PUT .../settings` dan `POST .../block`: kartu sesudah perubahan. */
@Serializable
data class CardEnvelopeResponse(
    val card: CardDto = CardDto(),
)

/**
 * Body `PUT /account/cards/{card_id}/settings`.
 *
 * Kedua field nullable supaya "matikan luar negeri" bisa dibedakan dari
 * "jangan sentuh luar negeri" — `explicitNulls = false` di `NetworkModule`
 * membuang yang null, jadi hanya sakelar yang diubah yang terkirim.
 */
@Serializable
data class UpdateCardSettingsRequest(
    @SerialName("debit_online_enabled") val debitOnlineEnabled: Boolean? = null,
    @SerialName("international_enabled") val internationalEnabled: Boolean? = null,
)

@Serializable
data class BlockCardRequest(
    val reason: String,
    @SerialName("verification_token") val verificationToken: String,
)

/**
 * Body `POST /account/cards/{card_id}/replacement`.
 *
 * Kunci idempotensi **tidak** ikut di body — ia header `X-Idempotency-Key`,
 * sama seperti transfer dan e-wallet.
 */
@Serializable
data class CardReplacementRequest(
    val reason: String,
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("verification_token") val verificationToken: String,
)

/**
 * Balasan `201 Created` penggantian kartu.
 *
 * `fee` integer rupiah — berbeda dari `amount` transaksi yang desimal.
 */
@Serializable
data class CardReplacementResponse(
    @SerialName("request_id") val requestId: String = "",
    @SerialName("card_id") val cardId: String = "",
    val status: String = "",
    val reason: String = "",
    @SerialName("delivery_method") val deliveryMethod: String = "",
    val fee: Long = 0L,
    @SerialName("estimated_arrival_from") val estimatedArrivalFrom: String = "",
    @SerialName("estimated_arrival_to") val estimatedArrivalTo: String = "",
    @SerialName("masked_number") val maskedNumber: String = "",
)
