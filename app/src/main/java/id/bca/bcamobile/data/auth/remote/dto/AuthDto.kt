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

@Serializable
data class BiometricChallengeResponse(
    @SerialName("challenge_id") val challengeId: String,
    val challenge: String,
    @SerialName("expires_at") val expiresAt: String? = null,
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

@Serializable
data class RegisterBiometricResponse(
    @SerialName("biometric_id") val biometricId: String = "",
    @SerialName("registered_at") val registeredAt: String? = null,
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
)
