package id.bca.bcamobile.ui.screen.bantuan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Models ─────────────────────────────────────────────────────────

data class BantuanItem(
    val question: String,
    val answer: String,
)

/**
 * [key] dipetakan ke ikon oleh client. [title] datang dari server supaya
 * kategori baru yang belum dikenal build ini tetap punya teks untuk dicetak.
 */
data class BantuanKategori(
    val key: String,
    val title: String,
    val items: List<BantuanItem>,
)

data class PusatBantuanUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<BantuanKategori> = emptyList(),
    /** Pertanyaan yang sedang terbuka; hanya satu pada satu waktu. */
    val expandedQuestion: String? = null,
)

// ── Main Screen ──────────────────────────────────────────────────────────

/**
 * Pusat Bantuan (FAQ).
 *
 * **Tidak ada di artefak Stitch** — `docs/design/missing-screens-audit.md` §E5
 * hanya mendaftar elemen yang dibutuhkan. Susunannya dirakit dari komponen dan
 * token yang sudah ada, mengikuti preseden layar Konfirmasi QRIS.
 * **Perlu direview pemilik desain.**
 *
 * Kotak pencarian yang disebut audit **tidak** dibuat: tidak ada endpoint
 * pencarian, dan menyaring di client atas isi yang sudah termuat akan terlihat
 * seperti pencarian yang tidak menemukan apa-apa.
 */
@Composable
fun PusatBantuanScreen(
    state: PusatBantuanUiState,
    onItemToggle: (String) -> Unit,
    onHubungiCsClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.bantuan_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.s6),
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

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                contentPadding = PaddingValues(Spacing.s4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (state.categories.isEmpty()) {
                    item(key = "kosong") {
                        Text(
                            text = stringResource(R.string.bantuan_kosong),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.categories.forEach { category ->
                    item(key = "judul-${category.key}") {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    items(
                        count = category.items.size,
                        key = { index -> "${category.key}-$index" },
                    ) { index ->
                        val item = category.items[index]
                        BantuanItemCard(
                            item = item,
                            isExpanded = state.expandedQuestion == item.question,
                            onClick = { onItemToggle(item.question) },
                        )
                    }
                }

                item(key = "hubungi-cs") {
                    TextButton(
                        onClick = onHubungiCsClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.bantuan_hubungi_cs),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun BantuanItemCard(
    item: BantuanItem,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = StrokeWidth.w0,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.A30),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(Spacing.s4),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppSize.MinTouchTarget),
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.s2),
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PusatBantuanScreenPreview() {
    BcaMobileTheme {
        PusatBantuanScreen(
            state = PusatBantuanUiState(
                categories = listOf(
                    BantuanKategori(
                        key = "TRANSFER",
                        title = "Transfer & Pembayaran",
                        items = listOf(
                            BantuanItem("Kenapa transfer saya gagal?", "Periksa saldo dan limit."),
                        ),
                    ),
                ),
                expandedQuestion = "Kenapa transfer saya gagal?",
            ),
            onItemToggle = {},
            onHubungiCsClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
