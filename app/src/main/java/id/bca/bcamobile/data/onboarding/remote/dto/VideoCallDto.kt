package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinQueueRequest(
    @SerialName("session_id") val sessionId: String,
)

@Serializable
data class OperatingHoursDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
)

/**
 * Satu entri STUN/TURN, bentuknya sudah mengikuti `RTCIceServer` — diteruskan
 * apa adanya ke `PeerConnection`, **jangan dipetakan ulang**.
 *
 * `username` dan `credential` `omitempty` di backend: entri STUN murni tidak
 * membawa keduanya.
 */
@Serializable
data class IceServerDto(
    val urls: List<String> = emptyList(),
    val username: String? = null,
    val credential: String? = null,
)

/**
 * Balasan `POST /onboarding/video-call/queue`.
 *
 * **Token di `signaling_url` berumur 5 menit dan sekali pakai** — ditandai
 * terpakai pada sambungan WebSocket pertama. Sambungan yang putus harus join
 * antrean lagi untuk mendapat token baru, bukan menyambung ulang URL yang sama.
 * Karena itu URL ini tidak boleh dirakit dari base URL maupun disimpan.
 */
@Serializable
data class JoinQueueResponse(
    @SerialName("queue_id") val queueId: String,
    @SerialName("queue_number") val queueNumber: String = "",
    val position: Int = 0,
    @SerialName("estimated_wait_seconds") val estimatedWaitSeconds: Int = 0,
    @SerialName("operating_hours") val operatingHours: OperatingHoursDto? = null,
    @SerialName("signaling_url") val signalingUrl: String = "",
    @SerialName("signaling_expires_in") val signalingExpiresIn: Int = 0,
    /**
     * Daftar kosong adalah jawaban yang **sah**: artinya TURN belum
     * dikonfigurasi. Panggilan tetap jadi di jaringan ramah dan gagal di
     * seluler ber-NAT ketat — bedakan ini dari kegagalan.
     */
    @SerialName("ice_servers") val iceServers: List<IceServerDto> = emptyList(),
)
