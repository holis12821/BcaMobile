package id.bca.bcamobile.ui.screen.rekening

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── State Models ─────────────────────────────────────────────────────────

data class RekeningItem(
    val accountId: String,
    val label: String,
    /** Nomor rekening dikelompokkan empat digit seperti di desain. */
    val nomor: String,
    val saldoEfektif: String,
    val saldoTersedia: String,
    val danaDitahan: String,
    val isDetailExpanded: Boolean = false,
)

/**
 * Satu kartu debit di bagian "Kartu Debit".
 *
 * Nomornya sudah tersamar dari server; PAN utuh tidak pernah ada di client.
 */
data class KartuDebitItem(
    val cardId: String,
    val productName: String,
    val maskedNumber: String,
    val validThru: String,
    val isBlocked: Boolean,
    val isExpired: Boolean,
)

data class RekeningKartuUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBalanceVisible: Boolean = false,
    val items: List<RekeningItem> = emptyList(),
    /** Kartu debit dari `GET /account/cards`; kosong = nasabah belum punya kartu. */
    val cards: List<KartuDebitItem> = emptyList(),
)

// ── Main Screen ──────────────────────────────────────────────────────────

/**
 * Daftar rekening beserta saldo efektifnya.
 *
 * Bagian "Kartu Debit" dari artefak Stitch kini terisi dari
 * `GET /account/cards`. Aksinya (blokir, atur limit) **tidak** diduplikasi di
 * sini: seluruhnya sudah ada di Profil Saya, dan dua tempat yang sama-sama
 * bisa memblokir kartu adalah dua tempat yang bisa berselisih.
 */
@Composable
fun RekeningKartuScreen(
    state: RekeningKartuUiState,
    onToggleBalance: () -> Unit,
    onDetailToggle: (String) -> Unit,
    onMutasiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rekening_title),
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
                item(key = "heading") {
                    Text(
                        text = stringResource(R.string.rekening_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                items(state.items, key = { it.accountId }) { item ->
                    RekeningCard(
                        item = item,
                        isBalanceVisible = state.isBalanceVisible,
                        onToggleBalance = onToggleBalance,
                        onDetailToggle = { onDetailToggle(item.accountId) },
                        onMutasiClick = { onMutasiClick(item.accountId) },
                    )
                }

                if (state.cards.isNotEmpty()) {
                    item(key = "kartu-heading") {
                        Text(
                            text = stringResource(R.string.rekening_kartu_debit),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    items(state.cards, key = { it.cardId }) { card ->
                        KartuDebitCard(card = card)
                    }
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

/**
 * Kartu debit: nama produk, nomor tersamar, masa berlaku, dan status.
 *
 * Status kedaluwarsa dihitung server di zona WIB; di sini hanya ditampilkan.
 */
@Composable
private fun KartuDebitCard(
    card: KartuDebitItem,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R6)
            .padding(Spacing.s4),
    ) {
        Text(
            text = card.productName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = card.maskedNumber,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.rekening_kartu_valid_thru, card.validThru),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(
                    when {
                        card.isBlocked -> R.string.rekening_kartu_status_diblokir
                        card.isExpired -> R.string.rekening_kartu_status_kedaluwarsa
                        else -> R.string.rekening_kartu_status_aktif
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (card.isBlocked || card.isExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
        }
    }
}

@Composable
private fun RekeningCard(
    item: RekeningItem,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit,
    onDetailToggle: () -> Unit,
    onMutasiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R6)
            .padding(Spacing.s4),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = item.nomor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IconButton(
                onClick = onToggleBalance,
                modifier = Modifier.size(AppSize.MinTouchTarget),
            ) {
                Icon(
                    painter = painterResource(
                        if (isBalanceVisible) {
                            R.drawable.ic_visibility_off
                        } else {
                            R.drawable.ic_visibility
                        },
                    ),
                    contentDescription = stringResource(R.string.cd_toggle_balance),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(AppSize.Icon20),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
            Text(
                text = stringResource(R.string.rekening_saldo_efektif),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                // Saldo hanya dirender saat pengguna membukanya; nilainya tidak
                // pernah ikut tampil di keadaan tersembunyi.
                text = if (isBalanceVisible) {
                    item.saldoEfektif
                } else {
                    stringResource(R.string.rekening_saldo_tersembunyi)
                },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (item.isDetailExpanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow(
                label = stringResource(R.string.rekening_saldo_tersedia),
                value = if (isBalanceVisible) {
                    item.saldoTersedia
                } else {
                    stringResource(R.string.rekening_saldo_tersembunyi)
                },
            )
            DetailRow(
                label = stringResource(R.string.rekening_dana_ditahan),
                value = if (isBalanceVisible) {
                    item.danaDitahan
                } else {
                    stringResource(R.string.rekening_saldo_tersembunyi)
                },
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CardAction(
                iconRes = R.drawable.ic_receipt,
                label = stringResource(R.string.rekening_mutasi),
                onClick = onMutasiClick,
                modifier = Modifier.weight(1f),
            )
            CardAction(
                iconRes = R.drawable.ic_info,
                label = stringResource(R.string.rekening_detail),
                onClick = onDetailToggle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CardAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R4,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.heightIn(min = AppSize.MinTouchTarget),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.s2),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Preview ──────────────────────────────────────────────────────────────

private val previewState = RekeningKartuUiState(
    isBalanceVisible = true,
    items = listOf(
        RekeningItem(
            accountId = "acc_001",
            label = "TAHAPAN BCA",
            nomor = "1234 5678 90",
            saldoEfektif = "Rp12.345.678",
            saldoTersedia = "Rp12.345.678",
            danaDitahan = "Rp0",
        ),
        RekeningItem(
            accountId = "acc_002",
            label = "TAHAPAN GOLD",
            nomor = "9876 5432 10",
            saldoEfektif = "Rp50.000.000",
            saldoTersedia = "Rp49.500.000",
            danaDitahan = "Rp500.000",
            isDetailExpanded = true,
        ),
    ),
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun RekeningKartuScreenPreview() {
    BcaMobileTheme {
        RekeningKartuScreen(
            state = previewState,
            onToggleBalance = {},
            onDetailToggle = {},
            onMutasiClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark + saldo tersembunyi")
@Composable
private fun RekeningKartuScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        RekeningKartuScreen(
            state = previewState.copy(isBalanceVisible = false),
            onToggleBalance = {},
            onDetailToggle = {},
            onMutasiClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Memuat")
@Composable
private fun RekeningKartuScreenLoadingPreview() {
    BcaMobileTheme {
        RekeningKartuScreen(
            state = RekeningKartuUiState(isLoading = true),
            onToggleBalance = {},
            onDetailToggle = {},
            onMutasiClick = {},
            onBackClick = {},
            onRetry = {},
        )
    }
}
