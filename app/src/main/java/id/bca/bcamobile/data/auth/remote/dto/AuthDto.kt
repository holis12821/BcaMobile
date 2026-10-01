package id.bca.bcamobile.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -- Login -------------------------------------------------------------------

@Serializable
data class DeviceInfoDto(
    val model: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("app_version") val appVersion: String,
)

@Serializable
data class LoginPinRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("pin_encrypted") val pinEncrypted: String,
    @SerialName("device_info") val deviceInfo: DeviceInfoDto,
    /**
     * Opsional di server, tapi selalu dikirim: tanpa ini kunci yang sudah basi
     * terbaca sebagai PIN salah — dan PIN salah punya lockout, jadi nasabah bisa
     * terkunci karena kesalahan yang bukan miliknya.
     */
    @SerialName("encryption_key_id") val encryptionKeyId: String? = null,
)

@Serializable
data class LoginUserDto(
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("masked_account") val maskedAccount: String = "",
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val user: LoginUserDto? = null,
)

// -- Biometrik ---------------------------------------------------------------

/**
 * Balasan `GET /auth/biometric/challenge`.
 *
 * Response **membawa kontraknya sendiri**: `algorithm` dan `signature_format`
 * memberi tahu client persis apa yang harus diproduksi, jadi client membacanya
 * dari sini dan tidak menyalin konstanta dari prosa dokumen.
 *
 * `challenge` berumur 60 detik dan **benar-benar sekali pakai** — server
 * memakai `GETDEL`, jadi challenge yang sudah dipakai dijawab
 * `401 AUTH_TOKEN_INVALID`, bukan hanya yang kedaluwarsa. Jangan pernah
 * mencoba ulang dengan challenge yang sama; ambil yang baru.
 */
@Serializable
data class BiometricChallengeResponse(
    @SerialName("challenge_id") val challengeId: String,
    val challenge: String,
    @SerialName("expires_in") val expiresIn: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null,
    /** Mis. `EC-P256`. Kosong berarti server versi lama yang belum mengirimnya. */
    val algorithm: String = "",
    @SerialName("signature_format") val signatureFormat: String = "",
)

@Serializable
data class LoginBiometricRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("biometric_type") val biometricType: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("signed_challenge") val signedChallenge: String,
    @SerialName("key_id") val keyId: String,
)

@Serializable
data class RegisterBiometricRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("biometric_type") val biometricType: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("key_id") val keyId: String,
    val attestation: String? = null,
)

/**
 * Balasan `POST /auth/biometric/register`.
 *
 * **Pendaftaran ulang MENGGANTI**: sidik jari baru menghanguskan kunci
 * Keystore, aplikasi mendaftar lagi, dan semua kunci aktif nasabah pada
 * perangkat itu dicabut. Jumlahnya dilaporkan lewat `replaced_keys`.
 * Pencabutan dibatasi satu perangkat — beberapa perangkat per nasabah boleh.
 */
@Serializable
data class RegisterBiometricResponse(
    @SerialName("biometric_id") val biometricId: String = "",
    @SerialName("key_id") val keyId: String = "",
    @SerialName("registered_at") val registeredAt: String? = null,
    @SerialName("replaced_keys") val replacedKeys: Int = 0,
)

// -- Token -------------------------------------------------------------------

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int = 0,
)

@Serializable
data class LogoutRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("revoke_all_devices") val revokeAllDevices: Boolean = false,
)

@Serializable
data class MessageResponse(
    val message: String = "",
)

// -- PIN ---------------------------------------------------------------------

@Serializable
data class VerifyPinRequest(
    @SerialName("pin_encrypted") val pinEncrypted: String,
    val purpose: String,
    @SerialName("encryption_key_id") val encryptionKeyId: String? = null,
)

@Serializable
data class VerifyPinResponse(
    @SerialName("verification_token") val verificationToken: String,
    @SerialName("expires_in") val expiresIn: Int = 0,
)

@Serializable
data class ChangePinRequest(
    @SerialName("old_pin_encrypted") val oldPinEncrypted: String,
    @SerialName("new_pin_encrypted") val newPinEncrypted: String,
    @SerialName("encryption_key_id") val encryptionKeyId: String? = null,
)
