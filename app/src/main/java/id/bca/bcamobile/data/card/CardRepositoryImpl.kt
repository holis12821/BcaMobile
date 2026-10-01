package id.bca.bcamobile.data.card

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.data.card.mapper.toDomain
import id.bca.bcamobile.data.card.remote.CardApi
import id.bca.bcamobile.data.card.remote.dto.BlockCardRequest
import id.bca.bcamobile.data.card.remote.dto.CardReplacementRequest
import id.bca.bcamobile.data.card.remote.dto.UpdateCardSettingsRequest
import id.bca.bcamobile.domain.card.CardRepository
import id.bca.bcamobile.domain.card.model.BlockedReason
import id.bca.bcamobile.domain.card.model.CardReplacement
import id.bca.bcamobile.domain.card.model.DeliveryMethod
import id.bca.bcamobile.domain.card.model.PaymentCard
import id.bca.bcamobile.domain.card.model.ReplacementReason
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val api: CardApi,
    private val caller: ApiCaller,
) : CardRepository {

    override suspend fun cards(): DataResult<List<PaymentCard>> =
        caller.call { api.cards() }.map { dto -> dto.cards.map { it.toDomain() } }

    override suspend fun updateSettings(
        cardId: String,
        debitOnlineEnabled: Boolean?,
        internationalEnabled: Boolean?,
    ): DataResult<PaymentCard> = caller.call {
        api.updateSettings(
            cardId = cardId,
            request = UpdateCardSettingsRequest(
                debitOnlineEnabled = debitOnlineEnabled,
                internationalEnabled = internationalEnabled,
            ),
        )
    }.map { it.card.toDomain() }

    /**
     * Tidak di-retry otomatis: token verifikasi sekali pakai, jadi percobaan
     * kedua dengan token yang sama pasti dijawab `AUTH_TOKEN_INVALID` dan
     * nasabah melihat kegagalan yang salah sebabnya.
     */
    override suspend fun block(
        cardId: String,
        reason: BlockedReason,
        verificationToken: String,
    ): DataResult<PaymentCard> = caller.call(allowRetry = false) {
        api.block(
            cardId = cardId,
            request = BlockCardRequest(
                reason = reason.wireValue,
                verificationToken = verificationToken,
            ),
        )
    }.map { it.card.toDomain() }

    /**
     * Retry ditangani server lewat kunci idempotensi, bukan oleh [ApiCaller]:
     * percobaan ulang dengan kunci yang sama dijawab body identik, jadi
     * mengulang di sini hanya menggandakan jendela balapannya.
     */
    override suspend fun requestReplacement(
        cardId: String,
        reason: ReplacementReason,
        deliveryMethod: DeliveryMethod,
        verificationToken: String,
        idempotencyKey: String,
    ): DataResult<CardReplacement> = caller.call(allowRetry = false) {
        api.requestReplacement(
            cardId = cardId,
            idempotencyKey = idempotencyKey,
            request = CardReplacementRequest(
                reason = reason.wireValue,
                deliveryMethod = deliveryMethod.wireValue,
                verificationToken = verificationToken,
            ),
        )
    }.map { it.toDomain() }
}
