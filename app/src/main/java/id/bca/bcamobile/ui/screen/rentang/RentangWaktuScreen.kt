package id.bca.bcamobile.ui.screen.rentang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

/**
 * Pilihan rentang waktu mutasi.
 *
 * Nilainya harus dilayani `GET /transactions/mutations`: `period` plus
 * `start_date`/`end_date` untuk rentang bebas. Dropdown "Jenis Transaksi" di
 * desain tidak dibuat — endpoint mutasi tidak punya parameter jenis.
 */
enum class RentangPilihan {
    HARI_INI, TUJUH_HARI, BULAN_INI, BULAN_LALU, PILIH_TANGGAL
}

data class RentangWaktuUiState(
    val pilihan: RentangPilihan = RentangPilihan.TUJUH_HARI,
    /** `yyyy-MM-dd`, hanya dipakai saat [RentangPilihan.PILIH_TANGGAL]. */
    val tanggalMulai: String = "",
    val tanggalAkhir: String = "",
) {
    val isTerapkanAktif: Boolean
        get() = pilihan != RentangPilihan.PILIH_TANGGAL ||
            (tanggalMulai.isNotBlank() && tanggalAkhir.isNotBlank())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentangWaktuScreen(
    state: RentangWaktuUiState,
    onPilihanSelected: (RentangPilihan) -> Unit,
    onTanggalMulaiSelected: (Long) -> Unit,
    onTanggalAkhirSelected: (Long) -> Unit,
    onTerapkanClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rentang_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            // Tombol menempel di bawah seperti di desain, bukan ikut menggulir.
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = onTerapkanClick,
                    enabled = state.isTerapkanAktif,
                    shape = AppShape.R6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.s4)
                        .heightIn(min = AppSize.MinTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.rentang_terapkan),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        ) {
            RentangPilihan.entries.forEach { pilihan ->
                PilihanRow(
                    label = stringResource(pilihan.labelRes()),
                    isSelected = pilihan == state.pilihan,
                    onClick = { onPilihanSelected(pilihan) },
                )
            }

            if (state.pilihan == RentangPilihan.PILIH_TANGGAL) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.s2),
                ) {
                    TanggalButton(
                        label = state.tanggalMulai.ifBlank {
                            stringResource(R.string.rentang_tanggal_mulai)
                        },
                        onClick = { pickerTarget = PickerTarget.MULAI },
                        modifier = Modifier.weight(1f),
                    )
                    TanggalButton(
                        label = state.tanggalAkhir.ifBlank {
                            stringResource(R.string.rentang_tanggal_akhir)
                        },
                        onClick = { pickerTarget = PickerTarget.AKHIR },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    val target = pickerTarget
    if (target != null) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            when (target) {
                                PickerTarget.MULAI -> onTanggalMulaiSelected(millis)
                                PickerTarget.AKHIR -> onTanggalAkhirSelected(millis)
                            }
                        }
                        pickerTarget = null
                    },
                ) {
                    Text(stringResource(R.string.rentang_pilih))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text(stringResource(R.string.limit_batal))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class PickerTarget { MULAI, AKHIR }

@Composable
private fun PilihanRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R4,
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppSize.MinTouchTarget),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.padding(horizontal = Spacing.s3, vertical = Spacing.s2),
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TanggalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = AppShape.R4,
        modifier = modifier.heightIn(min = AppSize.MinTouchTarget),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun RentangPilihan.labelRes(): Int = when (this) {
    RentangPilihan.HARI_INI -> R.string.rentang_hari_ini
    RentangPilihan.TUJUH_HARI -> R.string.rentang_tujuh_hari
    RentangPilihan.BULAN_INI -> R.string.rentang_bulan_ini
    RentangPilihan.BULAN_LALU -> R.string.rentang_bulan_lalu
    RentangPilihan.PILIH_TANGGAL -> R.string.rentang_pilih_tanggal
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun RentangWaktuScreenPreview() {
    BcaMobileTheme {
        RentangWaktuScreen(
            state = RentangWaktuUiState(),
            onPilihanSelected = {},
            onTanggalMulaiSelected = {},
            onTanggalAkhirSelected = {},
            onTerapkanClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark + rentang bebas")
@Composable
private fun RentangWaktuScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        RentangWaktuScreen(
            state = RentangWaktuUiState(
                pilihan = RentangPilihan.PILIH_TANGGAL,
                tanggalMulai = "2026-09-01",
                tanggalAkhir = "2026-09-24",
            ),
            onPilihanSelected = {},
            onTanggalMulaiSelected = {},
            onTanggalAkhirSelected = {},
            onTerapkanClick = {},
            onBackClick = {},
        )
    }
}
