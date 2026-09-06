package id.bca.bcamobile.ui.screen.bukti_transaksi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- State Model ---------------------------------------------------------------

data class BuktiTransaksiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tanggal: String = "",
    val noReferensi: String = "",
    val sumberRekening: String = "",
    val namaPengirim: String = "",
    val nomorTujuan: String = "",
    val namaTujuan: String = "",
    val jenisTransaksi: String = "",
    val nominal: String = "",
    val biayaAdmin: String = "",
    val total: String = "",
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BuktiTransaksiScreen(
    state: BuktiTransaksiUiState,
    onBackClick: () -> Unit,
    onBagikanClick: () -> Unit,
    onSimpanClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.navigation_transaction_receipt),
                onBackClick = onBackClick,
            )
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
                    verticalArrangement = Arrangement.spacedBy(Spacing.s6),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.s4),
                ) {
                    SuccessHeader()
                    ReceiptCard(state = state)
                    IllustrationBanner()
                    ActionButtons(
                        onBagikanClick = onBagikanClick,
                        onSimpanClick = onSimpanClick,
                    )
                }
            }
        }
    }
}

// -- Success Header ------------------------------------------------------------

@Composable
private fun SuccessHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s7, bottom = Spacing.s4),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s10)
                .background(AppColor.Success400, AppShape.Full),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_bukti_transaksi_sukses),
                tint = AppColor.Success1000,
                modifier = Modifier.size(Spacing.s7),
            )
        }
        Text(
            text = stringResource(R.string.bukti_transaksi_berhasil),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.bukti_transaksi_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// -- Receipt Card --------------------------------------------------------------

@Composable
private fun ReceiptCard(
    state: BuktiTransaksiUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.R6)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        DateReferenceRow(
            tanggal = state.tanggal,
            noReferensi = state.noReferensi,
        )
        TransferDetailSection(
            sumberRekening = state.sumberRekening,
            namaPengirim = state.namaPengirim,
            nomorTujuan = state.nomorTujuan,
            namaTujuan = state.namaTujuan,
            jenisTransaksi = state.jenisTransaksi,
        )
        AmountBreakdownSection(
            nominal = state.nominal,
            biayaAdmin = state.biayaAdmin,
            total = state.total,
        )
    }
}

// -- Date & Reference Row ------------------------------------------------------

@Composable
private fun DateReferenceRow(
    tanggal: String,
    noReferensi: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.s4),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.bukti_transaksi_tanggal_waktu),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tanggal,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.bukti_transaksi_no_referensi),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = noReferensi,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -- Transfer Detail Section ---------------------------------------------------

@Composable
private fun TransferDetailSection(
    sumberRekening: String,
    namaPengirim: String,
    nomorTujuan: String,
    namaTujuan: String,
    jenisTransaksi: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        modifier = modifier.padding(Spacing.s4),
    ) {
        // Source account
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s8)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        AppShape.Full,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wallet),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bukti_transaksi_dari),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = sumberRekening,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = namaPengirim,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Connector line
        Box(
            modifier = Modifier
                .padding(start = Spacing.s5)
                .width(StrokeWidth.w0)
                .height(Spacing.s6)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        // Destination account
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s8)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        AppShape.Full,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_balance),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bukti_transaksi_ke),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = nomorTujuan,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = namaTujuan,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Service type
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier.padding(top = Spacing.s2),
        ) {
            Text(
                text = stringResource(R.string.bukti_transaksi_jenis_layanan),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = jenisTransaksi,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -- Amount Breakdown Section --------------------------------------------------

@Composable
private fun AmountBreakdownSection(
    nominal: String,
    biayaAdmin: String,
    total: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.s4),
    ) {
        AmountRow(
            label = stringResource(R.string.bukti_transaksi_nominal),
            value = nominal,
        )
        AmountRow(
            label = stringResource(R.string.bukti_transaksi_biaya_admin),
            value = biayaAdmin,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s1),
        ) {
            Text(
                text = stringResource(R.string.bukti_transaksi_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = total,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -- Illustration Banner -------------------------------------------------------

@Composable
private fun IllustrationBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(AppShape.R6)
            .background(AppColor.Neutral500),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColor.Neutral500.copy(alpha = 0f),
                            AppColor.Neutral1000.copy(alpha = AppAlpha.A60),
                        ),
                    ),
                ),
        )
        Text(
            text = stringResource(R.string.bukti_transaksi_simpan_info),
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Neutral100.copy(alpha = AppAlpha.A90),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.s4),
        )
    }
}

// -- Action Buttons ------------------------------------------------------------

@Composable
private fun ActionButtons(
    onBagikanClick: () -> Unit,
    onSimpanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s2),
    ) {
        // Primary: Bagikan
        Button(
            onClick = onBagikanClick,
            shape = AppShape.R4,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.bukti_transaksi_bagikan),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // Secondary: Simpan
        Button(
            onClick = onSimpanClick,
            shape = AppShape.R4,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_receipt),
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.bukti_transaksi_simpan),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// -- Previews ------------------------------------------------------------------

private val previewState = BuktiTransaksiUiState(
    tanggal = "24 Okt 2023, 14:30 WIB",
    noReferensi = "TRX9876543210",
    sumberRekening = "Tabungan Utama (1234 5678)",
    namaPengirim = "Budi Santoso",
    nomorTujuan = "Bank XYZ (8765 4321)",
    namaTujuan = "PT. Maju Mundur",
    jenisTransaksi = "Transfer Online",
    nominal = "Rp 500.000",
    biayaAdmin = "Rp 2.500",
    total = "Rp 502.500",
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun BuktiTransaksiScreenPreview() {
    BcaMobileTheme {
        BuktiTransaksiScreen(
            state = previewState,
            onBackClick = {},
            onBagikanClick = {},
            onSimpanClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun BuktiTransaksiScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        BuktiTransaksiScreen(
            state = previewState,
            onBackClick = {},
            onBagikanClick = {},
            onSimpanClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun BuktiTransaksiScreenLoadingPreview() {
    BcaMobileTheme {
        BuktiTransaksiScreen(
            state = BuktiTransaksiUiState(isLoading = true),
            onBackClick = {},
            onBagikanClick = {},
            onSimpanClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun BuktiTransaksiScreenErrorPreview() {
    BcaMobileTheme {
        BuktiTransaksiScreen(
            state = BuktiTransaksiUiState(
                error = stringResource(R.string.error_general_retry),
            ),
            onBackClick = {},
            onBagikanClick = {},
            onSimpanClick = {},
            onRetry = {},
        )
    }
}