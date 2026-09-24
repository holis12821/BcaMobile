package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.CardArt
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------

/** Varian material kartu; menentukan gradient kartu dan chip, bukan perilaku. */
enum class KartuPasporGaya { BLUE, GOLD, PLATINUM }

data class KartuPaspor(
    val nama: String,
    val tipeLabel: String,
    val badge: String,
    val biayaAdmin: String,
    val limitTarikTunai: String,
    val limitTransferBca: String,
    val limitAntarBank: String,
    val limitDebit: String,
    val gaya: KartuPasporGaya,
    val isPalingPopuler: Boolean = false,
)

data class BukaRekeningPilihKartuUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val kartuList: List<KartuPaspor> = emptyList(),
    val selectedIndex: Int = 0,
)

// -- Default Data (from string resources) -------------------------------------

@Composable
fun defaultKartuPasporList(): List<KartuPaspor> = listOf(
    KartuPaspor(
        nama = stringResource(R.string.buka_rekening_kartu_blue_nama),
        tipeLabel = stringResource(R.string.buka_rekening_kartu_blue_tipe_label),
        badge = stringResource(R.string.buka_rekening_kartu_blue_badge),
        biayaAdmin = stringResource(R.string.buka_rekening_kartu_blue_biaya),
        limitTarikTunai = stringResource(R.string.buka_rekening_kartu_blue_tarik_tunai),
        limitTransferBca = stringResource(R.string.buka_rekening_kartu_blue_transfer_bca),
        limitAntarBank = stringResource(R.string.buka_rekening_kartu_blue_antar_bank),
        limitDebit = stringResource(R.string.buka_rekening_kartu_blue_debit),
        gaya = KartuPasporGaya.BLUE,
        isPalingPopuler = true,
    ),
    KartuPaspor(
        nama = stringResource(R.string.buka_rekening_kartu_gold_nama),
        tipeLabel = stringResource(R.string.buka_rekening_kartu_gold_tipe_label),
        badge = stringResource(R.string.buka_rekening_kartu_gold_badge),
        biayaAdmin = stringResource(R.string.buka_rekening_kartu_gold_biaya),
        limitTarikTunai = stringResource(R.string.buka_rekening_kartu_gold_tarik_tunai),
        limitTransferBca = stringResource(R.string.buka_rekening_kartu_gold_transfer_bca),
        limitAntarBank = stringResource(R.string.buka_rekening_kartu_gold_antar_bank),
        limitDebit = stringResource(R.string.buka_rekening_kartu_gold_debit),
        gaya = KartuPasporGaya.GOLD,
    ),
    KartuPaspor(
        nama = stringResource(R.string.buka_rekening_kartu_platinum_nama),
        tipeLabel = stringResource(R.string.buka_rekening_kartu_platinum_tipe_label),
        badge = stringResource(R.string.buka_rekening_kartu_platinum_badge),
        biayaAdmin = stringResource(R.string.buka_rekening_kartu_platinum_biaya),
        limitTarikTunai = stringResource(R.string.buka_rekening_kartu_platinum_tarik_tunai),
        limitTransferBca = stringResource(R.string.buka_rekening_kartu_platinum_transfer_bca),
        limitAntarBank = stringResource(R.string.buka_rekening_kartu_platinum_antar_bank),
        limitDebit = stringResource(R.string.buka_rekening_kartu_platinum_debit),
        gaya = KartuPasporGaya.PLATINUM,
    ),
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningPilihKartuScreen(
    state: BukaRekeningPilihKartuUiState,
    onKartuSelected: (Int) -> Unit,
    onLanjutClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_kartu_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (!state.isLoading && state.error == null && state.kartuList.isNotEmpty()) {
                Surface(
                    shadowElevation = Spacing.s1,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                        modifier = Modifier.padding(Spacing.s4),
                    ) {
                        Button(
                            onClick = onLanjutClick,
                            shape = AppShape.R6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = AppSize.MinTouchTarget),
                        ) {
                            Text(
                                text = stringResource(R.string.buka_rekening_kartu_lanjut),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.width(Spacing.s2))
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_forward),
                                contentDescription = null,
                                modifier = Modifier.size(AppSize.IconSmall),
                            )
                        }
                        OjkLpsFooter()
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.s4),
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.s4))
                    TextButton(onClick = onRetry) {
                        Text(
                            text = stringResource(R.string.mutasi_coba_lagi),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Step indicator + heading
                    Column(
                        modifier = Modifier.padding(
                            start = Spacing.s4,
                            end = Spacing.s4,
                            top = Spacing.s4,
                            bottom = Spacing.s2,
                        ),
                    ) {
                        StepProgressIndicator(
                            currentStep = 2,
                            totalSteps = 7,
                            stepLabel = stringResource(R.string.buka_rekening_kartu_step_label),
                        )
                        Spacer(Modifier.height(Spacing.s3))
                        Text(
                            text = stringResource(R.string.buka_rekening_kartu_heading),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.s1))
                        Text(
                            text = stringResource(R.string.buka_rekening_kartu_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Card options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                        modifier = Modifier.padding(
                            horizontal = Spacing.s4,
                            vertical = Spacing.s2,
                        ),
                    ) {
                        state.kartuList.forEachIndexed { index, kartu ->
                            KartuPasporCard(
                                kartu = kartu,
                                isSelected = state.selectedIndex == index,
                                onClick = { onKartuSelected(index) },
                            )
                        }

                        PengirimanKartuNote()
                    }

                    Spacer(Modifier.height(Spacing.s4))
                }
            }
        }
    }
}

// -- Kartu Paspor Card ---------------------------------------------------------

@Composable
private fun KartuPasporCard(
    kartu: KartuPaspor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBackground = if (isSelected) {
        AppColor.Primary100.copy(alpha = AppAlpha.A20)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val elevation = if (isSelected) Spacing.s1 else Spacing.s0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, AppShape.R6)
            .clip(AppShape.R6)
            .background(cardBackground)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(Spacing.s4),
    ) {
        // Badges + selection indicator
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                modifier = Modifier.weight(1f),
            ) {
                if (kartu.isPalingPopuler) {
                    KartuBadge(
                        text = stringResource(R.string.buka_rekening_paling_populer),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                KartuBadge(
                    text = kartu.badge,
                    containerColor = kartu.gaya.badgeContainerColor(),
                    contentColor = kartu.gaya.badgeContentColor(),
                )
            }

            SelectionIndicator(isSelected = isSelected)
        }

        Spacer(Modifier.height(Spacing.s3))

        KartuPasporArt(kartu = kartu)

        Spacer(Modifier.height(Spacing.s4))

        // Biaya administrasi + limit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R4)
                .padding(Spacing.s3),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_biaya_admin),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = kartu.biayaAdmin,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_kartu_per_bulan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.s2))

            LimitGrid(kartu = kartu)
        }
    }
}

// -- Badge ---------------------------------------------------------------------

@Composable
private fun KartuBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = contentColor,
        modifier = modifier
            .background(containerColor, AppShape.Full)
            .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
    )
}

// -- Render miniatur kartu fisik ----------------------------------------------

@Composable
private fun KartuPasporArt(
    kartu: KartuPaspor,
    modifier: Modifier = Modifier,
) {
    val gaya = kartu.gaya

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(AppSize.DebitCardHeight)
            .clip(AppShape.R6)
            .background(gaya.cardBrush())
            .padding(Spacing.s4),
    ) {
        // BCA | PASPOR + contactless
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_brand),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = gaya.onCardColor(),
                )
                Box(
                    modifier = Modifier
                        .width(StrokeWidth.w0)
                        .height(Spacing.s3)
                        .background(gaya.onCardColor().copy(alpha = AppAlpha.A30)),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_paspor),
                    style = MaterialTheme.typography.labelSmall,
                    color = gaya.onCardMutedColor(),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_contactless),
                contentDescription = null,
                tint = gaya.onCardMutedColor(),
                modifier = Modifier.size(Spacing.s5),
            )
        }

        // Chip EMV + label tipe
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmvChip(gaya = gaya)
            Text(
                text = kartu.tipeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = gaya.onCardMutedColor(),
            )
        }

        // Tipe kartu + emblem Mastercard
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.buka_rekening_kartu_tipe),
                    style = MaterialTheme.typography.labelSmall,
                    color = gaya.onCardMutedColor(),
                )
                Text(
                    text = kartu.nama,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = gaya.onCardColor(),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_mastercard),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(AppSize.CardChipWidth)
                    .height(Spacing.s6),
            )
        }
    }
}

// -- Chip EMV ------------------------------------------------------------------

@Composable
private fun EmvChip(
    gaya: KartuPasporGaya,
    modifier: Modifier = Modifier,
) {
    val lineColor = gaya.chipLineColor()

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(AppSize.CardChipWidth)
            .height(AppSize.CardChipHeight)
            .clip(AppShape.R3)
            .background(gaya.chipBrush())
            .padding(Spacing.s1),
    ) {
        ChipContactLine(color = lineColor, widthFraction = FULL_WIDTH)
        ChipContactLine(color = lineColor, widthFraction = TWO_THIRD_WIDTH)
        ChipContactLine(color = lineColor, widthFraction = FULL_WIDTH)
    }
}

@Composable
private fun ChipContactLine(
    color: Color,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(StrokeWidth.w0)
            .background(color),
    )
}

// -- Limit grid ----------------------------------------------------------------

@Composable
private fun LimitGrid(
    kartu: KartuPaspor,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R2)
            .padding(Spacing.s2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            LimitItem(
                label = stringResource(R.string.buka_rekening_kartu_limit_tarik_tunai),
                value = kartu.limitTarikTunai,
                modifier = Modifier.weight(1f),
            )
            LimitItem(
                label = stringResource(R.string.buka_rekening_kartu_limit_transfer_bca),
                value = kartu.limitTransferBca,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            LimitItem(
                label = stringResource(R.string.buka_rekening_kartu_limit_antar_bank),
                value = kartu.limitAntarBank,
                modifier = Modifier.weight(1f),
            )
            LimitItem(
                label = stringResource(R.string.buka_rekening_kartu_limit_debit),
                value = kartu.limitDebit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LimitItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -- Catatan pengiriman kartu --------------------------------------------------

@Composable
private fun PengirimanKartuNote(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(
                AppColor.Secondary100.copy(alpha = AppAlpha.A50),
                AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_local_shipping),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(Spacing.s6),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
            Text(
                text = stringResource(R.string.buka_rekening_kartu_pengiriman_judul),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.buka_rekening_kartu_pengiriman_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Footer OJK / LPS ----------------------------------------------------------

@Composable
private fun OjkLpsFooter(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_verified_user),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Spacing.s4),
        )
        Text(
            text = stringResource(R.string.buka_rekening_kartu_ojk_lps),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Pemetaan gaya kartu ke token ---------------------------------------------

private fun KartuPasporGaya.cardBrush(): Brush = when (this) {
    KartuPasporGaya.BLUE -> Brush.linearGradient(
        listOf(CardArt.BlueStart, CardArt.BlueEnd),
    )

    KartuPasporGaya.GOLD -> Brush.linearGradient(
        listOf(CardArt.GoldStart, CardArt.GoldMid, CardArt.GoldEnd),
    )

    KartuPasporGaya.PLATINUM -> Brush.linearGradient(
        listOf(CardArt.PlatinumStart, CardArt.PlatinumMid, CardArt.PlatinumEnd),
    )
}

private fun KartuPasporGaya.chipBrush(): Brush = when (this) {
    KartuPasporGaya.BLUE -> Brush.horizontalGradient(
        listOf(CardArt.ChipBlueStart, CardArt.ChipBlueEnd),
    )

    KartuPasporGaya.GOLD -> Brush.horizontalGradient(
        listOf(CardArt.ChipGoldStart, CardArt.ChipGoldEnd),
    )

    KartuPasporGaya.PLATINUM -> Brush.horizontalGradient(
        listOf(CardArt.ChipPlatinumStart, CardArt.ChipPlatinumEnd),
    )
}

private fun KartuPasporGaya.chipLineColor(): Color = when (this) {
    KartuPasporGaya.PLATINUM -> CardArt.PlatinumEnd.copy(alpha = AppAlpha.A50)
    else -> CardArt.ChipLine.copy(alpha = AppAlpha.A50)
}

private fun KartuPasporGaya.onCardColor(): Color = when (this) {
    KartuPasporGaya.PLATINUM -> CardArt.OnCardPlatinum
    else -> CardArt.OnCard
}

private fun KartuPasporGaya.onCardMutedColor(): Color = when (this) {
    KartuPasporGaya.PLATINUM -> CardArt.OnCardPlatinumMuted
    else -> CardArt.OnCard.copy(alpha = AppAlpha.A80)
}

@Composable
private fun KartuPasporGaya.badgeContainerColor(): Color = when (this) {
    KartuPasporGaya.BLUE -> MaterialTheme.colorScheme.secondaryContainer
    KartuPasporGaya.GOLD -> MaterialTheme.colorScheme.surfaceContainerHighest
    KartuPasporGaya.PLATINUM -> MaterialTheme.colorScheme.inverseSurface
}

@Composable
private fun KartuPasporGaya.badgeContentColor(): Color = when (this) {
    KartuPasporGaya.BLUE -> MaterialTheme.colorScheme.onSecondaryContainer
    KartuPasporGaya.GOLD -> MaterialTheme.colorScheme.onSurfaceVariant
    KartuPasporGaya.PLATINUM -> MaterialTheme.colorScheme.inverseOnSurface
}

private const val FULL_WIDTH = 1f
private const val TWO_THIRD_WIDTH = 0.66f

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true, name = "Light")
@Composable
private fun BukaRekeningPilihKartuScreenPreview() {
    BcaMobileTheme {
        BukaRekeningPilihKartuScreen(
            state = BukaRekeningPilihKartuUiState(kartuList = defaultKartuPasporList()),
            onKartuSelected = {},
            onLanjutClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun BukaRekeningPilihKartuScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        BukaRekeningPilihKartuScreen(
            state = BukaRekeningPilihKartuUiState(
                kartuList = defaultKartuPasporList(),
                selectedIndex = 2,
            ),
            onKartuSelected = {},
            onLanjutClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun BukaRekeningPilihKartuScreenLoadingPreview() {
    BcaMobileTheme {
        BukaRekeningPilihKartuScreen(
            state = BukaRekeningPilihKartuUiState(isLoading = true),
            onKartuSelected = {},
            onLanjutClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun BukaRekeningPilihKartuScreenErrorPreview() {
    BcaMobileTheme {
        BukaRekeningPilihKartuScreen(
            state = BukaRekeningPilihKartuUiState(
                error = stringResource(R.string.mutasi_coba_lagi),
            ),
            onKartuSelected = {},
            onLanjutClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
