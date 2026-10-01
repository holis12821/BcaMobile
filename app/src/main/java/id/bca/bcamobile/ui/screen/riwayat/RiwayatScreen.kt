package id.bca.bcamobile.ui.screen.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.annotation.DrawableRes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── State Models ─────────────────────────────────────────────────────────

/**
 * Filter jenis transaksi — nilainya mengikuti query `type` di
 * `GET /transactions/history`.
 *
 * [ALL] **tidak dikirim ke server**: handler menyaring `type` apa adanya, jadi
 * `type=ALL` menghasilkan daftar kosong, bukan seluruh riwayat. "Semua"
 * berarti tidak mengirim parameter itu sama sekali.
 */
enum class RiwayatFilter {
    ALL, TRANSFER, EWALLET, PAYMENT, PULSA
}

/**
 * Filter rentang waktu — query `period` di `GET /transactions/history`.
 *
 * Kosakatanya **sama persis** dengan Mutasi: kedua layar melewati
 * `resolvePeriod` yang sama di backend. Desain Stitch memang menggambar chip
 * rentang tanggal di layar ini; dulu tidak dibuat karena endpoint-nya belum
 * punya parameter periode.
 *
 * [SEMUA] berarti tanpa parameter `period`, bukan sebuah nilai.
 */
enum class RiwayatPeriod {
    SEMUA, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, CUSTOM
}

/** Status transaksi dari server; menentukan warna dan ikon lencana. */
enum class RiwayatStatus {
    SUCCESS, PENDING, FAILED
}

enum class RiwayatJenis {
    TRANSFER, EWALLET, QRIS, PULSA, LAINNYA
}

data class RiwayatItem(
    val id: String,
    val title: String,
    /** Tujuan + jam, mis. `0821****5678 • 21:33`. */
    val description: String,
    val amount: String,
    val referenceNumber: String,
    val status: RiwayatStatus,
    val jenis: RiwayatJenis,
)

data class RiwayatGroup(
    val date: String,
    val items: List<RiwayatItem>,
)

data class RiwayatUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedFilter: RiwayatFilter = RiwayatFilter.ALL,
    val selectedPeriod: RiwayatPeriod = RiwayatPeriod.SEMUA,
    /** Rentang kustom yang berlaku, mis. `1 Sep 2026 – 26 Sep 2026`; kosong selain itu. */
    val customRangeLabel: String = "",
    val groups: List<RiwayatGroup> = emptyList(),
    val hasMore: Boolean = false,
)

// ── Main Screen ──────────────────────────────────────────────────────────

/**
 * Riwayat transaksi yang dimulai nasabah: transfer, top-up, pembayaran.
 *
 * Berbeda dari Mutasi — di sini tidak ada transaksi masuk, karena endpoint
 * riwayat hanya memuat transaksi yang di-initiate nasabah. Nominal selalu
 * mengurangi saldo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    state: RiwayatUiState,
    onFilterSelected: (RiwayatFilter) -> Unit,
    onPeriodSelected: (RiwayatPeriod) -> Unit,
    onCustomDateClick: () -> Unit,
    onItemClick: (RiwayatItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.s4,
            end = Spacing.s4,
            top = Spacing.s4,
            bottom = Spacing.s6,
        ),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.riwayat_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        item(key = "filter") {
            RiwayatFilterChips(
                selected = state.selectedFilter,
                onSelected = onFilterSelected,
                modifier = Modifier.padding(top = Spacing.s4),
            )
        }

        item(key = "periode") {
            RiwayatPeriodChips(
                selected = state.selectedPeriod,
                customRangeLabel = state.customRangeLabel,
                onSelected = onPeriodSelected,
                onCustomDate = onCustomDateClick,
                modifier = Modifier.padding(top = Spacing.s2),
            )
        }

        when {
            state.isLoading -> item(key = "loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s9),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.error != null -> item(key = "error") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s9),
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

            state.groups.isEmpty() -> item(key = "empty") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s9),
                ) {
                    Text(
                        text = stringResource(R.string.mutasi_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.s2))
                    Text(
                        text = stringResource(R.string.riwayat_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                state.groups.forEach { group ->
                    stickyHeader(key = "date_${group.date}") {
                        Text(
                            text = group.date,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = Spacing.s2),
                        )
                    }

                    items(group.items, key = { it.id }) { item ->
                        RiwayatRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.padding(top = Spacing.s2),
                        )
                    }
                }

                if (state.hasMore) {
                    item(key = "load_more") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.s5),
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppSize.Icon24),
                                )
                            } else {
                                TextButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.heightIn(min = AppSize.MinTouchTarget),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tampilkan_lebih_banyak),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun RiwayatFilterChips(
    selected: RiwayatFilter,
    onSelected: (RiwayatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        RiwayatFilter.entries.forEach { filter ->
            RiwayatChip(
                text = stringResource(filter.labelRes()),
                isSelected = filter == selected,
                onClick = { onSelected(filter) },
            )
        }
    }
}

/**
 * Baris kedua: rentang waktu. Memakai chip yang sama dengan baris jenis supaya
 * keduanya terbaca sebagai satu kelompok filter, bukan dua kontrol berbeda.
 */
@Composable
private fun RiwayatPeriodChips(
    selected: RiwayatPeriod,
    customRangeLabel: String,
    onSelected: (RiwayatPeriod) -> Unit,
    onCustomDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        RiwayatPeriod.entries.forEach { period ->
            val isCustom = period == RiwayatPeriod.CUSTOM
            RiwayatChip(
                // Rentang yang sedang berlaku menggantikan label "Pilih Tanggal",
                // supaya filter aktif terbaca tanpa membuka layar rentang lagi.
                text = if (isCustom && customRangeLabel.isNotBlank()) {
                    customRangeLabel
                } else {
                    stringResource(period.labelRes())
                },
                isSelected = period == selected,
                onClick = {
                    if (isCustom) onCustomDate() else onSelected(period)
                },
            )
        }
    }
}

@Composable
private fun RiwayatChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = AppShape.Full,
        color = containerColor,
        modifier = modifier.heightIn(min = AppSize.MinTouchTarget),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun RiwayatRow(
    item: RiwayatItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        shape = AppShape.R6,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s8)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = AppShape.Full,
                    ),
            ) {
                Icon(
                    painter = painterResource(item.jenis.iconRes()),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppSize.Icon20),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s0),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.referenceNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.labelLarge,
                    // Riwayat hanya memuat transaksi keluar, jadi nominalnya
                    // selalu mengurangi saldo.
                    color = MaterialTheme.colorScheme.error,
                )
                RiwayatStatusBadge(status = item.status)
            }
        }
    }
}

@Composable
private fun RiwayatStatusBadge(
    status: RiwayatStatus,
    modifier: Modifier = Modifier,
) {
    val contentColor = status.contentColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier
            .background(contentColor.copy(alpha = AppAlpha.A10), AppShape.R1)
            .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
    ) {
        Icon(
            painter = painterResource(status.iconRes()),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(AppSize.Icon16),
        )
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

// ── Pemetaan enum → tampilan ─────────────────────────────────────────────

private fun RiwayatPeriod.labelRes(): Int = when (this) {
    RiwayatPeriod.SEMUA -> R.string.period_semua_waktu
    RiwayatPeriod.LAST_7_DAYS -> R.string.period_7_hari_terakhir
    RiwayatPeriod.LAST_30_DAYS -> R.string.period_30_hari_terakhir
    RiwayatPeriod.THIS_MONTH -> R.string.period_bulan_ini
    RiwayatPeriod.LAST_MONTH -> R.string.period_bulan_lalu
    RiwayatPeriod.CUSTOM -> R.string.period_pilih_tanggal
}

private fun RiwayatFilter.labelRes(): Int = when (this) {
    RiwayatFilter.ALL -> R.string.riwayat_filter_semua
    RiwayatFilter.TRANSFER -> R.string.riwayat_filter_transfer
    RiwayatFilter.EWALLET -> R.string.riwayat_filter_ewallet
    RiwayatFilter.PAYMENT -> R.string.riwayat_filter_pembayaran
    RiwayatFilter.PULSA -> R.string.riwayat_filter_pulsa
}

@DrawableRes
private fun RiwayatJenis.iconRes(): Int = when (this) {
    RiwayatJenis.TRANSFER -> R.drawable.ic_transfer
    RiwayatJenis.EWALLET -> R.drawable.ic_wallet
    RiwayatJenis.QRIS -> R.drawable.ic_qris
    RiwayatJenis.PULSA -> R.drawable.ic_smartphone
    RiwayatJenis.LAINNYA -> R.drawable.ic_payments
}

@DrawableRes
private fun RiwayatStatus.iconRes(): Int = when (this) {
    RiwayatStatus.SUCCESS -> R.drawable.ic_check_circle
    RiwayatStatus.PENDING -> R.drawable.ic_hourglass_top
    RiwayatStatus.FAILED -> R.drawable.ic_cancel
}

private fun RiwayatStatus.labelRes(): Int = when (this) {
    RiwayatStatus.SUCCESS -> R.string.riwayat_status_berhasil
    RiwayatStatus.PENDING -> R.string.riwayat_status_diproses
    RiwayatStatus.FAILED -> R.string.riwayat_status_gagal
}

@Composable
private fun RiwayatStatus.contentColor(): Color = when (this) {
    RiwayatStatus.SUCCESS -> AppColor.Success700
    RiwayatStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    RiwayatStatus.FAILED -> MaterialTheme.colorScheme.error
}

// ── Preview ──────────────────────────────────────────────────────────────

private val previewState = RiwayatUiState(
    groups = listOf(
        RiwayatGroup(
            date = "28 Jul 2026",
            items = listOf(
                RiwayatItem(
                    id = "txn_001",
                    title = "TRF E-BANKING",
                    description = "0821****5678 • 21:33",
                    amount = "- Rp50.000",
                    referenceNumber = "REF2026072800001",
                    status = RiwayatStatus.SUCCESS,
                    jenis = RiwayatJenis.TRANSFER,
                ),
                RiwayatItem(
                    id = "txn_002",
                    title = "TOP UP GOPAY",
                    description = "0812****8889 • 18:45",
                    amount = "- Rp150.000",
                    referenceNumber = "REF2026072800002",
                    status = RiwayatStatus.FAILED,
                    jenis = RiwayatJenis.EWALLET,
                ),
            ),
        ),
        RiwayatGroup(
            date = "26 Jul 2026",
            items = listOf(
                RiwayatItem(
                    id = "txn_003",
                    title = "PEMBAYARAN QRIS",
                    description = "KOPI KENANGAN • 09:15",
                    amount = "- Rp125.000",
                    referenceNumber = "REF2026072600003",
                    status = RiwayatStatus.PENDING,
                    jenis = RiwayatJenis.QRIS,
                ),
            ),
        ),
    ),
    hasMore = true,
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun RiwayatScreenPreview() {
    BcaMobileTheme {
        RiwayatScreen(
            state = previewState,
            onFilterSelected = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onItemClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun RiwayatScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        RiwayatScreen(
            state = previewState,
            onFilterSelected = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onItemClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Kosong")
@Composable
private fun RiwayatScreenEmptyPreview() {
    BcaMobileTheme {
        RiwayatScreen(
            state = RiwayatUiState(selectedFilter = RiwayatFilter.EWALLET),
            onFilterSelected = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onItemClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Memuat")
@Composable
private fun RiwayatScreenLoadingPreview() {
    BcaMobileTheme {
        RiwayatScreen(
            state = RiwayatUiState(isLoading = true),
            onFilterSelected = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onItemClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}
