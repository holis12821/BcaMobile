package id.bca.bcamobile.domain.card.model

/**
 * Kartu yang **dimiliki** nasabah — bukan katalog kartu yang bisa dipilih saat
 * buka rekening. Keduanya sengaja dipisah di backend (`internal/domain/card`
 * vs `internal/domain/onboarding`) karena kartu di sini punya daur hidup
 * sendiri: diblokir, diganti, kedaluwarsa.
 */
data class PaymentCard(
    val cardId: String,
    /**
     * Satu-satunya bentuk nomor kartu yang ada. PAN utuh tidak pernah disimpan
     * backend, jadi tidak ada layar yang boleh menjanjikan "lihat nomor lengkap".
     */
    val maskedNumber: String,
    val cardholderName: String,
    val cardType: String,
    val productName: String,
    val network: String,
    val tierKey: String,
    val style: CardStyle,
    /** `MM/YY`, sudah dirakit server. */
    val validThru: String,
    val status: CardStatus,
    val isPrimary: Boolean,
    val settings: CardSettings,
    /** Terisi **hanya** saat [status] `BLOCKED`; field-nya absen di luar itu. */
    val blockedReason: BlockedReason?,
)

/**
 * Status kartu menurut server.
 *
 * `EXPIRED` dihitung server dari `valid_thru` di zona WIB
 * (`Card.EffectiveStatus`). **Jangan menghitung ulang di client** — dua
 * perhitungan dengan zona berbeda akan berselisih satu hari.
 */
enum class CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED,
    REPLACEMENT_PENDING,

    /** Nilai yang belum dikenal build ini; kartu tetap ditampilkan, aksinya ditutup. */
    UNKNOWN,
    ;

    companion object {
        fun fromWire(value: String): CardStatus =
            entries.firstOrNull { it != UNKNOWN && it.name == value.uppercase() } ?: UNKNOWN
    }
}

/**
 * Bahan visual kartu. Server **tidak** mengirim hex warna maupun URL gambar dan
 * tidak akan — client memetakannya sendiri ke token `CardArt`.
 */
enum class CardStyle {
    BLUE,
    GOLD,
    PLATINUM,
    ;

    companion object {
        /** Gaya yang belum dikenal jatuh ke [BLUE], gaya dasar semua produk. */
        fun fromWire(value: String): CardStyle =
            entries.firstOrNull { it.name == value.uppercase() } ?: BLUE
    }
}

/** Dua sakelar kanal di layar Profil Saya. */
data class CardSettings(
    val debitOnlineEnabled: Boolean,
    val internationalEnabled: Boolean,
)

enum class BlockedReason(val wireValue: String) {
    LOST("LOST"),
    STOLEN("STOLEN"),
    DAMAGED("DAMAGED"),
    SUSPECTED_FRAUD("SUSPECTED_FRAUD"),
    ;

    companion object {
        fun fromWire(value: String?): BlockedReason? =
            entries.firstOrNull { it.wireValue == value?.uppercase() }
    }
}

/**
 * Alasan penggantian kartu. `UPGRADE` ada di sini dan tidak ada di
 * [BlockedReason]: ingin kartu yang lebih baik bukan insiden.
 */
enum class ReplacementReason(val wireValue: String) {
    DAMAGED("DAMAGED"),
    LOST("LOST"),
    UPGRADE("UPGRADE"),
}

enum class DeliveryMethod(val wireValue: String) {
    COURIER("COURIER"),

    /** Tidak selalu tersedia — server menjawab `CARD_DELIVERY_UNAVAILABLE`. */
    BRANCH_PICKUP("BRANCH_PICKUP"),
}

/**
 * Jawaban `POST /account/cards/{id}/replacement`.
 *
 * [fee] **integer rupiah**, bukan desimal seperti `amount` di transaksi.
 * Angkanya dibekukan saat permintaan dibuat: tarif katalog yang berubah nanti
 * tidak mengubah angka yang sudah dijanjikan ke nasabah.
 */
data class CardReplacement(
    val requestId: String,
    val cardId: String,
    val status: String,
    val reason: String,
    val deliveryMethod: String,
    val fee: Long,
    /** `YYYY-MM-DD`. */
    val estimatedArrivalFrom: String,
    val estimatedArrivalTo: String,
    val maskedNumber: String,
)

/** Kode error kartu yang punya perilaku UI sendiri (§1.5 skill). */
object CardErrorCode {
    const val NOT_FOUND = "CARD_NOT_FOUND"
    const val BLOCKED = "CARD_BLOCKED"
    const val REPLACEMENT_IN_PROGRESS = "CARD_REPLACEMENT_IN_PROGRESS"
    const val DELIVERY_UNAVAILABLE = "CARD_DELIVERY_UNAVAILABLE"
    const val TOKEN_INVALID = "AUTH_TOKEN_INVALID"
}
