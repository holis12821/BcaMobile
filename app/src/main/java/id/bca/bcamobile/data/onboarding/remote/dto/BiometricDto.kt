package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Dikirim sebagai JSON string di part `liveness_meta`, bukan sebagai body. */
@Serializable
data class LivenessMetaDto(
    @SerialName("challenge_type") val challengeType: String,
    @SerialName("completed_actions") val completedActions: Int,
    @SerialName("precision_score") val precisionScore: Double,
)

@Serializable
data class BiometricResponse(
    @SerialName("biometric_id") val biometricId: String,
    @SerialName("liveness_verified") val livenessVerified: Boolean = false,
    @SerialName("liveness_score") val livenessScore: Double = 0.0,
    @SerialName("face_match_with_ktp") val faceMatchWithKtp: Boolean = false,
    @SerialName("face_match_score") val faceMatchScore: Double = 0.0,
    @SerialName("iso_30107_compliant") val iso30107Compliant: Boolean = false,
    @SerialName("current_step") val currentStep: String? = null,
)
