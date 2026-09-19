package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------

data class VerifikasiBiometrikUiState(
    val faceDetected: Boolean = false,
    val precisionPercent: Int = 98,
    val currentAction: Int = 2,
    val totalActions: Int = 3,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningVerifikasiBiometrikScreen(
    state: VerifikasiBiometrikUiState,
    onMulaiClick: () -> Unit,
    onTipsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_biometrik_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomCtaSection(
                onMulaiClick = onMulaiClick,
                onTipsClick = onTipsClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Step progress
            StepProgressIndicator(
                currentStep = 7,
                totalSteps = 11,
                stepLabel = stringResource(R.string.buka_rekening_biometrik_step_label),
                modifier = Modifier.padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s4,
                ),
            )

            Spacer(Modifier.height(Spacing.s3))

            // Header text
            BiometrikHeader(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s3))

            // Biometric viewfinder
            BiometrikViewfinder(
                state = state,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s2))

            // Panduan pengambilan card
            PanduanPengambilanCard(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s2))

            // Security regulation card
            SecurityRegulationCard(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s3))
        }
    }
}

// -- Header -------------------------------------------------------------------

@Composable
private fun BiometrikHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_heading),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.s1))
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Biometric Viewfinder -----------------------------------------------------

@Composable
private fun BiometrikViewfinder(
    state: VerifikasiBiometrikUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(AppShape.R7)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Gradient scrim overlay (top + bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.A30),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.A50),
                        ),
                    ),
                ),
        )

        // Oval targeting guideline
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.s4)
                .border(
                    width = StrokeWidth.w1,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A80),
                    shape = AppShape.Full,
                ),
        ) {
            // Animated sweep scan line
            SweepScanLine()

            // Crosshair tick marks (horizontal)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.s2),
            ) {
                Box(
                    modifier = Modifier
                        .width(Spacing.s2)
                        .height(Spacing.s0)
                        .background(AppColor.Primary100.copy(alpha = AppAlpha.A30)),
                )
                Box(
                    modifier = Modifier
                        .width(Spacing.s2)
                        .height(Spacing.s0)
                        .background(AppColor.Primary100.copy(alpha = AppAlpha.A30)),
                )
            }

            // Crosshair tick marks (vertical)
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = Spacing.s2),
            ) {
                Box(
                    modifier = Modifier
                        .height(Spacing.s2)
                        .width(Spacing.s0)
                        .background(AppColor.Primary100.copy(alpha = AppAlpha.A30)),
                )
                Box(
                    modifier = Modifier
                        .height(Spacing.s2)
                        .width(Spacing.s0)
                        .background(AppColor.Primary100.copy(alpha = AppAlpha.A30)),
                )
            }
        }

        // Face detected status pill (top)
        if (state.faceDetected) {
            FaceDetectedPill(
                precisionPercent = state.precisionPercent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.s7),
            )
        }

        // Instruction card (bottom inside viewfinder)
        LivenessInstructionCard(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    bottom = Spacing.s4,
                ),
        )
    }
}

// -- Sweep Scan Line ----------------------------------------------------------

@Composable
private fun SweepScanLine(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val offsetY by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = Spacing.s10 * offsetY)
            .height(Spacing.s0)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                        AppColor.Primary100.copy(alpha = AppAlpha.A70),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}

// -- Face Detected Pill -------------------------------------------------------

@Composable
private fun FaceDetectedPill(
    precisionPercent: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AppAlpha.A90),
                AppShape.Full,
            )
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(Spacing.s4),
        )
        Text(
            text = stringResource(
                R.string.buka_rekening_biometrik_face_status,
                precisionPercent,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

// -- Liveness Instruction Card ------------------------------------------------

@Composable
private fun LivenessInstructionCard(
    state: VerifikasiBiometrikUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = AppAlpha.A90),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.padding(Spacing.s3),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s7)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_visibility),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.buka_rekening_biometrik_blink),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(
                            R.string.buka_rekening_biometrik_liveness_progress,
                            state.currentAction,
                            state.totalActions,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Liveness progress bar
            LinearProgressIndicator(
                progress = {
                    if (state.totalActions > 0) {
                        state.currentAction.toFloat() / state.totalActions.toFloat()
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.s1)
                    .clip(AppShape.Full),
                color = MaterialTheme.colorScheme.primaryContainer,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}

// -- Panduan Pengambilan Card -------------------------------------------------

@Composable
private fun PanduanPengambilanCard(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R7,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            // Title row
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_verified_user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_biometrik_panduan_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Checklist items
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_1),
            )
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_2),
            )
            PanduanItem(
                text = stringResource(R.string.buka_rekening_biometrik_panduan_3),
            )
        }
    }
}

@Composable
private fun PanduanItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s5)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = AppAlpha.A10),
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Spacing.s3),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Security Regulation Card -------------------------------------------------

@Composable
private fun SecurityRegulationCard(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                AppColor.Secondary100.copy(alpha = AppAlpha.A30),
                AppShape.R7,
            )
            .padding(Spacing.s3),
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
                painter = painterResource(R.drawable.ic_security),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s5),
            )
        }
        Text(
            text = stringResource(R.string.buka_rekening_biometrik_security),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// -- Bottom CTA Section -------------------------------------------------------

@Composable
private fun BottomCtaSection(
    onMulaiClick: () -> Unit,
    onTipsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                horizontal = Spacing.s4,
                vertical = Spacing.s3,
            ),
        ) {
            Button(
                onClick = onMulaiClick,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.MinTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_biometrik_mulai),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(Spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.s5),
                )
            }

            TextButton(
                onClick = onTipsClick,
                shape = AppShape.R6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help_outline),
                    contentDescription = null,
                    modifier = Modifier.size(AppSize.IconSmall),
                )
                Spacer(Modifier.width(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_biometrik_tips),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// -- Preview ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun VerifikasiBiometrikPreview() {
    BcaMobileTheme {
        BukaRekeningVerifikasiBiometrikScreen(
            state = VerifikasiBiometrikUiState(
                faceDetected = true,
                precisionPercent = 98,
                currentAction = 2,
                totalActions = 3,
            ),
            onMulaiClick = {},
            onTipsClick = {},
            onBackClick = {},
        )
    }
}