package id.bca.bcamobile.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Amplop response standar backend onboarding.
 * Kontrak: bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md
 */
@Serializable
data class ApiEnvelope<T>(
    val status: String,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ApiMeta? = null,
    val pagination: PaginationDto? = null,
) {
    val isSuccess: Boolean get() = status == STATUS_SUCCESS

    companion object {
        const val STATUS_SUCCESS = "success"
    }
}

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: JsonElement? = null,
)

@Serializable
data class ApiMeta(
    @SerialName("request_id") val requestId: String? = null,
    val timestamp: String? = null,
)

/**
 * Pagination cursor-based. Tampil sebagai saudara `data`, bukan di dalamnya —
 * lihat `bca-mobile-api/docs/01-API-SPECIFICATION.md` §4.
 */
@Serializable
data class PaginationDto(
    val cursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
    val limit: Int = 0,
)
