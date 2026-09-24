package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyResponse(
    val algorithm: String = "",
    @SerialName("key_id") val keyId: String = "",
    @SerialName("public_key_pem") val publicKeyPem: String = "",
    val note: String? = null,
)

@Serializable
data class SetCredentialsRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("access_code_encrypted") val accessCodeEncrypted: String,
    @SerialName("pin_encrypted") val pinEncrypted: String,
    @SerialName("encryption_key_id") val encryptionKeyId: String,
)

@Serializable
data class SetCredentialsResponse(
    @SerialName("credential_id") val credentialId: String,
    @SerialName("biometric_login_available") val biometricLoginAvailable: Boolean = false,
    @SerialName("current_step") val currentStep: String? = null,
)
