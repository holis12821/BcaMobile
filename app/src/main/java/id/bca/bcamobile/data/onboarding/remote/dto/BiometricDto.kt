package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body `POST /onboarding/liveness/challenge`. */
@Serializable
data class LivenessChallengeRequest(
    @SerialName("session_id") val sessionId: String,
    /**
     * Kunci publik perangkat didaftarkan bersamaan dengan permintaan tantangan,
     * supaya server punya kunci untuk memverifikasi tanda tangan saat frame dikirim.
     */
    @SerialName("device_key_id") val deviceKeyId: String,
    @SerialName("device_public_key") val devicePublicKey: String,
    @SerialName("signature_algorithm") val signatureAlgorithm: String,
)

/**
 * Tantangan yang diterbitkan server.
 *
 * `actions` datang dalam urutan yang sudah diacak server dan dipakai **apa adanya**.
 * Client tidak pernah mengurutkan ulang, menyaring, atau menambahkan aksi.
 */
@Serializable
data class LivenessChallengeResponse(
    @SerialName("challenge_id") val challengeId: String,
    val nonce: String,
    val actions: List<String>,
    /** RFC3339 UTC. */
    @SerialName("expires_at") val expiresAt: String,
)

/** Satu entri `step_meta`: urutan, aksi, dan kapan frame bukti diambil. */
@Serializable
data class LivenessStepMetaDto(
    val index: Int,
    val action: String,
    /** Epoch milidetik, dipakai server untuk memeriksa urutan waktu antar langkah. */
    @SerialName("captured_at") val capturedAtMillis: Long,
)

/** Sinyal risiko perangkat; **masukan** untuk server, bukan keputusan client. */
@Serializable
data class DeviceRiskSignalsDto(
    @SerialName("emulator_likely") val emulatorLikely: Boolean,
    @SerialName("root_artifacts") val rootArtifacts: Boolean,
    @SerialName("debugger_attached") val debuggerAttached: Boolean,
)

/**
 * Hasil verifikasi dari server.
 *
 * Field `iso_30107_compliant` **sudah dihapus** dari kontrak. Nilainya dulu diisi
 * mesin biometrik tiruan dan diteruskan ke layar sebagai klaim sertifikasi yang
 * tidak bisa dibuktikan produk ini; sertifikasi ISO/IEC 30107-3 hanya datang dari
 * pengujian lab terakreditasi. Lihat keputusan Q4.
 */
@Serializable
data class BiometricResponse(
    @SerialName("biometric_id") val biometricId: String,
    @SerialName("liveness_verified") val livenessVerified: Boolean = false,
    @SerialName("liveness_score") val livenessScore: Double = 0.0,
    @SerialName("face_match_with_ktp") val faceMatchWithKtp: Boolean = false,
    @SerialName("face_match_score") val faceMatchScore: Double = 0.0,
    @SerialName("current_step") val currentStep: String? = null,
)
