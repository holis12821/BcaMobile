package id.bca.bcamobile.data.card.mapper

import id.bca.bcamobile.data.card.remote.dto.CardDto
import id.bca.bcamobile.data.card.remote.dto.CardReplacementResponse
import id.bca.bcamobile.domain.card.model.BlockedReason
import id.bca.bcamobile.domain.card.model.CardReplacement
import id.bca.bcamobile.domain.card.model.CardSettings
import id.bca.bcamobile.domain.card.model.CardStatus
import id.bca.bcamobile.domain.card.model.CardStyle
import id.bca.bcamobile.domain.card.model.PaymentCard

/**
 * `status` dipakai apa adanya dari server: kedaluwarsa dihitung di sana dalam
 * zona WIB, dan menghitungnya lagi di sini hanya melahirkan selisih satu hari.
 */
fun CardDto.toDomain(): PaymentCard = PaymentCard(
    cardId = cardId,
    maskedNumber = maskedNumber,
    cardholderName = cardholderName,
    cardType = cardType,
    productName = productName,
    network = network,
    tierKey = tierKey,
    style = CardStyle.fromWire(style),
    validThru = validThru,
    status = CardStatus.fromWire(status),
    isPrimary = isPrimary,
    settings = CardSettings(
        debitOnlineEnabled = settings.debitOnlineEnabled,
        internationalEnabled = settings.internationalEnabled,
    ),
    blockedReason = BlockedReason.fromWire(blockedReason),
)

fun CardReplacementResponse.toDomain(): CardReplacement = CardReplacement(
    requestId = requestId,
    cardId = cardId,
    status = status,
    reason = reason,
    deliveryMethod = deliveryMethod,
    fee = fee,
    estimatedArrivalFrom = estimatedArrivalFrom,
    estimatedArrivalTo = estimatedArrivalTo,
    maskedNumber = maskedNumber,
)
