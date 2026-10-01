package id.bca.bcamobile.ui.screen.buka_rekening.pilih_kartu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.onboarding.model.CardBadge
import id.bca.bcamobile.domain.onboarding.model.CardStyle
import id.bca.bcamobile.domain.onboarding.model.CardTier
import id.bca.bcamobile.domain.onboarding.model.CardUnavailableReason
import id.bca.bcamobile.domain.onboarding.model.PasporCard
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningFlowState
import id.bca.bcamobile.ui.screen.buka_rekening.common.formatAmount
import id.bca.bcamobile.ui.screen.buka_rekening.common.resolve

data class BukaRekeningPilihKartuUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val kartuList: List<KartuPaspor> = emptyList(),
    val selectedIndex: Int = 0,
)

@Composable
fun BukaRekeningFlowState.toPilihKartuUiState(): BukaRekeningPilihKartuUiState =
    BukaRekeningPilihKartuUiState(
        isLoading = isLoading,
        error = error?.resolve(),
        kartuList = cardCatalog?.sortedCards?.map { it.toKartuPaspor() }.orEmpty(),
        selectedIndex = selectedCardIndex,
    )

@Composable
private fun PasporCard.toKartuPaspor(): KartuPaspor = KartuPaspor(
    nama = name,
    tipeLabel = stringResource(tier.labelRes()),
    badge = badge.labelRes()?.let { stringResource(it) }.orEmpty(),
    biayaAdmin = rupiah(fees.monthlyAdmin),
    limitTarikTunai = rupiah(limits.cashWithdrawal),
    limitTransferBca = rupiah(limits.transferBca),
    limitAntarBank = rupiah(limits.transferInterbank),
    limitDebit = rupiah(limits.debitPurchase),
    gaya = style.toGaya(),
    isPalingPopuler = isPopular,
    isTersedia = availability.isSelectable,
    keteranganTidakTersedia = availability.takeIf { !it.isSelectable }
        ?.let { stringResource(it.reason.labelRes()) },
)

@Composable
private fun rupiah(amount: Long): String =
    stringResource(R.string.buka_rekening_kartu_rupiah_format, formatAmount(amount))

private fun CardStyle.toGaya(): KartuPasporGaya = when (this) {
    CardStyle.BLUE -> KartuPasporGaya.BLUE
    CardStyle.GOLD -> KartuPasporGaya.GOLD
    CardStyle.PLATINUM -> KartuPasporGaya.PLATINUM
}

private fun CardTier.labelRes(): Int = when (this) {
    CardTier.DEBIT -> R.string.buka_rekening_kartu_tier_debit
    CardTier.PLATINUM_DEBIT -> R.string.buka_rekening_kartu_tier_platinum_debit
}

/** `NONE` berarti server memang tidak mengirim badge; jangan mengarang label. */
private fun CardBadge.labelRes(): Int? = when (this) {
    CardBadge.RECOMMENDED_BEGINNER -> R.string.buka_rekening_kartu_badge_recommended_beginner
    CardBadge.FLEXIBLE_TRANSACTION -> R.string.buka_rekening_kartu_badge_flexible_transaction
    CardBadge.MAX_LIMIT -> R.string.buka_rekening_kartu_badge_max_limit
    CardBadge.NONE -> null
}

private fun CardUnavailableReason?.labelRes(): Int = when (this) {
    CardUnavailableReason.STOCK_EMPTY_IN_REGION ->
        R.string.buka_rekening_kartu_tidak_tersedia_stok
    CardUnavailableReason.TEMPORARILY_DISABLED ->
        R.string.buka_rekening_kartu_tidak_tersedia_nonaktif
    CardUnavailableReason.PRODUCT_MISMATCH ->
        R.string.buka_rekening_kartu_tidak_tersedia_produk
    CardUnavailableReason.AGE_REQUIREMENT ->
        R.string.buka_rekening_kartu_tidak_tersedia_usia
    CardUnavailableReason.UNKNOWN, null ->
        R.string.buka_rekening_kartu_tidak_tersedia_umum
}
