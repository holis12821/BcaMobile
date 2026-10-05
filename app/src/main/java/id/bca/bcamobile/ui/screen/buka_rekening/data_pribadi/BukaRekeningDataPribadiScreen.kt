package id.bca.bcamobile.ui.screen.buka_rekening.data_pribadi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.components.bottomBarSafePadding
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth
import id.bca.bcamobile.ui.screen.buka_rekening.common.BukaRekeningLangkah
import id.bca.bcamobile.ui.screen.buka_rekening.common.DataPribadiField
import id.bca.bcamobile.ui.screen.buka_rekening.common.JenisKelamin
import id.bca.bcamobile.ui.screen.buka_rekening.common.Pekerjaan
import id.bca.bcamobile.ui.screen.buka_rekening.common.Penghasilan
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator
import id.bca.bcamobile.ui.screen.buka_rekening.common.SumberDana
import java.time.Instant
import java.time.Year

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningDataPribadiScreen(
    state: BukaRekeningDataPribadiUiState,
    onFieldChange: (DataPribadiField, String) -> Unit,
    onBirthDateSelect: (Long) -> Unit,
    onJenisKelaminSelect: (JenisKelamin) -> Unit,
    onAlamatDomisiliToggle: (Boolean) -> Unit,
    onPekerjaanSelect: (Pekerjaan) -> Unit,
    onPenghasilanSelect: (Penghasilan) -> Unit,
    onSumberDanaSelect: (SumberDana) -> Unit,
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
                isLanjutEnabled = state.isLanjutEnabled,
                error = state.error,
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
                langkah = BukaRekeningLangkah.DATA_PRIBADI,
            )

            // Intro banner
            IntroBanner()

            // Section 1: Identitas
            IdentitasSection(
                state = state,
                onFieldChange = onFieldChange,
                onBirthDateSelect = onBirthDateSelect,
                onJenisKelaminSelect = onJenisKelaminSelect,
            )

            // Section 2: Alamat
            AlamatSection(
                state = state,
                onFieldChange = onFieldChange,
                onAlamatDomisiliToggle = onAlamatDomisiliToggle,
            )

            // Section 3: Kontak
            KontakSection(
                state = state,
                onFieldChange = onFieldChange,
            )

            // Section 4: Pekerjaan
            PekerjaanSection(
                state = state,
                onPekerjaanSelect = onPekerjaanSelect,
                onPenghasilanSelect = onPenghasilanSelect,
                onSumberDanaSelect = onSumberDanaSelect,
            )

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
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    trailingIcon: Int? = null,
    trailingBadge: String? = null,
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

/**
 * Field yang benar-benar bisa diketik.
 *
 * Memakai [BasicTextField] dan bukan `TextField` Material supaya kotaknya tetap
 * sama persis dengan desain — `TextField` membawa indikator dan padding bawaan
 * yang tidak dipakai layar ini.
 */
@Composable
private fun EditableField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    trailingIcon: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    imeAction: ImeAction = ImeAction.Next,
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
            modifier = Modifier
                .fillMaxWidth()
                .errorBorder(error != null),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = capitalization,
                    imeAction = imeAction,
                ),
                decorationBox = { innerTextField ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppSize.MinTouchTarget)
                            .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            innerTextField()
                        }
                        if (trailingIcon != null) {
                            Icon(
                                painter = painterResource(trailingIcon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppSize.IconSmall),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FieldError(error)
    }
}

/**
 * Field yang membuka pemilih, bukan papan ketik — dipakai tanggal lahir.
 *
 * Tanggal tidak boleh diketik bebas: yang dikirim ke server `yyyy-MM-dd`,
 * sedangkan yang terbaca nasabah `21 April 1995`.
 */
@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    trailingIcon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
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
            modifier = Modifier
                .fillMaxWidth()
                .errorBorder(error != null)
                .clip(AppShape.R6)
                .clickable(onClick = onClick),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = AppSize.MinTouchTarget)
                    .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(trailingIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
            }
        }
        FieldError(error)
    }
}

/**
 * Daftar pilihan tertutup.
 *
 * Nilainya enum, bukan teks bebas: `pekerjaan`, `penghasilan_per_bulan`, dan
 * `sumber_dana_utama` punya daftar nilai tetap di kontrak API.
 */
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    leadingIcon: Int,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Surface(
                shape = AppShape.R6,
                shadowElevation = Spacing.s0,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShape.R6)
                    .clickable { isExpanded = true },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .defaultMinSize(minHeight = AppSize.MinTouchTarget)
                        .padding(horizontal = Spacing.s3, vertical = Spacing.s3),
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
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                options.forEach { (option, optionLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            isExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Garis merah tipis saat isinya belum sah; tanpa error kotaknya tetap polos. */
@Composable
private fun Modifier.errorBorder(hasError: Boolean): Modifier = if (hasError) {
    border(StrokeWidth.w0, MaterialTheme.colorScheme.error, AppShape.R6)
} else {
    this
}

// -- Section 1: Identitas Kependudukan ----------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentitasSection(
    state: BukaRekeningDataPribadiUiState,
    onFieldChange: (DataPribadiField, String) -> Unit,
    onBirthDateSelect: (Long) -> Unit,
    onJenisKelaminSelect: (JenisKelamin) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }

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

            // NIK — terkunci, wajib sama dengan hasil OCR
            ReadOnlyField(
                label = stringResource(R.string.buka_rekening_dp_nik_label),
                value = state.nik,
                leadingIcon = R.drawable.ic_credit_card,
                trailingIcon = R.drawable.ic_check_circle,
                trailingBadge = stringResource(R.string.buka_rekening_dp_nik_verified),
            )

            // Nama — terkunci, wajib sama dengan hasil OCR
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
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_tempat_lahir),
                    onValueChange = { onFieldChange(DataPribadiField.TEMPAT_LAHIR, it) },
                    error = state.fieldErrors[DataPribadiField.TEMPAT_LAHIR],
                    modifier = Modifier.weight(1f),
                )
                PickerField(
                    label = stringResource(R.string.buka_rekening_dp_tanggal_lahir),
                    value = state.tanggalLahir,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_tanggal_lahir),
                    trailingIcon = R.drawable.ic_calendar_today,
                    onClick = { isDatePickerVisible = true },
                    error = state.tanggalLahirError,
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

    if (isDatePickerVisible) {
        BirthDatePickerDialog(
            onDismiss = { isDatePickerVisible = false },
            onConfirm = {
                onBirthDateSelect(it)
                isDatePickerVisible = false
            },
        )
    }
}

/**
 * Pemilih tanggal lahir.
 *
 * Tanggal setelah hari ini dimatikan di pemilihnya, bukan ditolak sesudahnya —
 * tanggal lahir di masa depan tidak punya arti dan tidak perlu dibiarkan terpilih.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val pickerState = rememberDatePickerState(selectableDates = PastDates)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let(onConfirm) ?: onDismiss() },
            ) {
                Text(stringResource(R.string.buka_rekening_dp_pilih))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.buka_rekening_dp_batal))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= Instant.now().toEpochMilli()

    override fun isSelectableYear(year: Int): Boolean = year <= Year.now().value
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
            .defaultMinSize(minHeight = AppSize.MinTouchTarget)
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
    onFieldChange: (DataPribadiField, String) -> Unit,
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
                placeholder = stringResource(R.string.buka_rekening_dp_hint_alamat),
                onValueChange = { onFieldChange(DataPribadiField.ALAMAT_LENGKAP, it) },
                error = state.fieldErrors[DataPribadiField.ALAMAT_LENGKAP],
            )

            // RT/RW & Kode Pos (2-column)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_rt_rw),
                    value = state.rtRw,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_rt_rw),
                    onValueChange = { onFieldChange(DataPribadiField.RT_RW, it) },
                    error = state.fieldErrors[DataPribadiField.RT_RW],
                    // Papan ketik teks, bukan angka: garis miring pemisah RT dan RW
                    // tidak ada di papan angka. Penyaringan karakternya tetap jalan
                    // di `withField`, jadi huruf tidak bisa masuk.
                    capitalization = KeyboardCapitalization.None,
                    modifier = Modifier.weight(1f),
                )
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_kode_pos),
                    value = state.kodePos,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_kode_pos),
                    onValueChange = { onFieldChange(DataPribadiField.KODE_POS, it) },
                    error = state.fieldErrors[DataPribadiField.KODE_POS],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            // Kelurahan & Kecamatan (2-column) — terpisah karena payload memisahkannya
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_kelurahan_label),
                    value = state.kelurahan,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_kelurahan),
                    onValueChange = { onFieldChange(DataPribadiField.KELURAHAN, it) },
                    error = state.fieldErrors[DataPribadiField.KELURAHAN],
                    modifier = Modifier.weight(1f),
                )
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_kecamatan_label),
                    value = state.kecamatan,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_kecamatan),
                    onValueChange = { onFieldChange(DataPribadiField.KECAMATAN, it) },
                    error = state.fieldErrors[DataPribadiField.KECAMATAN],
                    modifier = Modifier.weight(1f),
                )
            }

            // Kota & Provinsi (2-column)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_kota_label),
                    value = state.kota,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_kota),
                    onValueChange = { onFieldChange(DataPribadiField.KOTA, it) },
                    error = state.fieldErrors[DataPribadiField.KOTA],
                    modifier = Modifier.weight(1f),
                )
                EditableField(
                    label = stringResource(R.string.buka_rekening_dp_provinsi_label),
                    value = state.provinsi,
                    placeholder = stringResource(R.string.buka_rekening_dp_hint_provinsi),
                    onValueChange = { onFieldChange(DataPribadiField.PROVINSI, it) },
                    error = state.fieldErrors[DataPribadiField.PROVINSI],
                    modifier = Modifier.weight(1f),
                )
            }

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

// -- Section 3: Kontak --------------------------------------------------------

/**
 * Nomor HP dan email.
 *
 * Tidak ada di e-KTP dan tidak ada di artefak desain, tapi `personal_data`
 * mewajibkan keduanya — dan nomor inilah yang menerima OTP di layar berikutnya,
 * jadi diberi kartu sendiri alih-alih dititipkan ke section Identitas.
 */
@Composable
private fun KontakSection(
    state: BukaRekeningDataPribadiUiState,
    onFieldChange: (DataPribadiField, String) -> Unit,
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
                title = stringResource(R.string.buka_rekening_dp_section_kontak),
                trailingIcon = R.drawable.ic_smartphone,
            )

            EditableField(
                label = stringResource(R.string.buka_rekening_dp_nomor_hp),
                value = state.nomorHp,
                placeholder = stringResource(R.string.buka_rekening_dp_hint_nomor_hp),
                onValueChange = { onFieldChange(DataPribadiField.NOMOR_HP, it) },
                error = state.fieldErrors[DataPribadiField.NOMOR_HP],
                keyboardType = KeyboardType.Phone,
                capitalization = KeyboardCapitalization.None,
            )

            EditableField(
                label = stringResource(R.string.buka_rekening_dp_email),
                value = state.email,
                placeholder = stringResource(R.string.buka_rekening_dp_hint_email),
                onValueChange = { onFieldChange(DataPribadiField.EMAIL, it) },
                error = state.fieldErrors[DataPribadiField.EMAIL],
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R4)
                    .padding(Spacing.s3),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_dp_kontak_catatan),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// -- Section 4: Pekerjaan & Penghasilan ---------------------------------------

@Composable
private fun PekerjaanSection(
    state: BukaRekeningDataPribadiUiState,
    onPekerjaanSelect: (Pekerjaan) -> Unit,
    onPenghasilanSelect: (Penghasilan) -> Unit,
    onSumberDanaSelect: (SumberDana) -> Unit,
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
                options = pekerjaanOptions(),
                onSelect = onPekerjaanSelect,
            )

            DropdownField(
                label = stringResource(R.string.buka_rekening_dp_penghasilan),
                value = state.penghasilanPerBulan,
                leadingIcon = R.drawable.ic_payments,
                options = penghasilanOptions(),
                onSelect = onPenghasilanSelect,
            )

            DropdownField(
                label = stringResource(R.string.buka_rekening_dp_sumber_dana),
                value = state.sumberDanaUtama,
                leadingIcon = R.drawable.ic_account_balance_wallet,
                options = sumberDanaOptions(),
                onSelect = onSumberDanaSelect,
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
    isLanjutEnabled: Boolean,
    error: String?,
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
            modifier = Modifier
                .bottomBarSafePadding()
                .padding(
                    horizontal = Spacing.s4,
                    vertical = Spacing.s3,
                ),
        ) {
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = Spacing.s2),
                )
            }

            Button(
                onClick = onLanjutClick,
                enabled = isLanjutEnabled,
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
            state = BukaRekeningDataPribadiUiState(
                nik = "3174082104950001",
                namaLengkap = "MUHAMMAD ARDAN PRAYOGI",
                tempatLahir = "Jakarta",
                tanggalLahir = "21 April 1995",
                alamatLengkap = "Jl. Sudirman Kav. 45 No. 12B",
                rtRw = "003/005",
                kelurahan = "Senayan",
                kecamatan = "Kebayoran Baru",
                kota = "Jakarta Selatan",
                provinsi = "DKI Jakarta",
                jenisPekerjaan = "Karyawan Swasta",
                penghasilanPerBulan = "Rp 10.000.000 - Rp 20.000.000",
                sumberDanaUtama = "Gaji",
            ),
            onFieldChange = { _, _ -> },
            onBirthDateSelect = {},
            onJenisKelaminSelect = {},
            onAlamatDomisiliToggle = {},
            onPekerjaanSelect = {},
            onPenghasilanSelect = {},
            onSumberDanaSelect = {},
            onLanjutClick = {},
            onSimpanClick = {},
            onBackClick = {},
        )
    }
}
