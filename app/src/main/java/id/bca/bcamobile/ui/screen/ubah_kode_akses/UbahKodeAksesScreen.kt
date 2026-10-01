package id.bca.bcamobile.ui.screen.ubah_kode_akses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── State Models ─────────────────────────────────────────────────────────

/**
 * Tiga langkah yang benar-benar dilayani `POST /auth/pin/change`.
 *
 * Artefak Stitch menggambarkan lima langkah — dua di antaranya verifikasi kartu
 * ATM dan OTP, yang belum punya endpoint di `01-API-SPECIFICATION.md`. Dua langkah
 * itu tidak dibuat, bukan dibuat setengah jalan: di fitur keamanan, layar yang
 * terlihat memverifikasi tapi tidak memanggil apa pun lebih berbahaya daripada
 * tidak ada.
 */
enum class UbahKodeLangkah {
    KODE_LAMA, KODE_BARU, BERHASIL
}

data class UbahKodeAksesUiState(
    val langkah: UbahKodeLangkah = UbahKodeLangkah.KODE_LAMA,
    val isLoading: Boolean = false,
    val error: String? = null,
    val kodeLama: String = "",
    val kodeBaru: String = "",
    val konfirmasiKodeBaru: String = "",
    val isKodeLamaVisible: Boolean = false,
    val isKodeBaruVisible: Boolean = false,
    val isKonfirmasiVisible: Boolean = false,
    /** Kriteria keamanan, ditampilkan sebagai daftar centang seperti di desain. */
    val isPanjangValid: Boolean = false,
    val isTidakBerurutan: Boolean = false,
    val isTidakBerulang: Boolean = false,
    val isKonfirmasiCocok: Boolean = false,
    val isLanjutAktif: Boolean = false,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun UbahKodeAksesScreen(
    state: UbahKodeAksesUiState,
    onKodeLamaChanged: (String) -> Unit,
    onKodeBaruChanged: (String) -> Unit,
    onKonfirmasiChanged: (String) -> Unit,
    onToggleKodeLamaVisibility: () -> Unit,
    onToggleKodeBaruVisibility: () -> Unit,
    onToggleKonfirmasiVisibility: () -> Unit,
    onLanjutClick: () -> Unit,
    onSelesaiClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.ubah_kode_title),
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
            Stepper(langkah = state.langkah)

            when (state.langkah) {
                UbahKodeLangkah.KODE_LAMA -> KodeLamaStep(
                    state = state,
                    onKodeLamaChanged = onKodeLamaChanged,
                    onToggleVisibility = onToggleKodeLamaVisibility,
                )

                UbahKodeLangkah.KODE_BARU -> KodeBaruStep(
                    state = state,
                    onKodeBaruChanged = onKodeBaruChanged,
                    onKonfirmasiChanged = onKonfirmasiChanged,
                    onToggleKodeBaruVisibility = onToggleKodeBaruVisibility,
                    onToggleKonfirmasiVisibility = onToggleKonfirmasiVisibility,
                )

                UbahKodeLangkah.BERHASIL -> BerhasilStep()
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = if (state.langkah == UbahKodeLangkah.BERHASIL) {
                    onSelesaiClick
                } else {
                    onLanjutClick
                },
                enabled = state.isLanjutAktif && !state.isLoading,
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
                        text = stringResource(
                            when (state.langkah) {
                                UbahKodeLangkah.KODE_LAMA -> R.string.ubah_kode_lanjut
                                UbahKodeLangkah.KODE_BARU -> R.string.ubah_kode_simpan
                                UbahKodeLangkah.BERHASIL -> R.string.ubah_kode_selesai
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

// ── Steps ────────────────────────────────────────────────────────────────

@Composable
private fun Stepper(
    langkah: UbahKodeLangkah,
    modifier: Modifier = Modifier,
) {
    val index = UbahKodeLangkah.entries.indexOf(langkah) + 1
    val total = UbahKodeLangkah.entries.size

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.ubah_kode_langkah_format, index, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(langkah.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { index.toFloat() / total },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.s0),
        )
    }
}

@Composable
private fun KodeLamaStep(
    state: UbahKodeAksesUiState,
    onKodeLamaChanged: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.fillMaxWidth(),
    ) {
        StepHeading(
            iconRes = R.drawable.ic_lock,
            title = stringResource(R.string.ubah_kode_lama_title),
            description = stringResource(R.string.ubah_kode_lama_desc),
        )
        KodeField(
            value = state.kodeLama,
            label = stringResource(R.string.ubah_kode_lama_label),
            isVisible = state.isKodeLamaVisible,
            onValueChange = onKodeLamaChanged,
            onToggleVisibility = onToggleVisibility,
        )
    }
}

@Composable
private fun KodeBaruStep(
    state: UbahKodeAksesUiState,
    onKodeBaruChanged: (String) -> Unit,
    onKonfirmasiChanged: (String) -> Unit,
    onToggleKodeBaruVisibility: () -> Unit,
    onToggleKonfirmasiVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.fillMaxWidth(),
    ) {
        StepHeading(
            iconRes = R.drawable.ic_shield,
            title = stringResource(R.string.ubah_kode_baru_title),
            description = stringResource(R.string.ubah_kode_baru_desc),
        )
        KodeField(
            value = state.kodeBaru,
            label = stringResource(R.string.ubah_kode_baru_label),
            isVisible = state.isKodeBaruVisible,
            onValueChange = onKodeBaruChanged,
            onToggleVisibility = onToggleKodeBaruVisibility,
        )
        KodeField(
            value = state.konfirmasiKodeBaru,
            label = stringResource(R.string.ubah_kode_konfirmasi_label),
            isVisible = state.isKonfirmasiVisible,
            onValueChange = onKonfirmasiChanged,
            onToggleVisibility = onToggleKonfirmasiVisibility,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.ubah_kode_kriteria),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            KriteriaRow(
                isValid = state.isPanjangValid,
                text = stringResource(R.string.ubah_kode_kriteria_panjang),
            )
            KriteriaRow(
                isValid = state.isTidakBerurutan,
                text = stringResource(R.string.ubah_kode_kriteria_berurutan),
            )
            KriteriaRow(
                isValid = state.isTidakBerulang,
                text = stringResource(R.string.ubah_kode_kriteria_berulang),
            )
            KriteriaRow(
                isValid = state.isKonfirmasiCocok,
                text = stringResource(R.string.ubah_kode_kriteria_cocok),
            )
        }
    }
}

@Composable
private fun BerhasilStep(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s7),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.LogoContainer)
                .padding(Spacing.s2),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = AppColor.Success700,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(Spacing.s5))
        Text(
            text = stringResource(R.string.ubah_kode_berhasil_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.ubah_kode_berhasil_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Bagian kecil ─────────────────────────────────────────────────────────

@Composable
private fun StepHeading(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s10)
                .padding(Spacing.s2),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s1))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KodeField(
    value: String,
    label: String,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        // Kode akses m-BCA boleh huruf dan angka, jadi bukan papan angka.
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    painter = painterResource(
                        if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                    ),
                    contentDescription = stringResource(R.string.ubah_kode_lihat),
                    modifier = Modifier.size(AppSize.Icon20),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun KriteriaRow(
    isValid: Boolean,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(
                if (isValid) R.drawable.ic_check_circle else R.drawable.ic_cancel,
            ),
            contentDescription = null,
            tint = if (isValid) AppColor.Success700 else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(AppSize.IconSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun UbahKodeLangkah.labelRes(): Int = when (this) {
    UbahKodeLangkah.KODE_LAMA -> R.string.ubah_kode_step_lama
    UbahKodeLangkah.KODE_BARU -> R.string.ubah_kode_step_baru
    UbahKodeLangkah.BERHASIL -> R.string.ubah_kode_step_berhasil
}

// ── Preview ──────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Langkah 1")
@Composable
private fun UbahKodeAksesLamaPreview() {
    BcaMobileTheme {
        UbahKodeAksesScreen(
            state = UbahKodeAksesUiState(kodeLama = "Bca202", isLanjutAktif = true),
            onKodeLamaChanged = {},
            onKodeBaruChanged = {},
            onKonfirmasiChanged = {},
            onToggleKodeLamaVisibility = {},
            onToggleKodeBaruVisibility = {},
            onToggleKonfirmasiVisibility = {},
            onLanjutClick = {},
            onSelesaiClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Langkah 2 (dark)")
@Composable
private fun UbahKodeAksesBaruPreview() {
    BcaMobileTheme(darkTheme = true) {
        UbahKodeAksesScreen(
            state = UbahKodeAksesUiState(
                langkah = UbahKodeLangkah.KODE_BARU,
                kodeBaru = "Bca901",
                konfirmasiKodeBaru = "Bca901",
                isPanjangValid = true,
                isTidakBerurutan = true,
                isTidakBerulang = true,
                isKonfirmasiCocok = true,
                isLanjutAktif = true,
            ),
            onKodeLamaChanged = {},
            onKodeBaruChanged = {},
            onKonfirmasiChanged = {},
            onToggleKodeLamaVisibility = {},
            onToggleKodeBaruVisibility = {},
            onToggleKonfirmasiVisibility = {},
            onLanjutClick = {},
            onSelesaiClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Langkah 3")
@Composable
private fun UbahKodeAksesBerhasilPreview() {
    BcaMobileTheme {
        UbahKodeAksesScreen(
            state = UbahKodeAksesUiState(
                langkah = UbahKodeLangkah.BERHASIL,
                isLanjutAktif = true,
            ),
            onKodeLamaChanged = {},
            onKodeBaruChanged = {},
            onKonfirmasiChanged = {},
            onToggleKodeLamaVisibility = {},
            onToggleKodeBaruVisibility = {},
            onToggleKonfirmasiVisibility = {},
            onLanjutClick = {},
            onSelesaiClick = {},
            onBackClick = {},
        )
    }
}
