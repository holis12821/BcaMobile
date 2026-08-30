package id.bca.bcamobile.ui.screen.mutasi

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── State Models ─────────────────────────────────────────────────────────

data class TransactionItem(
    val id: String,
    val title: String,
    val description: String,
    val amount: String,
    val time: String,
    val isCredit: Boolean,
    val icon: ImageVector,
)

data class TransactionGroup(
    val date: String,
    val transactions: List<TransactionItem>,
)

enum class MutasiPeriod {
    LAST_7_DAYS, THIS_MONTH, LAST_MONTH, CUSTOM
}

data class MutasiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountLabel: String = "",
    val balance: String = "",
    val selectedPeriod: MutasiPeriod = MutasiPeriod.LAST_7_DAYS,
    val periodInfo: String = "",
    val transactionGroups: List<TransactionGroup> = emptyList(),
    val hasMore: Boolean = false,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiScreen(
    state: MutasiUiState,
    onAccountClick: () -> Unit,
    onPeriodSelected: (MutasiPeriod) -> Unit,
    onCustomDateClick: () -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_riwayat),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.cd_notifikasi),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.s4,
                end = Spacing.s4,
                top = Spacing.s4,
                bottom = Spacing.s4,
            ),
        ) {
            // ── Always visible: filters ──────────────────────────────────
            item(key = "account") {
                AccountSelector(
                    label = state.accountLabel,
                    balance = state.balance,
                    onClick = onAccountClick,
                )
            }

            item(key = "period") {
                PeriodChips(
                    selected = state.selectedPeriod,
                    onSelected = onPeriodSelected,
                    onCustomDate = onCustomDateClick,
                    modifier = Modifier.padding(top = Spacing.s6),
                )
            }

            if (state.periodInfo.isNotEmpty()) {
                item(key = "info") {
                    MutasiInfoBox(
                        text = state.periodInfo,
                        modifier = Modifier.padding(top = Spacing.s6),
                    )
                }
            }

            // ── State-dependent content ──────────────────────────────────
            when {
                state.isLoading -> {
                    item(key = "loading") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.s9),
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                state.error != null -> {
                    item(key = "error") {
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
                }

                state.transactionGroups.isEmpty() -> {
                    item(key = "empty") {
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
                                text = stringResource(R.string.mutasi_empty_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(Spacing.s4))
                            TextButton(onClick = onCustomDateClick) {
                                Text(
                                    text = stringResource(R.string.mutasi_ubah_periode),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                else -> {
                    item(key = "tx_header") {
                        Text(
                            text = stringResource(R.string.daftar_transaksi),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = Spacing.s6),
                        )
                    }

                    state.transactionGroups.forEach { group ->
                        stickyHeader(key = "date_${group.date}") {
                            Text(
                                text = group.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = Spacing.s2),
                            )
                        }

                        items(group.transactions, key = { it.id }) { tx ->
                            TransactionRow(
                                item = tx,
                                onClick = { onTransactionClick(tx) },
                                modifier = Modifier.padding(top = Spacing.s3),
                            )
                        }
                    }

                    if (state.hasMore) {
                        item(key = "load_more") {
                            TextButton(
                                onClick = onLoadMore,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.s4),
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

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun AccountSelector(
    label: String,
    balance: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.rekening_sumber),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(
            onClick = onClick,
            shape = AppShape.R6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s4),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = balance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.cd_pilih_rekening),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PeriodChips(
    selected: MutasiPeriod,
    onSelected: (MutasiPeriod) -> Unit,
    onCustomDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.periode_mutasi),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            PeriodChip(
                text = stringResource(R.string.period_7_hari_terakhir),
                isSelected = selected == MutasiPeriod.LAST_7_DAYS,
                onClick = { onSelected(MutasiPeriod.LAST_7_DAYS) },
            )
            PeriodChip(
                text = stringResource(R.string.period_bulan_ini),
                isSelected = selected == MutasiPeriod.THIS_MONTH,
                onClick = { onSelected(MutasiPeriod.THIS_MONTH) },
            )
            PeriodChip(
                text = stringResource(R.string.period_bulan_lalu),
                isSelected = selected == MutasiPeriod.LAST_MONTH,
                onClick = { onSelected(MutasiPeriod.LAST_MONTH) },
            )
            PeriodChip(
                text = stringResource(R.string.period_pilih_tanggal),
                isSelected = selected == MutasiPeriod.CUSTOM,
                onClick = {
                    onSelected(MutasiPeriod.CUSTOM)
                    onCustomDate()
                },
                leadingIcon = Icons.Default.DateRange,
            )
        }
    }
}

@Composable
private fun PeriodChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = AppShape.Full,
        color = containerColor,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s4),
                    tint = contentColor,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MutasiInfoBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppColor.Primary700.copy(alpha = AppAlpha.A10),
                shape = AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.s6),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TransactionRow(
    item: TransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        shape = AppShape.R6,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.s4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .background(
                            color = if (item.isCredit) {
                                AppColor.Success100
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = AppShape.Full,
                        ),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (item.isCredit) {
                            AppColor.Success700
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(Spacing.s6),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            ) {
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.isCredit) {
                        AppColor.Success700
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

private val previewState = MutasiUiState(
    accountLabel = "Tahapan BCA - 1234567890",
    balance = "Rp 12.500.000",
    selectedPeriod = MutasiPeriod.LAST_7_DAYS,
    periodInfo = "Menampilkan mutasi rekening untuk periode 7 hari terakhir (14 Okt - 21 Okt 2023).",
    transactionGroups = listOf(
        TransactionGroup(
            date = "21 OKTOBER 2023",
            transactions = listOf(
                TransactionItem(
                    id = "1",
                    title = "Tokopedia",
                    description = "Pembayaran E-Commerce",
                    amount = "- Rp 250.000",
                    time = "14:30 WIB",
                    isCredit = false,
                    icon = Icons.Default.ShoppingCart,
                ),
                TransactionItem(
                    id = "2",
                    title = "Transfer ke Budi",
                    description = "BCA - 0987654321",
                    amount = "- Rp 500.000",
                    time = "09:15 WIB",
                    isCredit = false,
                    icon = Icons.AutoMirrored.Filled.Send,
                ),
            ),
        ),
        TransactionGroup(
            date = "20 OKTOBER 2023",
            transactions = listOf(
                TransactionItem(
                    id = "3",
                    title = "Gaji Bulan Oktober",
                    description = "PT Maju Mundur",
                    amount = "+ Rp 8.500.000",
                    time = "08:00 WIB",
                    isCredit = true,
                    icon = Icons.Default.KeyboardArrowDown,
                ),
                TransactionItem(
                    id = "4",
                    title = "PLN Pascabayar",
                    description = "Tagihan Listrik",
                    amount = "- Rp 350.000",
                    time = "19:45 WIB",
                    isCredit = false,
                    icon = Icons.Default.Star,
                ),
            ),
        ),
    ),
    hasMore = true,
)

@Preview(showBackground = true, name = "Success")
@Composable
private fun MutasiScreenPreview() {
    BcaMobileTheme {
        MutasiScreen(
            state = previewState,
            onAccountClick = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onTransactionClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun MutasiScreenEmptyPreview() {
    BcaMobileTheme {
        MutasiScreen(
            state = MutasiUiState(
                accountLabel = "Tahapan BCA - 1234567890",
                balance = "Rp 12.500.000",
            ),
            onAccountClick = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onTransactionClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun MutasiScreenLoadingPreview() {
    BcaMobileTheme {
        MutasiScreen(
            state = MutasiUiState(
                accountLabel = "Tahapan BCA - 1234567890",
                balance = "Rp 12.500.000",
                isLoading = true,
            ),
            onAccountClick = {},
            onPeriodSelected = {},
            onCustomDateClick = {},
            onTransactionClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}