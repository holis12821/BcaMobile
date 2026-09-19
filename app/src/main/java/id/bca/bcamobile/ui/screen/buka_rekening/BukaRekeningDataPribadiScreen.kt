package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------

enum class JenisKelamin { LAKI_LAKI, PEREMPUAN }

data class BukaRekeningDataPribadiUiState(
    val nik: String = "3174082104950001",
    val namaLengkap: String = "MUHAMMAD ARDAN PRAYOGI",
    val tempatLahir: String = "Jakarta",
    val tanggalLahir: String = "21 April 1995",
    val jenisKelamin: JenisKelamin = JenisKelamin.LAKI_LAKI,
    val alamatLengkap: String = "Jl. Sudirman Kav. 45 No. 12B",
    val rtRw: String = "004 / 002",
    val kodePos: String = "12190",
    val kelurahanKecamatan: String = "Senayan, Kebayoran Baru",
    val kotaProvinsi: String = "Jakarta Selatan, DKI Jakarta",
    val alamatDomisiliSama: Boolean = true,
    val jenisPekerjaan: String = "Karyawan Swasta",
    val penghasilanPerBulan: String = "Rp 10.000.000 - Rp 20.000.000",
    val sumberDanaUtama: String = "Gaji",
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningDataPribadiScreen(
    state: BukaRekeningDataPribadiUiState,
    onJenisKelaminSelect: (JenisKelamin) -> Unit,
    onAlamatDomisiliToggle: (Boolean) -> Unit,
    onLanjutClick: () -> Unit,
    onSimpanClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_dp_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomActions(
                onLanjutClick = onLanjutClick,
                onSimpanClick = onSimpanClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Spacer(Modifier.height(Spacing.s0))

            // Step progress
            StepProgressIndicator(
                currentStep = 4,
                totalSteps = 8,
                stepLabel = stringResource(R.string.buka_rekening_dp_step_label),
            )

            // Intro banner
            IntroBanner()

            // Section 1: Identitas
            IdentitasSection(
                state = state,
                onJenisKelaminSelect = onJenisKelaminSelect,
            )

            // Section 2: Alamat
            AlamatSection(
                state = state,
                onAlamatDomisiliToggle = onAlamatDomisiliToggle,
            )

            // Section 3: Pekerjaan
            PekerjaanSection(state = state)

            // Security banner
            SecurityBanner()

            Spacer(Modifier.height(Spacing.s2))
        }
    }
}

// -- Intro Banner -------------------------------------------------------------

@Composable
private fun IntroBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s4)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .background(AppColor.Primary100, AppShape.R4),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_verified_user),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s6),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.buka_rekening_dp_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.s1))
                    Text(
                        text = stringResource(R.string.buka_rekening_dp_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.s4))

            // OCR verified pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        AppShape.R4,
                    )
                    .padding(Spacing.s3),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s6)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_document_scanner),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(Spacing.s4),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.buka_rekening_dp_ocr_verified),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_dp_ocr_verified_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
        }
    }
}

// -- Section Header -----------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingIcon: Int? = null,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = Spacing.s0, height = Spacing.s4)
                    .background(MaterialTheme.colorScheme.primary, AppShape.Full),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Spacing.s5),
            )
        }
    }
}

// -- Form Field Components ----------------------------------------------------

@Composable
private fun ReadOnlyField(
    label: String,
    value: String,
    leadingIcon: Int? = null,
    trailingIcon: Int? = null,
    trailingBadge: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trailingBadge != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AppColor.Primary100, AppShape.Full)
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_verified),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s3),
                    )
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    AppShape.R6,
                )
                .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (trailingIcon != null) {
                Icon(
                    painter = painterResource(trailingIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
        }
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: Int? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = AppShape.R6,
            shadowElevation = Spacing.s0,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.s3, vertical = Spacing.s3),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (trailingIcon != null) {
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    leadingIcon: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = AppShape.R6,
            shadowElevation = Spacing.s0,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.s3, vertical = Spacing.s3),
            ) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(Spacing.s6),
                )
            }
        }
    }
}

// -- Section 1: Identitas Kependudukan ----------------------------------------

@Composable
private fun IdentitasSection(
    state: BukaRekeningDataPribadiUiState,
    onJenisKelaminSelect: (JenisKelamin) -> Unit,
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
            modifier = Modifier.padding(Spacing.s4),
        ) {
            SectionHeader(title = stringResource(R.string.buka_rekening_dp_section_identitas))

            // NIK
            ReadOnlyField(
                label = stringResource(R.string.buka_rekening_dp_nik_label),
                value = state.nik,
                leadingIcon = R.drawable.ic_credit_card,
                trailingIcon = R.drawable.ic_check_circle,
                trailingBadge = stringResource(R.string.buka_rekening_dp_nik_verified),
            )

            // Nama
            ReadOnlyField(
                label = stringResource(R.string.buka_rekening_dp_nama_label),
                value = state.namaLengkap,
                leadingIcon = R.drawable.ic_person,
                trailingIcon = R.drawable.ic_lock,
            )

            // Tempat & Tanggal Lahir (2-column)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_tempat_lahir),
                    value = state.tempatLahir,
                    modifier = Modifier.weight(1f),
                )
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_tanggal_lahir),
                    value = state.tanggalLahir,
                    trailingIcon = R.drawable.ic_calendar_today,
                    modifier = Modifier.weight(1f),
                )
            }

            // Jenis Kelamin
            GenderSelector(
                selected = state.jenisKelamin,
                onSelect = onJenisKelaminSelect,
            )
        }
    }
}

@Composable
private fun GenderSelector(
    selected: JenisKelamin,
    onSelect: (JenisKelamin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_dp_jenis_kelamin),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            GenderOption(
                label = stringResource(R.string.buka_rekening_dp_laki_laki),
                iconRes = R.drawable.ic_male,
                isSelected = selected == JenisKelamin.LAKI_LAKI,
                onClick = { onSelect(JenisKelamin.LAKI_LAKI) },
                modifier = Modifier.weight(1f),
            )
            GenderOption(
                label = stringResource(R.string.buka_rekening_dp_perempuan),
                iconRes = R.drawable.ic_female,
                isSelected = selected == JenisKelamin.PEREMPUAN,
                onClick = { onSelect(JenisKelamin.PEREMPUAN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GenderOption(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        AppColor.Primary100
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(AppShape.R6)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(Spacing.s3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isSelected) contentColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Spacing.s5),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = fontWeight),
                color = contentColor,
            )
        }
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s5)
                    .background(MaterialTheme.colorScheme.primary, AppShape.Full),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Spacing.s3),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(Spacing.s5)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        AppShape.Full,
                    ),
            )
        }
    }
}

// -- Section 2: Alamat --------------------------------------------------------

@Composable
private fun AlamatSection(
    state: BukaRekeningDataPribadiUiState,
    onAlamatDomisiliToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            SectionHeader(
                title = stringResource(R.string.buka_rekening_dp_section_alamat),
                trailingIcon = R.drawable.ic_home_pin,
            )

            // Alamat Lengkap
            EditableField(
                label = stringResource(R.string.buka_rekening_dp_alamat_lengkap),
                value = state.alamatLengkap,
            )

            // RT/RW & Kode Pos (2-column)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_rt_rw),
                    value = state.rtRw,
                    modifier = Modifier.weight(1f),
                )
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_kode_pos),
                    value = state.kodePos,
                    modifier = Modifier.weight(1f),
                )
            }

            // Kelurahan & Kecamatan
            EditableField(
                label = stringResource(R.string.buka_rekening_dp_kelurahan),
                value = state.kelurahanKecamatan,
                trailingIcon = R.drawable.ic_search,
            )

            // Kota & Provinsi
            EditableField(
                label = stringResource(R.string.buka_rekening_dp_kota),
                value = state.kotaProvinsi,
                trailingIcon = R.drawable.ic_apartment,
            )

            // Domisili toggle
            DomisiliToggle(
                isChecked = state.alamatDomisiliSama,
                onToggle = onAlamatDomisiliToggle,
            )
        }
    }
}

@Composable
private fun DomisiliToggle(
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                AppColor.Primary100.copy(alpha = 0.4f),
                AppShape.R6,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_location_on),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s6),
            )
            Text(
                text = stringResource(R.string.buka_rekening_dp_domisili_sama),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        )
    }
}

// -- Section 3: Pekerjaan & Penghasilan ---------------------------------------

@Composable
private fun PekerjaanSection(
    state: BukaRekeningDataPribadiUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            SectionHeader(
                title = stringResource(R.string.buka_rekening_dp_section_pekerjaan),
                trailingIcon = R.drawable.ic_work,
            )

            DropdownField(
                label = stringResource(R.string.buka_rekening_dp_jenis_pekerjaan),
                value = state.jenisPekerjaan,
                leadingIcon = R.drawable.ic_business_center,
            )

            DropdownField(
                label = stringResource(R.string.buka_rekening_dp_penghasilan),
                value = state.penghasilanPerBulan,
                leadingIcon = R.drawable.ic_payments,
            )

            DropdownField(
                label = stringResource(R.string.buka_rekening_dp_sumber_dana),
                value = state.sumberDanaUtama,
                leadingIcon = R.drawable.ic_account_balance_wallet,
            )
        }
    }
}

// -- Security Banner ----------------------------------------------------------

@Composable
private fun SecurityBanner(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Text(
            text = stringResource(R.string.buka_rekening_dp_security),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s6)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    AppShape.R2,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s3),
            )
        }
    }
}

// -- Bottom Actions -----------------------------------------------------------

@Composable
private fun BottomActions(
    onLanjutClick: () -> Unit,
    onSimpanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                horizontal = Spacing.s4,
                vertical = Spacing.s3,
            ),
        ) {
            Button(
                onClick = onLanjutClick,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_dp_lanjut),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(Spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s5),
                )
            }

            TextButton(onClick = onSimpanClick) {
                Text(
                    text = stringResource(R.string.buka_rekening_dp_simpan),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun BukaRekeningDataPribadiPreview() {
    BcaMobileTheme {
        BukaRekeningDataPribadiScreen(
            state = BukaRekeningDataPribadiUiState(),
            onJenisKelaminSelect = {},
            onAlamatDomisiliToggle = {},
            onLanjutClick = {},
            onSimpanClick = {},
            onBackClick = {},
        )
    }
}