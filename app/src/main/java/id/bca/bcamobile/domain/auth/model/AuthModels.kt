package id.bca.bcamobile.domain.auth.model

/** Identitas ringkas yang dibalas server setelah login berhasil. */
data class AuthUser(
    val id: String,
    val displayName: String,
    val maskedAccount: String,
)

data class BiometricChallenge(
    val challengeId: String,
    val challenge: String,
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
    QRIS("QRIS"),
}
