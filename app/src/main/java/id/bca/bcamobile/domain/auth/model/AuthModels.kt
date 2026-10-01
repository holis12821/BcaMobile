package id.bca.bcamobile.domain.auth.model

/** Identitas ringkas yang dibalas server setelah login berhasil. */
data class AuthUser(
    val id: String,
    val displayName: String,
    val maskedAccount: String,
)

/**
 * Tantangan login biometrik.
 *
 * [algorithm] dan [signatureFormat] datang **dari server** dan bukan konstanta
 * yang disalin dari dokumen: begitu backend mengganti algoritmanya, client
 * berhenti dengan pesan yang tepat alih-alih mengirim tanda tangan yang pasti
 * ditolak. Keduanya bisa kosong kalau server belum mengirimkannya.
 *
 * [challenge] base64 32 byte, **sekali pakai** dan berumur [expiresInSeconds].
 * Yang ditandatangani adalah isi aslinya setelah di-decode — tanpa `device_id`,
 * tanpa prefiks apa pun.
 */
data class BiometricChallenge(
    val challengeId: String,
    val challenge: String,
    val expiresInSeconds: Int,
    val algorithm: String,
    val signatureFormat: String,
)

/**
 * Hasil pendaftaran biometrik.
 *
 * [replacedKeys] menghitung kunci yang dicabut oleh pendaftaran ini pada
 * perangkat yang sama — nilainya > 0 saat nasabah mendaftar ulang setelah
 * sidik jarinya berubah.
 */
data class BiometricRegistration(
    val biometricId: String,
    val keyId: String,
    val replacedKeys: Int,
)

enum class BiometricType(val wireValue: String) {
    FACE_ID("FACE_ID"),
    FINGERPRINT("FINGERPRINT"),
}

/**
 * Token verifikasi PIN untuk satu transaksi. Berlaku singkat (kontrak: 120 detik),
 * jadi hanya boleh hidup di state ViewModel — jangan dipersistensi.
 */
data class PinVerification(
    val verificationToken: String,
    val expiresInSeconds: Int,
)

/** Tujuan verifikasi PIN; server memakainya untuk membatasi cakupan token. */
enum class PinPurpose(val wireValue: String) {
    TRANSFER("TRANSFER"),
    EWALLET_TOPUP("EWALLET_TOPUP"),

    /**
     * Pembayaran QRIS.
     *
     * Nilai wire-nya `PAYMENT` mengikuti `docs/backend/01-API-SPECIFICATION.md` §2
     * (`TRANSFER, EWALLET_TOPUP, PAYMENT`). Skill `bca-mobile-api` menyebut
     * `QRIS_PAYMENT` dan client sebelumnya mengirim `QRIS` — keduanya tidak ada di
     * spec. Selisih ini dilaporkan, bukan diakali dengan menebak.
     */
    QRIS_PAYMENT("PAYMENT"),

    /**
     * Ubah limit transaksi. Spec menyebut endpoint-nya butuh verifikasi PIN tetapi
     * tidak menyebut nilai `purpose`-nya; nilai ini mengikuti skill `bca-mobile-api`.
     */
    CHANGE_LIMIT("CHANGE_LIMIT"),

    /**
     * Blokir kartu. Nilainya dibaca dari `card.PurposeBlockCard`
     * (`internal/domain/card/entity.go`), bukan dari spec.
     */
    BLOCK_CARD("BLOCK_CARD"),

    /**
     * Penggantian kartu — **bukan** `BLOCK_CARD`. Dua aksi berbeda dengan dua
     * cakupan token berbeda; token blokir ditolak di endpoint penggantian.
     */
    REPLACE_CARD("REPLACE_CARD"),
}
