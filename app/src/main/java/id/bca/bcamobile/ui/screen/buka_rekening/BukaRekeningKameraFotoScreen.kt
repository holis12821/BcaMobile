package id.bca.bcamobile.ui.screen.buka_rekening

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------

enum class FlashMode { AUTO, ON, OFF }

data class BukaRekeningKameraFotoUiState(
    val isAutoCaptureEnabled: Boolean = true,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isDetecting: Boolean = true,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningKameraFotoScreen(
    state: BukaRekeningKameraFotoUiState,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onHelpClick: () -> Unit,
    onFlashToggle: () -> Unit,
    onAutoCaptureToggle: (Boolean) -> Unit,
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
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Utility header + Instruction pill
            UtilityHeader(
                flashMode = state.flashMode,
                onFlashToggle = onFlashToggle,
            )

            // Camera viewfinder
            CameraViewfinder(
                isDetecting = state.isDetecting,
                modifier = Modifier.weight(1f),
            )

            // Tips section
            TipsSection()

            // Bottom controls
            BottomControlHub(
                isAutoCaptureEnabled = state.isAutoCaptureEnabled,
                onAutoCaptureToggle = onAutoCaptureToggle,
                onShutterClick = onShutterClick,
                onGalleryClick = onGalleryClick,
                onHelpClick = onHelpClick,
            )
        }
    }
}

// -- Utility Header -----------------------------------------------------------

@Composable
private fun UtilityHeader(
    flashMode: FlashMode,
    onFlashToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.padding(
            start = Spacing.s4,
            end = Spacing.s4,
            top = Spacing.s2,
            bottom = Spacing.s3,
        ),
    ) {
        // Top row: eKYC badge + Lensa Siap | Flash toggle
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // eKYC badge
                Text(
                    text = stringResource(R.string.buka_rekening_kamera_ekyc_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            AppShape.Full,
                        )
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                )

                // Lensa Siap indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.s2)
                            .background(MaterialTheme.colorScheme.tertiary, AppShape.Full),
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_kamera_lensa_siap),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Flash toggle button
            FlashToggleButton(
                flashMode = flashMode,
                onClick = onFlashToggle,
            )
        }

        // Instruction pill
        InstructionPill()
    }
}

@Composable
private fun FlashToggleButton(
    flashMode: FlashMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flashLabel = when (flashMode) {
        FlashMode.AUTO -> stringResource(R.string.buka_rekening_kamera_flash_auto)
        FlashMode.ON -> stringResource(R.string.buka_rekening_kamera_flash_on)
        FlashMode.OFF -> stringResource(R.string.buka_rekening_kamera_flash_off)
    }

    Surface(
        onClick = onClick,
        shape = AppShape.Full,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Spacing.s0,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.s3, vertical = Spacing.s2),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flash_auto),
                contentDescription = stringResource(R.string.cd_buka_rekening_kamera_flash),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
            Text(
                text = flashLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InstructionPill(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.inverseSurface.copy(alpha = AppAlpha.A90),
                    AppShape.Full,
                )
                .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crop_free),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inversePrimary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
            Text(
                text = stringResource(R.string.buka_rekening_kamera_instruction),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

// -- Camera Viewfinder --------------------------------------------------------

@Composable
private fun CameraViewfinder(
    isDetecting: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // KTP frame
            KtpViewfinderFrame(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1.586f),
            )

            Spacer(Modifier.height(Spacing.s4))

            // Detection status badge
            if (isDetecting) {
                DetectionBadge()
            }
        }
    }
}

@Composable
private fun KtpViewfinderFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(AppShape.R6)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = AppAlpha.A10),
            ),
    ) {
        // Corner brackets
        CornerBracket(Modifier.align(Alignment.TopStart))
        CornerBracket(Modifier.align(Alignment.TopEnd))
        CornerBracket(Modifier.align(Alignment.BottomStart))
        CornerBracket(Modifier.align(Alignment.BottomEnd))

        // Wireframe content
        KtpWireframe(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.s3),
        )
    }
}

@Composable
private fun CornerBracket(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Spacing.s6)
            .background(MaterialTheme.colorScheme.primary, AppShape.R2),
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.s5)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.inverseSurface, AppShape.R1),
        )
    }
}

@Composable
private fun KtpWireframe(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier,
    ) {
        // Header wireframe
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s1),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Box(
                    Modifier
                        .width(Spacing.s8)
                        .height(Spacing.s0)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest
                                .copy(alpha = AppAlpha.A50),
                            AppShape.Full,
                        ),
                )
                Box(
                    Modifier
                        .width(Spacing.s7)
                        .height(Spacing.s0)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest
                                .copy(alpha = AppAlpha.A30),
                            AppShape.Full,
                        ),
                )
            }
            Box(
                Modifier
                    .size(Spacing.s4)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest
                            .copy(alpha = AppAlpha.A30),
                        AppShape.Full,
                    ),
            )
        }

        // Mid body: text lines + photo area
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s1),
        ) {
            // Text lines
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_account_box),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceContainerLowest
                            .copy(alpha = AppAlpha.A50),
                        modifier = Modifier.size(Spacing.s3),
                    )
                    Box(
                        Modifier
                            .width(Spacing.s9)
                            .height(Spacing.s0)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest
                                    .copy(alpha = AppAlpha.A30),
                                AppShape.Full,
                            ),
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(Spacing.s0)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest
                                .copy(alpha = AppAlpha.A30),
                            AppShape.Full,
                        ),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(Spacing.s0)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest
                                .copy(alpha = AppAlpha.A20),
                            AppShape.Full,
                        ),
                )
            }

            Spacer(Modifier.width(Spacing.s2))

            // Photo avatar silhouette
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(width = Spacing.s8, height = Spacing.s9)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest
                            .copy(alpha = AppAlpha.A20),
                        AppShape.R4,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainerLowest
                        .copy(alpha = AppAlpha.A50),
                    modifier = Modifier.size(AppSize.IconLarge),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_kamera_area_pasfoto),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                        .copy(alpha = AppAlpha.A50),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Bottom: chip + hologram hints
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s1),
        ) {
            Box(
                Modifier
                    .size(width = Spacing.s7, height = Spacing.s5)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest
                            .copy(alpha = AppAlpha.A30),
                        AppShape.R2,
                    ),
            )
            Box(
                Modifier
                    .width(Spacing.s7)
                    .height(Spacing.s0)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest
                            .copy(alpha = AppAlpha.A30),
                        AppShape.Full,
                    ),
            )
        }
    }
}

@Composable
private fun DetectionBadge(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sync),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.s4),
        )
        Text(
            text = stringResource(R.string.buka_rekening_kamera_detection),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -- Tips Section -------------------------------------------------------------

@Composable
private fun TipsSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
    ) {
        Surface(
            shape = AppShape.R6,
            shadowElevation = Spacing.s0,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                modifier = Modifier.padding(Spacing.s3),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.IconLarge)
                        .background(AppColor.Primary100, AppShape.Full),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wb_incandescent),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.s0),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_kamera_tips_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_kamera_tips_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// -- Bottom Control Hub -------------------------------------------------------

@Composable
private fun BottomControlHub(
    isAutoCaptureEnabled: Boolean,
    onAutoCaptureToggle: (Boolean) -> Unit,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        // Auto-capture toggle
        AutoCaptureToggle(
            isEnabled = isAutoCaptureEnabled,
            onToggle = onAutoCaptureToggle,
        )

        // Shutter panel
        ShutterPanel(
            onGalleryClick = onGalleryClick,
            onShutterClick = onShutterClick,
            onHelpClick = onHelpClick,
        )

        // Security footer
        SecurityFooter()
    }
}

@Composable
private fun AutoCaptureToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
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
                    painter = painterResource(R.drawable.ic_center_focus_strong),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.buka_rekening_kamera_auto_capture),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.buka_rekening_kamera_auto_capture_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceDim,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        )
    }
}

@Composable
private fun ShutterPanel(
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s2),
    ) {
        // Gallery button
        SecondaryActionButton(
            iconRes = R.drawable.ic_photo_library,
            label = stringResource(R.string.buka_rekening_kamera_dari_galeri),
            onClick = onGalleryClick,
        )

        // Shutter button
        ShutterButton(onClick = onShutterClick)

        // Help button
        SecondaryActionButton(
            iconRes = R.drawable.ic_help_outline,
            label = stringResource(R.string.buka_rekening_kamera_bantuan),
            onClick = onHelpClick,
        )
    }
}

@Composable
private fun SecondaryActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(AppSize.MinTouchTarget)
                .background(MaterialTheme.colorScheme.surfaceContainer, AppShape.R6),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.s6),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, AppShape.Full)
            .padding(StrokeWidth.w2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.BiometricButton)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.Full),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s9)
                    .background(MaterialTheme.colorScheme.primaryContainer, AppShape.Full),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.cd_buka_rekening_kamera_foto),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Spacing.s6),
                )
            }
        }
    }
}

@Composable
private fun SecurityFooter(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s1),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Spacing.s3),
        )
        Spacer(Modifier.width(Spacing.s1))
        Text(
            text = stringResource(R.string.buka_rekening_kamera_security),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun BukaRekeningKameraFotoPreview() {
    BcaMobileTheme {
        BukaRekeningKameraFotoScreen(
            state = BukaRekeningKameraFotoUiState(),
            onShutterClick = {},
            onGalleryClick = {},
            onHelpClick = {},
            onFlashToggle = {},
            onAutoCaptureToggle = {},
            onBackClick = {},
        )
    }
}