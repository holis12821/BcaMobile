package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// -- Data Model ---------------------------------------------------------------

data class JenisRekening(
    val nama: String,
    val deskripsi: String,
    val setoranAwalMinimum: String,
    val fitur: List<String>,
    val iconRes: Int,
    val iconBackgroundColor: Color,
    val iconTintColor: Color,
    val isPalingPopuler: Boolean = false,
)

data class BukaRekeningPilihJenisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val jenisRekeningList: List<JenisRekening> = emptyList(),
)

// -- Default Data (from string resources) -------------------------------------

@Composable
fun defaultJenisRekeningList(): List<JenisRekening> = listOf(
    JenisRekening(
        nama = stringResource(R.string.buka_rekening_tahapan_bca),
        deskripsi = stringResource(R.string.buka_rekening_tahapan_bca_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tahapan_bca_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_1),
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_2),
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_3),
        ),
        iconRes = R.drawable.ic_account_balance_wallet,
        iconBackgroundColor = AppColor.Primary100,
        iconTintColor = AppColor.Primary900,
        isPalingPopuler = true,
    ),
    JenisRekening(
        nama = stringResource(R.string.buka_rekening_tahapan_xpresi),
        deskripsi = stringResource(R.string.buka_rekening_tahapan_xpresi_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tahapan_xpresi_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_1),
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_2),
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_3),
        ),
        iconRes = R.drawable.ic_credit_card,
        iconBackgroundColor = AppColor.Secondary100,
        iconTintColor = AppColor.Secondary900,
    ),
    JenisRekening(
        nama = stringResource(R.string.buka_rekening_tabunganku),
        deskripsi = stringResource(R.string.buka_rekening_tabunganku_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tabunganku_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tabunganku_fitur_1),
            stringResource(R.string.buka_rekening_tabunganku_fitur_2),
        ),
        iconRes = R.drawable.ic_savings,
        iconBackgroundColor = AppColor.Neutral200,
        iconTintColor = AppColor.Neutral900,
    ),
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningPilihJenisScreen(
    state: BukaRekeningPilihJenisUiState,
    onJenisSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_baru_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (!state.isLoading && state.error == null && state.jenisRekeningList.isNotEmpty()) {
                Surface(
                    shadowElevation = Spacing.s1,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Button(
                        onClick = { onJenisSelected(selectedIndex) },
                        shape = AppShape.R6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.s4),
                    ) {
                        Text(
                            text = stringResource(R.string.buka_rekening_lanjut),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(Spacing.s2))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.size(AppSize.IconSmall),
                        )
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
                            currentStep = 1,
                            totalSteps = 4,
                            stepLabel = stringResource(R.string.buka_rekening_pilih_produk),
                        )
                        Spacer(Modifier.height(Spacing.s3))
                        Text(
                            text = stringResource(R.string.buka_rekening_pilih_jenis_heading),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.s1))
                        Text(
                            text = stringResource(R.string.buka_rekening_pilih_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Account type cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                        modifier = Modifier.padding(
                            horizontal = Spacing.s4,
                            vertical = Spacing.s2,
                        ),
                    ) {
                        state.jenisRekeningList.forEachIndexed { index, jenis ->
                            JenisRekeningCard(
                                jenis = jenis,
                                isSelected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                            )
                        }

                        // Info box
                        InfoBox()

                        // Terms text
                        TermsText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.s2),
                        )
                    }
                }
            }
        }
    }
}

// -- Jenis Rekening Card -------------------------------------------------------

@Composable
private fun JenisRekeningCard(
    jenis: JenisRekening,
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, AppShape.R6)
            .clip(AppShape.R6)
            .background(cardBackground)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.s4)) {
            // Header: Icon + Name + Selection indicator
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .background(jenis.iconBackgroundColor, AppShape.R6),
                ) {
                    Icon(
                        painter = painterResource(jenis.iconRes),
                        contentDescription = null,
                        tint = jenis.iconTintColor,
                        modifier = Modifier.size(Spacing.s6),
                    )
                }

                // Title + description
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            end = if (jenis.isPalingPopuler) Spacing.s10 else Spacing.s0,
                        ),
                ) {
                    Text(
                        text = jenis.nama,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.s0))
                    Text(
                        text = jenis.deskripsi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Selection circle
                SelectionIndicator(isSelected = isSelected)
            }

            Spacer(Modifier.height(Spacing.s4))

            // Setoran Awal Minimum row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        AppShape.R6,
                    )
                    .padding(Spacing.s3),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_setoran_awal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = jenis.setoranAwalMinimum,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(Spacing.s3))

            // Features grid — 2 columns, last item spans full width if odd count
            FeaturesGrid(fitur = jenis.fitur)
        }

        // "Paling Populer" badge
        if (jenis.isPalingPopuler) {
            Text(
                text = stringResource(R.string.buka_rekening_paling_populer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(bottomStart = Spacing.s3),
                    )
                    .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
            )
        }
    }
}

// -- Features Grid (2-col with col-span) --------------------------------------

@Composable
private fun FeaturesGrid(
    fitur: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        // Items that fit in pairs
        val pairCount = fitur.size / 2
        for (i in 0 until pairCount) {
            val first = fitur[i * 2]
            val second = fitur[i * 2 + 1]
            // If this is the last pair AND there's no remaining item,
            // OR if item count <= 2, render full-width
            if (fitur.size <= 2) {
                FeatureItem(text = first)
                FeatureItem(text = second)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    FeatureItem(
                        text = first,
                        modifier = Modifier.weight(1f),
                    )
                    FeatureItem(
                        text = second,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        // Remaining item (odd count, last one spans full width)
        if (fitur.size % 2 != 0) {
            FeatureItem(text = fitur.last())
        }
    }
}

// -- Feature Item -------------------------------------------------------------

@Composable
private fun FeatureItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_verified),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(AppSize.IconSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -- Info Box -----------------------------------------------------------------

@Composable
private fun InfoBox(modifier: Modifier = Modifier) {
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
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.s5),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
            Text(
                text = stringResource(R.string.buka_rekening_persiapan_dokumen),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.buka_rekening_persiapan_dokumen_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// -- Terms Text ---------------------------------------------------------------

@Composable
private fun TermsText(modifier: Modifier = Modifier) {
    val prefix = stringResource(R.string.buka_rekening_syarat_prefix)
    val link = stringResource(R.string.buka_rekening_syarat_link)
    val suffix = stringResource(R.string.buka_rekening_syarat_suffix)

    Text(
        text = buildAnnotatedString {
            append(prefix)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(link)
            }
            append(suffix)
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// -- Previews ------------------------------------------------------------------

private val previewState = BukaRekeningPilihJenisUiState(
    jenisRekeningList = listOf(
        JenisRekening(
            nama = "Tahapan BCA",
            deskripsi = "Tabungan utama untuk kemudahan transaksi harian dan proteksi finansial keluarga.",
            setoranAwalMinimum = "Rp 500.000",
            fitur = listOf("Debit Mastercard", "m-BCA & KlikBCA", "Bebas tarik tunai di ribuan ATM"),
            iconRes = R.drawable.ic_account_balance_wallet,
            iconBackgroundColor = AppColor.Primary100,
            iconTintColor = AppColor.Primary900,
            isPalingPopuler = true,
        ),
        JenisRekening(
            nama = "Tahapan Xpresi",
            deskripsi = "Tabungan digital untuk anak muda, serba praktis tanpa ribet buku tabungan.",
            setoranAwalMinimum = "Rp 50.000",
            fitur = listOf("Desain Kartu Custom", "m-Banking 24/7", "Biaya admin bulanan sangat ringan"),
            iconRes = R.drawable.ic_credit_card,
            iconBackgroundColor = AppColor.Secondary100,
            iconTintColor = AppColor.Secondary900,
        ),
        JenisRekening(
            nama = "TabunganKu",
            deskripsi = "Tabungan perorangan dengan persyaratan sangat mudah, terjangkau, dan hemat.",
            setoranAwalMinimum = "Rp 20.000",
            fitur = listOf("Tanpa biaya administrasi bulanan", "Bunga tabungan kompetitif"),
            iconRes = R.drawable.ic_savings,
            iconBackgroundColor = AppColor.Neutral200,
            iconTintColor = AppColor.Neutral900,
        ),
    ),
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun BukaRekeningPilihJenisScreenPreview() {
    BcaMobileTheme {
        BukaRekeningPilihJenisScreen(
            state = previewState,
            onJenisSelected = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun BukaRekeningPilihJenisScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        BukaRekeningPilihJenisScreen(
            state = previewState,
            onJenisSelected = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun BukaRekeningPilihJenisScreenLoadingPreview() {
    BcaMobileTheme {
        BukaRekeningPilihJenisScreen(
            state = BukaRekeningPilihJenisUiState(isLoading = true),
            onJenisSelected = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun BukaRekeningPilihJenisScreenErrorPreview() {
    BcaMobileTheme {
        BukaRekeningPilihJenisScreen(
            state = BukaRekeningPilihJenisUiState(
                error = "Terjadi kesalahan. Silakan coba lagi.",
            ),
            onJenisSelected = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}