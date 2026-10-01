package id.bca.bcamobile.ui.screen.buka_rekening.hasil_foto

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.screen.buka_rekening.common.StepProgressIndicator

// -- Data Model ---------------------------------------------------------------


// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningHasilFotoScreen(
    state: BukaRekeningHasilFotoUiState,
    onGunakanFoto: () -> Unit,
    onAmbilUlang: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_kamera_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomActions(
                onGunakanFoto = onGunakanFoto,
                onAmbilUlang = onAmbilUlang,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
        ) {
            // Step Progress + Heading
            Column(
                modifier = Modifier.padding(top = Spacing.s2),
            ) {
                StepProgressIndicator(
                    currentStep = 3,
                    totalSteps = 8,
                    stepLabel = stringResource(R.string.buka_rekening_hasil_step_label),
                )
                Spacer(Modifier.height(Spacing.s3))
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Photo Preview Card
            PhotoPreviewCard(
                isFotoValid = state.isFotoValid,
                resolusiInfo = state.resolusiInfo,
                ocrAccuracy = state.ocrAccuracy,
                onRetakeClick = onAmbilUlang,
            )

            // OCR Data Card
            OcrDataCard(state = state)

            // Security Disclaimer
            SecurityDisclaimer()

            Spacer(Modifier.height(Spacing.s2))
        }
    }
}

// -- Photo Preview Card -------------------------------------------------------

@Composable
private fun PhotoPreviewCard(
    isFotoValid: Boolean,
    resolusiInfo: String,
    ocrAccuracy: String,
    onRetakeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // KTP Photo area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.58f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(AppShape.None),
            ) {
                // KTP Illustration placeholder
                KtpPhotoPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )

                // Quality badge (top-left)
                if (isFotoValid) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.s3)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest
                                    .copy(alpha = AppAlpha.A90),
                                AppShape.Full,
                            )
                            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(Spacing.s4),
                        )
                        Text(
                            text = stringResource(R.string.buka_rekening_hasil_foto_tajam),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                // Retake badge (bottom-right)
                Surface(
                    onClick = onRetakeClick,
                    shape = AppShape.R4,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                        .copy(alpha = AppAlpha.A90),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.s3),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = Spacing.s3,
                            vertical = Spacing.s1,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cached),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(Spacing.s4),
                        )
                        Text(
                            text = stringResource(R.string.buka_rekening_hasil_foto_ulang_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Resolution info bar
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(Spacing.s3),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_document_scanner),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(Spacing.s5),
                    )
                    Text(
                        text = resolusiInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = ocrAccuracy,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            AppShape.R2,
                        )
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                )
            }
        }
    }
}

@Composable
private fun KtpPhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(Spacing.s5),
    ) {
        // Simulated captured KTP card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1.58f)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                    AppShape.R4,
                )
                .padding(Spacing.s3),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Spacing.s5)
                            .background(AppColor.Secondary100, AppShape.Full),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flag),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(Spacing.s3),
                        )
                    }
                    Column {
                        Box(
                            Modifier
                                .width(Spacing.s8)
                                .height(Spacing.s0)
                                .background(AppColor.Primary100, AppShape.Full),
                        )
                        Spacer(Modifier.height(Spacing.s0))
                        Box(
                            Modifier
                                .width(Spacing.s6)
                                .height(Spacing.s0)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    AppShape.Full,
                                ),
                        )
                    }
                }

                // Middle: photo + text lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = Spacing.s9, height = Spacing.s10)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                AppShape.R2,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(Spacing.s6),
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(Spacing.s2)
                                .background(AppColor.Primary100, AppShape.Full),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(Spacing.s1)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    AppShape.Full,
                                ),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .height(Spacing.s1)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    AppShape.Full,
                                ),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.65f)
                                .height(Spacing.s1)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    AppShape.Full,
                                ),
                        )
                    }
                }

                // Footer
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .width(Spacing.s7)
                            .height(Spacing.s1)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                AppShape.Full,
                            ),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s0),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_verified),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(Spacing.s3),
                        )
                    }
                }
            }
        }
    }
}

// -- OCR Data Card ------------------------------------------------------------

@Composable
private fun OcrDataCard(
    state: BukaRekeningHasilFotoUiState,
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
            // OCR Header
            OcrHeader()

            // Data fields
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                // NIK (highlighted)
                NikField(nik = state.nik)

                // Nama Lengkap
                OcrDataField(
                    label = stringResource(R.string.buka_rekening_hasil_nama_label),
                    value = state.namaLengkap,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

                // Tempat / Tanggal Lahir
                OcrDataField(
                    label = stringResource(R.string.buka_rekening_hasil_ttl_label),
                    value = state.tempatTanggalLahir,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

                // Alamat
                OcrDataField(
                    label = stringResource(R.string.buka_rekening_hasil_alamat_label),
                    value = state.alamat,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

                // Agama & Status Perkawinan (side by side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.s1),
                ) {
                    OcrDataField(
                        label = stringResource(R.string.buka_rekening_hasil_agama_label),
                        value = state.agama,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.weight(1f),
                        showPadding = false,
                    )
                    OcrDataField(
                        label = stringResource(R.string.buka_rekening_hasil_status_label),
                        value = state.statusPerkawinan,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.weight(1f),
                        showPadding = false,
                    )
                }
            }

            // Notice
            NoticeBox()
        }
    }
}

@Composable
private fun OcrHeader(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s7)
                    .background(AppColor.Primary100, AppShape.R4),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_smart_toy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_ocr_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_ocr_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Valid badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    AppShape.R3,
                )
                .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Spacing.s4),
            )
            Text(
                text = stringResource(R.string.buka_rekening_hasil_valid),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun NikField(
    nik: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier
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
                text = stringResource(R.string.buka_rekening_hasil_nik_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Text(
            text = nik,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OcrDataField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    showPadding: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s0),
        modifier = modifier.then(
            if (showPadding) Modifier.padding(horizontal = Spacing.s1) else Modifier,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NoticeBox(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
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
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(Spacing.s5)
                .padding(top = Spacing.s0),
        )
        Text(
            text = stringResource(R.string.buka_rekening_hasil_notice),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Security Disclaimer ------------------------------------------------------

@Composable
private fun SecurityDisclaimer(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Spacing.s4),
        )
        Spacer(Modifier.width(Spacing.s1))
        Text(
            text = stringResource(R.string.buka_rekening_hasil_security),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Bottom Actions -----------------------------------------------------------

@Composable
private fun BottomActions(
    onGunakanFoto: () -> Unit,
    onAmbilUlang: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Button(
                onClick = onGunakanFoto,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_gunakan),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(Spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s5),
                )
            }

            OutlinedButton(
                onClick = onAmbilUlang,
                shape = AppShape.R6,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s5),
                )
                Spacer(Modifier.width(Spacing.s2))
                Text(
                    text = stringResource(R.string.buka_rekening_hasil_ambil_ulang),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun BukaRekeningHasilFotoPreview() {
    BcaMobileTheme {
        BukaRekeningHasilFotoScreen(
            state = BukaRekeningHasilFotoUiState(),
            onGunakanFoto = {},
            onAmbilUlang = {},
            onBackClick = {},
        )
    }
}