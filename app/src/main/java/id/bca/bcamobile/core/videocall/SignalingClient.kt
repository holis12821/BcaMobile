package id.bca.bcamobile.core.videocall

/**
 * Peristiwa dari server signaling, sudah bersih dari bentuk wire-nya.
 *
 * SDP dan ICE sengaja dibawa sebagai [String]/field mentah, bukan tipe WebRTC: lapisan
 * signaling tidak boleh ikut bergantung pada pustaka media, supaya bisa diuji tanpa
 * perangkat dan tanpa `PeerConnectionFactory`.
 */
sealed interface SignalingEvent {

    /** Posisi antrean berubah; dikirim berkala selama menunggu. */
    data class QueueUpdate(val position: Int, val estimatedWaitSeconds: Int) : SignalingEvent

    /** Petugas sudah ditugaskan. Inilah pemicu membuat `PeerConnection`, bukan `connect()`. */
    data class AgentAssigned(val name: String, val employeeId: String) : SignalingEvent

    /** Jawaban SDP dari agent atas penawaran kita. */
    data class Answer(val sdp: String) : SignalingEvent

    data class IceCandidate(
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int,
    ) : SignalingEvent

    /** Instruksi CS selama panggilan, mis. "Mohon tunjukkan e-KTP asli Anda ke kamera". */
    data class Instruction(val text: String) : SignalingEvent

    /**
     * Panggilan diakhiri agent. [result] mengikuti `§5c` (`APPROVED`, dst) — tapi client
     * **tidak** menavigasi berdasarkan nilai ini: langkah berikutnya tetap ditanyakan ke
     * server lewat `GET sessions/{id}`.
     */
    data class CallEnded(
        val result: String,
        val agentName: String,
        val durationSeconds: Int,
    ) : SignalingEvent

    /** Kegagalan transport atau pesan error dari server. */
    data class Failed(val message: String) : SignalingEvent

    /** Socket tertutup. Pemanggil yang memutuskan ini perlu disambung ulang atau tidak. */
    data object Disconnected : SignalingEvent
}

/**
 * Kanal signaling WebRTC.
 *
 * Hanya mengantar SDP, ICE, dan kontrol media. Medianya sendiri mengalir langsung
 * antar-peer lewat DTLS-SRTP dan tidak pernah melewati kanal ini.
 */
interface SignalingClient {

    /**
     * Peristiwa masuk. `replay = 0`: peristiwa yang terlewat tidak diulang, karena SDP dan
     * ICE yang datang dua kali membuat negosiasi gagal.
     */
    val events: kotlinx.coroutines.flow.SharedFlow<SignalingEvent>

    /**
     * Menyambung ke [url] — URL utuh dari `signaling_url`, **termasuk** `?token=`.
     * Jangan dirakit dari base URL: tokennya JWT terbitan server dan sekali pakai.
     */
    fun connect(url: String, sessionId: String, queueId: String)

    fun sendOffer(sdp: String)

    fun sendIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int)

    fun sendMediaControl(action: MediaControlAction)

    fun disconnect()
}
