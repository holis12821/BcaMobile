package id.bca.bcamobile.data.card.remote

import id.bca.bcamobile.core.network.ApiEnvelope
import id.bca.bcamobile.data.card.remote.dto.BlockCardRequest
import id.bca.bcamobile.data.card.remote.dto.CardEnvelopeResponse
import id.bca.bcamobile.data.card.remote.dto.CardListResponse
import id.bca.bcamobile.data.card.remote.dto.CardReplacementRequest
import id.bca.bcamobile.data.card.remote.dto.CardReplacementResponse
import id.bca.bcamobile.data.card.remote.dto.UpdateCardSettingsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Kartu milik nasabah — bagian "Manajemen Kartu Paspor" di Profil Saya.
 *
 * Terpisah dari katalog kartu onboarding (`OnboardingApi`): yang ini punya
 * daur hidup (blokir, ganti, kedaluwarsa) yang tidak dikenal katalog.
 */
interface CardApi {

    /** Tanpa parameter: pemiliknya diambil dari access token. */
    @GET("account/cards")
    suspend fun cards(): Response<ApiEnvelope<CardListResponse>>

    @PUT("account/cards/{card_id}/settings")
    suspend fun updateSettings(
        @Path("card_id") cardId: String,
        @Body request: UpdateCardSettingsRequest,
    ): Response<ApiEnvelope<CardEnvelopeResponse>>

    @POST("account/cards/{card_id}/block")
    suspend fun block(
        @Path("card_id") cardId: String,
        @Body request: BlockCardRequest,
    ): Response<ApiEnvelope<CardEnvelopeResponse>>

    /**
     * Berbiaya. Kunci idempotensi dibuat **sekali per niat pengguna** dan
     * dipakai ulang untuk setiap percobaan — kunci baru untuk niat yang sama
     * dijawab `409 CARD_REPLACEMENT_IN_PROGRESS`.
     */
    @POST("account/cards/{card_id}/replacement")
    suspend fun requestReplacement(
        @Path("card_id") cardId: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: CardReplacementRequest,
    ): Response<ApiEnvelope<CardReplacementResponse>>
}
