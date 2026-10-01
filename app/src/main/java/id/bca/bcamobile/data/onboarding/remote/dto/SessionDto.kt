package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    @SerialName("product_type") val productType: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("accepted_tnc_version") val acceptedTncVersion: String,
    /**
     * Kosong berarti client belum menampilkan layar kartu; server menjawab
     * `current_step: CARD_SELECTION` alih-alih menolak.
     */
    @SerialName("card_type") val cardType: String? = null,
    /** Versi katalog yang dilihat nasabah; dicatat server untuk audit biaya. */
    @SerialName("card_catalog_version") val cardCatalogVersion: String? = null,
)

@Serializable
data class ProductDto(
    val type: String,
    val name: String,
    val currency: String,
    @SerialName("min_initial_deposit") val minInitialDeposit: Long,
    val features: List<String> = emptyList(),
)

@Serializable
data class CreateSessionResponse(
    @SerialName("session_id") val sessionId: String,
    val product: ProductDto? = null,
    val card: SessionCardDto? = null,
    @SerialName("current_step") val currentStep: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class StepsCompletedDto(
    @SerialName("tnc_accepted") val tncAccepted: Boolean = false,
    @SerialName("card_selected") val cardSelected: Boolean = false,
    @SerialName("ocr_verified") val ocrVerified: Boolean = false,
    @SerialName("personal_data_saved") val personalDataSaved: Boolean = false,
    @SerialName("otp_verified") val otpVerified: Boolean = false,
    @SerialName("biometric_verified") val biometricVerified: Boolean = false,
    @SerialName("video_call_verified") val videoCallVerified: Boolean = false,
    @SerialName("credentials_set") val credentialsSet: Boolean = false,
    val submitted: Boolean = false,
)

@Serializable
data class GetSessionResponse(
    @SerialName("session_id") val sessionId: String,
    val product: ProductDto? = null,
    val card: SessionCardDto? = null,
    @SerialName("current_step") val currentStep: String? = null,
    @SerialName("steps_completed") val stepsCompleted: StepsCompletedDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class DeleteResponse(
    val deleted: Boolean = false,
)
