package id.bca.bcamobile.data.content.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelpItemDto(
    val question: String = "",
    val answer: String = "",
)

@Serializable
data class HelpCategoryDto(
    val key: String = "",
    val title: String = "",
    val items: List<HelpItemDto> = emptyList(),
)

@Serializable
data class HelpCenterResponse(
    val categories: List<HelpCategoryDto> = emptyList(),
)

/** Bentuk dibaca dari `content.ContactCS` (`internal/domain/content/entity.go`). */
@Serializable
data class ContactCsResponse(
    val phone: String = "",
    @SerialName("phone_free") val phoneFree: String = "",
    val whatsapp: String = "",
    val email: String = "",
    @SerialName("chat_url") val chatUrl: String = "",
    val hours: String = "",
)
