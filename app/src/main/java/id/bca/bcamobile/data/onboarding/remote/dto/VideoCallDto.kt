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

@Serializable
data class JoinQueueResponse(
    @SerialName("queue_id") val queueId: String,
    @SerialName("queue_number") val queueNumber: String = "",
    val position: Int = 0,
    @SerialName("estimated_wait_seconds") val estimatedWaitSeconds: Int = 0,
    @SerialName("operating_hours") val operatingHours: OperatingHoursDto? = null,
    @SerialName("signaling_url") val signalingUrl: String = "",
)
