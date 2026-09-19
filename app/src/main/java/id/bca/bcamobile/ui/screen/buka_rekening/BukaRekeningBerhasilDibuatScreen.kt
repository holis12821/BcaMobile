package id.bca.bcamobile.ui.screen.buka_rekening

import android.content.res.Configuration
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// -- Data Model ---------------------------------------------------------------

data class BerhasilDibuatUiState(
    val jenisRekening: String = "",
    val nomorRekening: String = "",
    val namaPemilik: String = "",
    val kantorCabang: String = "",
    val minimumSetoran: String = "",
)

// -- Main Screen --------------------------------------------------------------

@Composable
fun BukaRekeningBerhasilDibuatScreen(
    state: BerhasilDibuatUiState,
    onMasukMbcaClick: () -> Unit,
    onBagikanClick: () -> Unit,
    onSalinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s4),
    ) {
        // Minimal header
        BerhasilHeader()

        Spacer(Modifier.height(Spacing.s2))

        // Hero success
        HeroSuccessSection()

        Spacer(Modifier.height(Spacing.s6))

        // Account details card
        AccountDetailsCard(
            state = state,
            onSalinClick = onSalinClick,
        )

        Spacer(Modifier.height(Spacing.s4))

        // Deposit notice
        SetoranAwalSection(minimumSetoran = state.minimumSetoran)

        Spacer(Modifier.height(Spacing.s6))

        // Action buttons
        ActionButtonsSection(
            onMasukMbcaClick = onMasukMbcaClick,
            onBagikanClick = onBagikanClick,
        )

        Spacer(Modifier.height(Spacing.s7))
    }
}

// -- Header -------------------------------------------------------------------

@Composable
private fun BerhasilHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s3),
    ) {
        // Empty spacer for balance
        Box(modifier = Modifier.size(Spacing.s7))

        Text(
            text = stringResource(R.string.buka_rekening_berhasil_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = { /* TODO: help */ }) {
            Icon(
                painter = painterResource(R.drawable.ic_help_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Spacing.s6),
            )
        }
    }
}

// -- Hero Success -------------------------------------------------------------

@Composable
private fun HeroSuccessSection(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s2),
    ) {
        // Green circle with check
        Box(contentAlignment = Alignment.Center) {
            // Outer pulse ring
            Box(
                modifier = Modifier
                    .size(Spacing.s10)
                    .background(
                        AppColor.Success100,
                        AppShape.Full,
                    ),
            )
            // Inner solid circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s9)
                    .background(AppColor.Success700, AppShape.Full),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = AppColor.Neutral100,
                    modifier = Modifier.size(AppSize.IconLarge),
                )
            }
        }

        Spacer(Modifier.height(Spacing.s4))

        Text(
            text = stringResource(R.string.buka_rekening_berhasil_heading),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.s2))

        Text(
            text = stringResource(R.string.buka_rekening_berhasil_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// -- Account Details Card -----------------------------------------------------

@Composable
private fun AccountDetailsCard(
    state: BerhasilDibuatUiState,
    onSalinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s5),
        ) {
            // Jenis Rekening + Aktif badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Spacing.s7)
                            .background(AppColor.Primary100, AppShape.R4),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_credit_card),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Spacing.s5),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.buka_rekening_berhasil_jenis_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.jenisRekening,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Aktif badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AppColor.Success100, AppShape.Full)
                        .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
                ) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.s1)
                            .background(AppColor.Success700, AppShape.Full),
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_berhasil_aktif),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = AppColor.Success700,
                    )
                }
            }

            // Nomor Rekening section
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        AppShape.R4,
                    )
                    .padding(Spacing.s3),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_berhasil_norek_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onSalinClick(state.nomorRekening) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_content_copy),
                            contentDescription = stringResource(R.string.cd_buka_rekening_berhasil_salin),
                            modifier = Modifier.size(Spacing.s4),
                        )
                        Spacer(Modifier.size(Spacing.s1))
                        Text(
                            text = stringResource(R.string.buka_rekening_berhasil_salin),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Text(
                    text = state.nomorRekening,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Detail rows
            DetailRow(
                label = stringResource(R.string.buka_rekening_berhasil_nama_label),
                value = state.namaPemilik,
            )

            DetailRow(
                label = stringResource(R.string.buka_rekening_berhasil_cabang_label),
                value = state.kantorCabang,
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
        verticalAlignment = Alignment.Top,
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
            textAlign = TextAlign.End,
        )
    }
}

// -- Setoran Awal Section -----------------------------------------------------

@Composable
private fun SetoranAwalSection(
    minimumSetoran: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s4),
    ) {
        // Info header
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s7)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        AppShape.Full,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.buka_rekening_berhasil_setoran_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_berhasil_setoran_desc, minimumSetoran),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.s2))

        // Pilihan Cara Setor
        Text(
            text = stringResource(R.string.buka_rekening_berhasil_cara_setor).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CaraSetorItem(
            iconRes = R.drawable.ic_transfer,
            title = stringResource(R.string.buka_rekening_berhasil_transfer_title),
            description = stringResource(R.string.buka_rekening_berhasil_transfer_desc),
        )

        CaraSetorItem(
            iconRes = R.drawable.ic_account_balance,
            title = stringResource(R.string.buka_rekening_berhasil_atm_title),
            description = stringResource(R.string.buka_rekening_berhasil_atm_desc),
        )

        CaraSetorItem(
            iconRes = R.drawable.ic_apartment,
            title = stringResource(R.string.buka_rekening_berhasil_cabang_title),
            description = stringResource(R.string.buka_rekening_berhasil_cabang_desc),
        )
    }
}

@Composable
private fun CaraSetorItem(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                AppShape.R4,
            )
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    AppShape.R4,
                ),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s5),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Action Buttons -----------------------------------------------------------

@Composable
private fun ActionButtonsSection(
    onMasukMbcaClick: () -> Unit,
    onBagikanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onMasukMbcaClick,
            shape = AppShape.R6,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.MinTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_berhasil_masuk_mbca),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.size(Spacing.s2))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
        }

        OutlinedButton(
            onClick = onBagikanClick,
            shape = AppShape.R6,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.MinTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_berhasil_bagikan),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

private fun previewState() = BerhasilDibuatUiState(
    jenisRekening = "Tahapan BCA",
    nomorRekening = "5420 8912 34",
    namaPemilik = "MUHAMMAD ARDAN PRAYOGI",
    kantorCabang = "KCU Jakarta Thamrin",
    minimumSetoran = "Rp 500.000",
)

@Preview(showBackground = true)
@Composable
private fun BerhasilDibuatPreview() {
    BcaMobileTheme {
        BukaRekeningBerhasilDibuatScreen(
            state = previewState(),
            onMasukMbcaClick = {},
            onBagikanClick = {},
            onSalinClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BerhasilDibuatDarkPreview() {
    BcaMobileTheme {
        BukaRekeningBerhasilDibuatScreen(
            state = previewState(),
            onMasukMbcaClick = {},
            onBagikanClick = {},
            onSalinClick = {},
        )
    }
}