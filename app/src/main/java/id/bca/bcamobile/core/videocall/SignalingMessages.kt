package id.bca.bcamobile.core.videocall

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Protokol WebSocket signaling video call.
 * Kontrak: `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` §5b.
 *
 * Dipetakan tangan, bukan lewat polimorfisme kotlinx: `type` yang tidak dikenal harus
 * **diabaikan**, bukan melempar. Server bisa menambah jenis pesan tanpa rilis aplikasi,
 * dan satu pesan asing tidak boleh memutus panggilan yang sedang jalan.
 */

// -- Client -> Server ----------------------------------------------------------

@Serializable
internal data class JoinMessage(
    val type: String = "join",
    @SerialName("session_id") val sessionId: String,
    @SerialName("queue_id") val queueId: String,
)

@Serializable
internal data class SdpMessage(
    /** `offer`; jawaban dari agent datang sebagai `answer`. */
    val type: String,
    val sdp: String,
)

@Serializable
internal data class IceCandidatePayload(
    val candidate: String = "",
    @SerialName("sdpMid") val sdpMid: String? = null,
    @SerialName("sdpMLineIndex") val sdpMLineIndex: Int = 0,
)

@Serializable
internal data class IceCandidateMessage(
    val type: String = "ice_candidate",
    val candidate: IceCandidatePayload,
)

@Serializable
internal data class MediaControlMessage(
    val type: String = "media_control",
    val action: String,
)

// -- Server -> Client ----------------------------------------------------------

@Serializable
internal data class AgentPayload(
    val name: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
)

/**
 * Satu bentuk untuk semua pesan masuk.
 *
 * Field-nya gabungan dari seluruh jenis pesan dan semuanya punya default, jadi pesan
 * apa pun tetap terdeserialisasi; [type] yang menentukan field mana yang berarti.
 * Lebih longgar daripada satu kelas per jenis, dan itu disengaja — pesan baru dari server
 * tidak boleh memutus panggilan.
 */
@Serializable
internal data class IncomingMessage(
    val type: String = "",
    val position: Int? = null,
    @SerialName("estimated_wait_seconds") val estimatedWaitSeconds: Int? = null,
    val agent: AgentPayload? = null,
    val sdp: String? = null,
    val candidate: IceCandidatePayload? = null,
    val text: String? = null,
    val result: String? = null,
    @SerialName("agent_name") val agentName: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val message: String? = null,
)

/** Aksi kontrol media yang dikirim ke agent (`§5b` Client → Server). */
enum class MediaControlAction(val wireValue: String) {
    MUTE_AUDIO("mute_audio"),
    UNMUTE_AUDIO("unmute_audio"),
    SWITCH_CAMERA("switch_camera"),
}

/**
 * Menerjemahkan satu pesan WebSocket jadi [SignalingEvent], atau `null` kalau pesannya
 * tidak kita pedulikan.
 *
 * Fungsi murni dan terpisah dari kliennya supaya seluruh protokol bisa diuji tanpa socket,
 * tanpa perangkat, dan tanpa `PeerConnectionFactory`.
 *
 * `null` **bukan** kegagalan. Dua hal sengaja jatuh ke sana: `type` yang belum dikenal
 * aplikasi, supaya server bisa menambah jenis pesan tanpa rilis; dan pesan cacat, supaya
 * satu JSON rusak tidak memutus panggilan yang sedang berjalan.
 */
internal fun parseSignalingMessage(json: Json, text: String): SignalingEvent? = runCatching {
    val msg = json.decodeFromString<IncomingMessage>(text)
    when (msg.type) {
        TYPE_QUEUE_UPDATE -> SignalingEvent.QueueUpdate(
            position = msg.position ?: 0,
            estimatedWaitSeconds = msg.estimatedWaitSeconds ?: 0,
        )

        // Tanpa objek `agent` tidak ada nama untuk ditampilkan, dan nama kosong di tag
        // petugas lebih buruk daripada tidak menampilkan tagnya sama sekali.
        TYPE_AGENT_ASSIGNED -> msg.agent?.let {
            SignalingEvent.AgentAssigned(name = it.name, employeeId = it.employeeId)
        }

        TYPE_ANSWER -> msg.sdp?.let { SignalingEvent.Answer(it) }

        TYPE_ICE_CANDIDATE -> msg.candidate?.let {
            SignalingEvent.IceCandidate(
                candidate = it.candidate,
                sdpMid = it.sdpMid,
                sdpMLineIndex = it.sdpMLineIndex,
            )
        }

        TYPE_INSTRUCTION -> msg.text?.takeIf { it.isNotBlank() }
            ?.let { SignalingEvent.Instruction(it) }

        TYPE_CALL_ENDED -> SignalingEvent.CallEnded(
            result = msg.result.orEmpty(),
            agentName = msg.agentName.orEmpty(),
            durationSeconds = msg.durationSeconds ?: 0,
        )

        TYPE_ERROR -> SignalingEvent.Failed(msg.message.orEmpty())

        else -> null
    }
}.getOrNull()

internal const val TYPE_OFFER = "offer"
internal const val TYPE_ANSWER = "answer"
internal const val TYPE_QUEUE_UPDATE = "queue_update"
internal const val TYPE_AGENT_ASSIGNED = "agent_assigned"
internal const val TYPE_ICE_CANDIDATE = "ice_candidate"
internal const val TYPE_INSTRUCTION = "instruction"
internal const val TYPE_CALL_ENDED = "call_ended"
internal const val TYPE_ERROR = "error"
