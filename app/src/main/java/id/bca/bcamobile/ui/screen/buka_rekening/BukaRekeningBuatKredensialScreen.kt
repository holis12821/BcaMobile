package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// -- Data Model ---------------------------------------------------------------

data class BuatKredensialUiState(
    val kodeAkses: String = "",
    val konfirmasiKodeAkses: String = "",
    val isKodeAksesVisible: Boolean = false,
    val isKonfirmasiVisible: Boolean = false,
    val pinDigitCount: Int = 0,
    val konfirmasiPinDigitCount: Int = 0,
    val isPinCocok: Boolean = false,
    val isValid6Karakter: Boolean = false,
    val isValidTidakBerurutan: Boolean = false,
    val isValidTidakBerulang: Boolean = false,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningBuatKredensialScreen(
    state: BuatKredensialUiState,
    onKodeAksesChange: (String) -> Unit,
    onKonfirmasiKodeAksesChange: (String) -> Unit,
    onToggleKodeAksesVisibility: () -> Unit,
    onToggleKonfirmasiVisibility: () -> Unit,
    onSimpanClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_kredensial_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomActionBar(onSimpanClick = onSimpanClick)
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4)
                .padding(top = Spacing.s3, bottom = Spacing.s4),
        ) {
            // Progress indicator
            StepProgressIndicator(
                currentStep = 9,
                totalSteps = 11,
                stepLabel = stringResource(R.string.buka_rekening_kredensial_step_label),
            )

            // Header section
            HeaderSection()

            // Security tips banner
            SecurityTipsBanner()

            // Kode Akses card
            KodeAksesCard(
                kodeAkses = state.kodeAkses,
                konfirmasi = state.konfirmasiKodeAkses,
                isKodeAksesVisible = state.isKodeAksesVisible,
                isKonfirmasiVisible = state.isKonfirmasiVisible,
                isValid6Karakter = state.isValid6Karakter,
                isValidTidakBerurutan = state.isValidTidakBerurutan,
                isValidTidakBerulang = state.isValidTidakBerulang,
                onKodeAksesChange = onKodeAksesChange,
                onKonfirmasiChange = onKonfirmasiKodeAksesChange,
                onToggleKodeAksesVisibility = onToggleKodeAksesVisibility,
                onToggleKonfirmasiVisibility = onToggleKonfirmasiVisibility,
            )

            // PIN Finansial card
            PinFinansialCard(
                pinDigitCount = state.pinDigitCount,
                konfirmasiPinDigitCount = state.konfirmasiPinDigitCount,
                isPinCocok = state.isPinCocok,
            )

            // Biometric option
            BiometricOption()
        }
    }
}

// -- Header Section -----------------------------------------------------------

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        // Security badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_kredensial_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        AppShape.Full,
                    )
                    .padding(horizontal = Spacing.s3, vertical = Spacing.s0),
            )
            Icon(
                painter = painterResource(R.drawable.ic_verified_user),
                contentDescription = null,
                tint = AppColor.Success500,
                modifier = Modifier.size(Spacing.s4),
            )
        }

        // Title
        Text(
            text = stringResource(R.string.buka_rekening_kredensial_heading),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Subtitle
        Text(
            text = stringResource(R.string.buka_rekening_kredensial_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Security Tips Banner -----------------------------------------------------

@Composable
private fun SecurityTipsBanner(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(
                AppColor.Primary100.copy(alpha = AppAlpha.A50),
                AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        // Shield icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s9)
                .background(MaterialTheme.colorScheme.primary, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Spacing.s5),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_kredensial_tips_title),
                style = MaterialTheme.typography.labelLarge,
                color = AppColor.Primary900,
            )
            Text(
                text = stringResource(R.string.buka_rekening_kredensial_tips_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Kode Akses Card ----------------------------------------------------------

@Composable
private fun KodeAksesCard(
    kodeAkses: String,
    konfirmasi: String,
    isKodeAksesVisible: Boolean,
    isKonfirmasiVisible: Boolean,
    isValid6Karakter: Boolean,
    isValidTidakBerurutan: Boolean,
    isValidTidakBerulang: Boolean,
    onKodeAksesChange: (String) -> Unit,
    onKonfirmasiChange: (String) -> Unit,
    onToggleKodeAksesVisibility: () -> Unit,
    onToggleKonfirmasiVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.s0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s5),
        ) {
            // Section header
            SectionHeader(
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.buka_rekening_kredensial_kode_title),
                subtitle = stringResource(R.string.buka_rekening_kredensial_kode_subtitle),
            )

            // Kode Akses input
            PasswordInputField(
                label = stringResource(R.string.buka_rekening_kredensial_kode_baru),
                value = kodeAkses,
                placeholder = stringResource(R.string.buka_rekening_kredensial_kode_placeholder),
                isVisible = isKodeAksesVisible,
                onValueChange = onKodeAksesChange,
                onToggleVisibility = onToggleKodeAksesVisibility,
            )

            // Konfirmasi Kode Akses input
            PasswordInputField(
                label = stringResource(R.string.buka_rekening_kredensial_kode_konfirmasi),
                value = konfirmasi,
                placeholder = stringResource(R.string.buka_rekening_kredensial_kode_konfirmasi_placeholder),
                isVisible = isKonfirmasiVisible,
                onValueChange = onKonfirmasiChange,
                onToggleVisibility = onToggleKonfirmasiVisibility,
            )

            // Validation checklist
            ValidationChecklist(
                isValid6Karakter = isValid6Karakter,
                isValidTidakBerurutan = isValidTidakBerurutan,
                isValidTidakBerulang = isValidTidakBerulang,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    iconRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .background(MaterialTheme.colorScheme.primary, AppShape.R6),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Spacing.s6),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PasswordInputField(
    label: String,
    value: String,
    placeholder: String,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.MinTouchTarget)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    AppShape.R4,
                )
                .padding(horizontal = Spacing.s4),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { if (it.length <= 6) onValueChange(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing,
                ),
                visualTransformation = if (isVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            innerTextField()
                        }
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.size(Spacing.s8),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isVisible) R.drawable.ic_visibility_off
                                    else R.drawable.ic_visibility,
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Spacing.s5),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ValidationChecklist(
    isValid6Karakter: Boolean,
    isValidTidakBerurutan: Boolean,
    isValidTidakBerulang: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                AppShape.R4,
            )
            .padding(Spacing.s3),
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_kredensial_validasi_kriteria).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ValidationItem(
            text = stringResource(R.string.buka_rekening_kredensial_validasi_6char),
            isValid = isValid6Karakter,
        )
        ValidationItem(
            text = stringResource(R.string.buka_rekening_kredensial_validasi_berurutan),
            isValid = isValidTidakBerurutan,
        )
        ValidationItem(
            text = stringResource(R.string.buka_rekening_kredensial_validasi_berulang),
            isValid = isValidTidakBerulang,
        )
    }
}

@Composable
private fun ValidationItem(
    text: String,
    isValid: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = if (isValid) AppColor.Success500 else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(Spacing.s5),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -- PIN Finansial Card -------------------------------------------------------

@Composable
private fun PinFinansialCard(
    pinDigitCount: Int,
    konfirmasiPinDigitCount: Int,
    isPinCocok: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.s0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
            modifier = Modifier.padding(Spacing.s5),
        ) {
            // Section header
            SectionHeader(
                iconRes = R.drawable.ic_security,
                title = stringResource(R.string.buka_rekening_kredensial_pin_title),
                subtitle = stringResource(R.string.buka_rekening_kredensial_pin_subtitle),
            )

            // PIN Transaksi Baru
            PinDotsSection(
                label = stringResource(R.string.buka_rekening_kredensial_pin_baru),
                trailingLabel = stringResource(R.string.buka_rekening_kredensial_pin_6digit),
                filledCount = pinDigitCount,
                dotColor = MaterialTheme.colorScheme.primary,
            )

            // Konfirmasi PIN
            PinDotsSection(
                label = stringResource(R.string.buka_rekening_kredensial_pin_konfirmasi),
                filledCount = konfirmasiPinDigitCount,
                dotColor = if (isPinCocok) AppColor.Success500
                else MaterialTheme.colorScheme.primary,
                matchBadge = if (isPinCocok) stringResource(R.string.buka_rekening_kredensial_pin_cocok)
                else null,
            )

            // Info note
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        AppShape.R4,
                    )
                    .padding(Spacing.s3),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_kredensial_pin_info),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PinDotsSection(
    label: String,
    filledCount: Int,
    dotColor: androidx.compose.ui.graphics.Color,
    trailingLabel: String? = null,
    matchBadge: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        // Label row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (matchBadge != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AppColor.Success100, AppShape.Full)
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = AppColor.Success700,
                        modifier = Modifier.size(Spacing.s3),
                    )
                    Text(
                        text = matchBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = AppColor.Success700,
                    )
                }
            } else if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // 6 PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    AppShape.R4,
                )
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        ) {
            Spacer(Modifier.weight(1f))
            repeat(6) { index ->
                val isFilled = index < filledCount
                Box(
                    modifier = Modifier
                        .size(Spacing.s4)
                        .then(
                            if (isFilled) {
                                Modifier
                                    .background(dotColor, AppShape.Full)
                                    .border(
                                        Spacing.s1,
                                        dotColor.copy(alpha = AppAlpha.A30),
                                        AppShape.Full,
                                    )
                            } else {
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        AppShape.Full,
                                    )
                                    .border(
                                        Spacing.s0,
                                        MaterialTheme.colorScheme.outline.copy(alpha = AppAlpha.A30),
                                        AppShape.Full,
                                    )
                            },
                        ),
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

// -- Biometric Option ---------------------------------------------------------

@Composable
private fun BiometricOption(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        // Fingerprint icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Spacing.s5),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_kredensial_biometrik_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.buka_rekening_kredensial_biometrik_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // "Tersedia" badge
        Text(
            text = stringResource(R.string.buka_rekening_kredensial_biometrik_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.Full,
                )
                .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        )
    }
}

// -- Bottom Action Bar --------------------------------------------------------

@Composable
private fun BottomActionBar(
    onSimpanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s2,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                horizontal = Spacing.s4,
                vertical = Spacing.s4,
            ),
        ) {
            Button(
                onClick = onSimpanClick,
                shape = AppShape.R6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.BiometricButton),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_kredensial_simpan),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.width(Spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s5),
                )
            }

            Spacer(Modifier.height(Spacing.s3))

            // Encryption footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = AppColor.Success500,
                    modifier = Modifier.size(Spacing.s3),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_kredensial_enkripsi),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun BuatKredensialPreview() {
    BcaMobileTheme {
        BukaRekeningBuatKredensialScreen(
            state = BuatKredensialUiState(
                kodeAkses = "Bca202",
                konfirmasiKodeAkses = "Bca202",
                pinDigitCount = 6,
                konfirmasiPinDigitCount = 6,
                isPinCocok = true,
                isValid6Karakter = true,
                isValidTidakBerurutan = true,
                isValidTidakBerulang = true,
            ),
            onKodeAksesChange = {},
            onKonfirmasiKodeAksesChange = {},
            onToggleKodeAksesVisibility = {},
            onToggleKonfirmasiVisibility = {},
            onSimpanClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BuatKredensialEmptyPreview() {
    BcaMobileTheme {
        BukaRekeningBuatKredensialScreen(
            state = BuatKredensialUiState(),
            onKodeAksesChange = {},
            onKonfirmasiKodeAksesChange = {},
            onToggleKodeAksesVisibility = {},
            onToggleKonfirmasiVisibility = {},
            onSimpanClick = {},
            onBackClick = {},
        )
    }
}