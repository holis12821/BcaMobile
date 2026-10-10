package id.bca.bcamobile.ui.screen.buka_rekening.pilih_jenis

import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.domain.onboarding.model.ProductType
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.bottomBarSafePadding
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.SelectionIndicator
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator

// -- Data Model ---------------------------------------------------------------

/** Gaya visual kartu produk; pemetaan ke token warna ada di [JenisRekeningGaya.warna]. */
enum class JenisRekeningGaya { PRIMARY, SECONDARY, NEUTRAL }

data class JenisRekening(
    /**
     * Identitas produk, ikut di dalam item — termasuk pada daftar bawaan.
     *
     * Inilah yang dikirim kembali saat nasabah menekan Lanjut. Posisi baris tidak pernah
     * dipakai untuk menyimpulkan produk: urutannya milik server.
     */
    val productType: ProductType,
    val nama: String,
    val deskripsi: String,
    val setoranAwalMinimum: String,
    val fitur: List<String>,
    val iconRes: Int,
    val gaya: JenisRekeningGaya,
    /**
     * Teks badge dari `badge_key`; kosong berarti server tidak mengirim badge.
     *
     * Tidak ada `isPalingPopuler` di sini: yang perlu layar hanya teksnya, dan `is_popular`
     * sendiri dibaca layar Ringkasan langsung dari katalog.
     */
    val badge: String = "",
    /** `availability.status` dari server; produk yang tutup tidak bisa dipilih. */
    val isTersedia: Boolean = true,
    /** Alasan singkat saat [isTersedia] false, dari `availability_reason_key`. */
    val keteranganTidakTersedia: String? = null,
)

/** Kunci ikon kotak persiapan dokumen saat katalog server tidak tersedia. */
internal const val ICON_KEY_INFO = "INFO"

/** Kunci asing tetap tampil dengan ikon cadangan — teksnya yang penting. */
internal fun productIconRes(iconKey: String): Int = when (iconKey) {
    "WALLET" -> R.drawable.ic_account_balance_wallet
    "CARD" -> R.drawable.ic_credit_card
    "SAVINGS" -> R.drawable.ic_savings
    "INFO" -> R.drawable.ic_info
    else -> R.drawable.ic_savings
}

// -- Default Data (from string resources) -------------------------------------

/**
 * Daftar bawaan, dipakai saat katalog server tidak tersedia — bukan pratinjau.
 *
 * Tiap entri menyebut [ProductType]-nya sendiri, jadi jalur ini pun tidak bergantung pada
 * urutan baris. Angkanya sengaja sama dengan isi katalog server: nilai yang tampil itulah
 * yang dicatat sebagai "yang dilihat nasabah".
 */
@Composable
fun defaultJenisRekeningList(): List<JenisRekening> = listOf(
    JenisRekening(
        productType = ProductType.TAHAPAN_BCA,
        nama = stringResource(R.string.buka_rekening_tahapan_bca),
        deskripsi = stringResource(R.string.buka_rekening_tahapan_bca_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tahapan_bca_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_1),
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_2),
            stringResource(R.string.buka_rekening_tahapan_bca_fitur_3),
        ),
        iconRes = R.drawable.ic_account_balance_wallet,
        gaya = JenisRekeningGaya.PRIMARY,
        badge = stringResource(R.string.buka_rekening_paling_populer),
    ),
    JenisRekening(
        productType = ProductType.TAHAPAN_XPRESI,
        nama = stringResource(R.string.buka_rekening_tahapan_xpresi),
        deskripsi = stringResource(R.string.buka_rekening_tahapan_xpresi_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tahapan_xpresi_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_1),
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_2),
            stringResource(R.string.buka_rekening_tahapan_xpresi_fitur_3),
        ),
        iconRes = R.drawable.ic_credit_card,
        gaya = JenisRekeningGaya.SECONDARY,
    ),
    JenisRekening(
        productType = ProductType.TABUNGANKU,
        nama = stringResource(R.string.buka_rekening_tabunganku),
        deskripsi = stringResource(R.string.buka_rekening_tabunganku_desc),
        setoranAwalMinimum = stringResource(R.string.buka_rekening_tabunganku_setoran),
        fitur = listOf(
            stringResource(R.string.buka_rekening_tabunganku_fitur_1),
            stringResource(R.string.buka_rekening_tabunganku_fitur_2),
        ),
        iconRes = R.drawable.ic_savings,
        gaya = JenisRekeningGaya.NEUTRAL,
    ),
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningPilihJenisScreen(
    state: BukaRekeningPilihJenisUiState,
    onJenisSelected: (ProductType) -> Unit,
    onLanjutClick: () -> Unit,
    onReloadClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pilihan tinggal di state bersama, bukan di `remember` layar: layar S&K dan layar
    // Pilih Kartu membaca produk yang sama, dan salinan lokal akan melenceng dari keduanya.
    val selected = state.jenisRekeningList.getOrNull(state.selectedIndex)

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
                        onClick = onLanjutClick,
                        // Produk yang sedang tutup tidak diteruskan: server akan
                        // menolaknya dengan 422 dua layar kemudian.
                        enabled = selected?.isTersedia == true,
                        shape = AppShape.R6,
                        modifier = Modifier
                            .bottomBarSafePadding()
                            .fillMaxWidth()
                            .padding(Spacing.s4),
                    ) {
                        Text(
                            text = state.ctaLabel,
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
                            langkah = BukaRekeningLangkah.PILIH_PRODUK,
                        )
                        Spacer(Modifier.height(Spacing.s3))
                        Text(
                            text = state.heading,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.s1))
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Penanda daftar bawaan + jalan untuk mencoba katalog lagi.
                    if (state.isShowingFallback) {
                        FallbackNotice(
                            isRefreshing = state.isRefreshing,
                            onReloadClick = onReloadClick,
                            modifier = Modifier.padding(
                                horizontal = Spacing.s4,
                                vertical = Spacing.s1,
                            ),
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
                                depositLabel = state.depositLabel,
                                isSelected = state.selectedIndex == index,
                                onClick = { onJenisSelected(jenis.productType) },
                            )
                        }

                        // Info box
                        state.notice?.let { InfoBox(notice = it) }

                        // Terms text
                        state.consent?.let {
                            TermsText(
                                consent = it,
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
}

// -- Jenis Rekening Card -------------------------------------------------------

@Composable
private fun JenisRekeningCard(
    jenis: JenisRekening,
    depositLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBackground = if (isSelected) {
        AppColor.Primary100.copy(alpha = AppAlpha.A20)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val (iconBackgroundColor, iconTintColor) = jenis.gaya.warna()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.R6)
            .background(cardBackground)
            .selectable(
                selected = isSelected,
                enabled = jenis.isTersedia,
                role = Role.RadioButton,
                onClick = onClick,
            ),
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
                        .background(iconBackgroundColor, AppShape.R6),
                ) {
                    Icon(
                        painter = painterResource(jenis.iconRes),
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(Spacing.s6),
                    )
                }

                // Title + description
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            end = if (jenis.badge.isNotEmpty()) Spacing.s10 else Spacing.s0,
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

            // Produk yang tutup tetap ditampilkan beserta setoran awalnya, tapi alasannya
            // disebut di sini supaya penolakan tidak muncul belakangan sebagai error saat
            // sesi dibuat.
            if (!jenis.isTersedia && jenis.keteranganTidakTersedia != null) {
                Spacer(Modifier.height(Spacing.s2))
                Text(
                    text = jenis.keteranganTidakTersedia,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(Spacing.s4))

            // Setoran Awal Minimum row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        cardBackground,
                        AppShape.R6,
                    )
                    .padding(Spacing.s3),
            ) {
                Text(
                    text = depositLabel,
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

        // Badge dari server (`badge_key`), mis. "Paling Populer"
        if (jenis.badge.isNotEmpty()) {
            Text(
                text = jenis.badge,
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

/** Satu-satunya tempat gaya produk jadi warna, dan selalu lewat token. */
private fun JenisRekeningGaya.warna(): Pair<Color, Color> = when (this) {
    JenisRekeningGaya.PRIMARY -> AppColor.Primary100 to AppColor.Primary900
    JenisRekeningGaya.SECONDARY -> AppColor.Secondary100 to AppColor.Secondary900
    JenisRekeningGaya.NEUTRAL -> AppColor.Neutral200 to AppColor.Neutral900
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

// -- Penanda daftar bawaan ----------------------------------------------------

/**
 * Baris halus, bukan layar error: daftar di bawahnya tampil penuh dan tombol Lanjut tetap
 * aktif. Yang disampaikan hanya "angka ini mungkin bukan yang terbaru", beserta satu jalan
 * memperbaikinya — tanpa itu tidak ada cara meminta katalog lagi dalam satu sesi layar.
 */
@Composable
private fun FallbackNotice(
    isRefreshing: Boolean,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_jenis_daftar_bawaan),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onReloadClick,
            enabled = !isRefreshing,
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_jenis_muat_ulang),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// -- Info Box -----------------------------------------------------------------

@Composable
private fun InfoBox(
    notice: JenisRekeningNotice,
    modifier: Modifier = Modifier,
) {
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
            painter = painterResource(productIconRes(notice.iconKey)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.s5),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
            Text(
                text = notice.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = notice.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// -- Terms Text ---------------------------------------------------------------

@Composable
private fun TermsText(
    consent: JenisRekeningConsent,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            append(consent.prefix)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(consent.link)
            }
            append(consent.suffix)
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// -- Previews ------------------------------------------------------------------

@Composable
private fun previewState(
    isLoading: Boolean = false,
    error: String? = null,
    items: List<JenisRekening>? = null,
): BukaRekeningPilihJenisUiState = BukaRekeningPilihJenisUiState(
    isLoading = isLoading,
    error = error,
    heading = stringResource(R.string.buka_rekening_pilih_jenis_heading),
    subtitle = stringResource(R.string.buka_rekening_pilih_subtitle),
    depositLabel = stringResource(R.string.buka_rekening_setoran_awal),
    ctaLabel = stringResource(R.string.buka_rekening_lanjut),
    notice = JenisRekeningNotice(
        iconKey = ICON_KEY_INFO,
        title = stringResource(R.string.buka_rekening_persiapan_dokumen),
        body = stringResource(R.string.buka_rekening_persiapan_dokumen_desc),
    ),
    consent = JenisRekeningConsent(
        prefix = stringResource(R.string.buka_rekening_syarat_prefix),
        link = stringResource(R.string.buka_rekening_syarat_link),
        suffix = stringResource(R.string.buka_rekening_syarat_suffix),
    ),
    jenisRekeningList = items ?: defaultJenisRekeningList(),
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun BukaRekeningPilihJenisScreenPreview() {
    BcaMobileTheme {
        BukaRekeningPilihJenisScreen(
            state = previewState(),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
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
            state = previewState(),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Produk tutup")
@Composable
private fun BukaRekeningPilihJenisScreenUnavailablePreview() {
    BcaMobileTheme {
        val items = defaultJenisRekeningList().mapIndexed { index, jenis ->
            if (index != 2) jenis else jenis.copy(
                isTersedia = false,
                keteranganTidakTersedia =
                    stringResource(R.string.buka_rekening_jenis_tidak_tersedia_perbaikan),
            )
        }
        BukaRekeningPilihJenisScreen(
            state = previewState(items = items).copy(selectedIndex = 2),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Daftar bawaan")
@Composable
private fun BukaRekeningPilihJenisScreenFallbackPreview() {
    BcaMobileTheme {
        BukaRekeningPilihJenisScreen(
            state = previewState().copy(isShowingFallback = true),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
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
            state = previewState(isLoading = true),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
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
            state = previewState(error = "Terjadi kesalahan. Silakan coba lagi."),
            onJenisSelected = {},
            onLanjutClick = {},
            onReloadClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
