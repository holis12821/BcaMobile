package id.bca.bcamobile.ui.screen.limit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── State Models ─────────────────────────────────────────────────────────

/** Tiga batas yang dilayani `PUT /account/transaction-limit`. */
enum class LimitJenis {
    TRANSFER_INTERNAL, TRANSFER_EXTERNAL, EWALLET
}

data class LimitItem(
    val jenis: LimitJenis,
    /** Batas harian siap tampil, atau kosong bila server belum mengirimkannya. */
    val limitHarian: String,
    val terpakai: String,
    val sisa: String,
    /** 0f..1f untuk bilah pemakaian; null bila batasnya belum diketahui. */
    val progress: Float?,
    val isNearLimit: Boolean,
    /** Nilai yang sedang diketik saat kartu ini dalam mode ubah. */
    val input: String = "",
)

data class AturLimitUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<LimitItem> = emptyList(),
    val editing: LimitJenis? = null,
    /**
     * True saat batas yang berlaku belum diketahui — hanya sebelum
     * `GET /account/transaction-limit` membalas, atau saat panggilan itu gagal.
     */
    val isCurrentLimitUnknown: Boolean = true,
    val isSaveEnabled: Boolean = false,
)

// ── Main Screen ──────────────────────────────────────────────────────────

/**
 * Atur limit transaksi harian.
 *
 * **Catatan kontrak:** kartu ketiga di artefak Stitch berjudul "Debit Online",
 * sedangkan batas ketiga yang dilayani server adalah **e-Wallet**
 * (`ewallet_daily`). Judul mengikuti endpoint supaya yang tampil sama dengan yang
 * benar-benar diubah.
 */
@Composable
fun AturLimitScreen(
    state: AturLimitUiState,
    onEditClick: (LimitJenis) -> Unit,
    onInputChanged: (LimitJenis, String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // Papan ketik: lihat BottomBarInsets.kt — edge-to-edge membuat window tidak
        // menyusut, jadi inset IME diambil lewat safeDrawing di sini.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.limit_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Text(
                    text = stringResource(R.string.limit_heading),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.limit_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            InfoBox(
                title = stringResource(R.string.limit_info_title),
                message = if (state.isCurrentLimitUnknown) {
                    stringResource(R.string.limit_info_belum_tersedia)
                } else {
                    stringResource(R.string.limit_info_pesan)
                },
            )

            state.items.forEach { item ->
                LimitCard(
                    item = item,
                    isEditing = state.editing == item.jenis,
                    onEditClick = { onEditClick(item.jenis) },
                    onInputChanged = { onInputChanged(item.jenis, it) },
                    onCancelEdit = onCancelEdit,
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.editing != null) {
                Button(
                    onClick = onSaveClick,
                    enabled = state.isSaveEnabled && !state.isLoading,
                    shape = AppShape.R6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppSize.MinTouchTarget),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(AppSize.Icon20),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.limit_simpan),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.s7))
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun InfoBox(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = AppAlpha.A20),
                AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppSize.Icon20),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LimitCard(
    item: LimitItem,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onInputChanged: (String) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R7)
            .padding(Spacing.s5),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        painter = painterResource(item.jenis.iconRes()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppSize.Icon20),
                    )
                }
                Text(
                    text = stringResource(item.jenis.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            TextButton(
                onClick = if (isEditing) onCancelEdit else onEditClick,
                modifier = Modifier.heightIn(min = AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.limit_batal else R.string.limit_ubah,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = item.input,
                onValueChange = onInputChanged,
                label = { Text(stringResource(R.string.limit_harian)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0)) {
                    Text(
                        text = stringResource(R.string.limit_harian),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.limitHarian,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Spacing.s0),
                ) {
                    Text(
                        text = stringResource(R.string.limit_terpakai),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.terpakai,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isNearLimit) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            if (item.progress != null) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = if (item.isNearLimit) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.s2),
                )
                Text(
                    text = stringResource(R.string.limit_sisa_format, item.sisa),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Pemetaan enum → tampilan ─────────────────────────────────────────────

private fun LimitJenis.labelRes(): Int = when (this) {
    LimitJenis.TRANSFER_INTERNAL -> R.string.limit_transfer_internal
    LimitJenis.TRANSFER_EXTERNAL -> R.string.limit_transfer_external
    LimitJenis.EWALLET -> R.string.limit_ewallet
}

@DrawableRes
private fun LimitJenis.iconRes(): Int = when (this) {
    LimitJenis.TRANSFER_INTERNAL -> R.drawable.ic_sync
    LimitJenis.TRANSFER_EXTERNAL -> R.drawable.ic_account_balance
    LimitJenis.EWALLET -> R.drawable.ic_wallet
}

// ── Preview ──────────────────────────────────────────────────────────────

private val previewState = AturLimitUiState(
    isCurrentLimitUnknown = false,
    items = listOf(
        LimitItem(
            jenis = LimitJenis.TRANSFER_INTERNAL,
            limitHarian = "Rp250.000.000",
            terpakai = "Rp50.000.000",
            sisa = "Rp200.000.000",
            progress = 0.2f,
            isNearLimit = false,
        ),
        LimitItem(
            jenis = LimitJenis.TRANSFER_EXTERNAL,
            limitHarian = "Rp100.000.000",
            terpakai = "Rp85.000.000",
            sisa = "Rp15.000.000",
            progress = 0.85f,
            isNearLimit = true,
        ),
        LimitItem(
            jenis = LimitJenis.EWALLET,
            limitHarian = "Rp10.000.000",
            terpakai = "Rp0",
            sisa = "Rp10.000.000",
            progress = 0f,
            isNearLimit = false,
        ),
    ),
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun AturLimitScreenPreview() {
    BcaMobileTheme {
        AturLimitScreen(
            state = previewState,
            onEditClick = {},
            onInputChanged = { _, _ -> },
            onCancelEdit = {},
            onSaveClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun AturLimitScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        AturLimitScreen(
            state = previewState,
            onEditClick = {},
            onInputChanged = { _, _ -> },
            onCancelEdit = {},
            onSaveClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Mode ubah")
@Composable
private fun AturLimitScreenEditingPreview() {
    BcaMobileTheme {
        AturLimitScreen(
            state = previewState.copy(
                editing = LimitJenis.TRANSFER_EXTERNAL,
                isSaveEnabled = true,
                items = previewState.items.map {
                    if (it.jenis == LimitJenis.TRANSFER_EXTERNAL) {
                        it.copy(input = "120000000")
                    } else {
                        it
                    }
                },
            ),
            onEditClick = {},
            onInputChanged = { _, _ -> },
            onCancelEdit = {},
            onSaveClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Limit belum diketahui")
@Composable
private fun AturLimitScreenUnknownPreview() {
    BcaMobileTheme {
        AturLimitScreen(
            state = AturLimitUiState(
                items = LimitJenis.entries.map {
                    LimitItem(
                        jenis = it,
                        limitHarian = "-",
                        terpakai = "-",
                        sisa = "-",
                        progress = null,
                        isNearLimit = false,
                    )
                },
            ),
            onEditClick = {},
            onInputChanged = { _, _ -> },
            onCancelEdit = {},
            onSaveClick = {},
            onBackClick = {},
        )
    }
}
