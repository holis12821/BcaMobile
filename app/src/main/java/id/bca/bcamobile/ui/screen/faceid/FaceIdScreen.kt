package id.bca.bcamobile.ui.screen.faceid

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Model ──────────────────────────────────────────────────────────

enum class FaceIdStatus {
    SCANNING,
    SUCCESS,
    FAILED,
}

data class FaceIdUiState(
    val status: FaceIdStatus = FaceIdStatus.SCANNING,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceIdScreen(
    state: FaceIdUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.faceid_title),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_faceid_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.s4),
        ) {
            ScannerFrame(status = state.status)

            Spacer(Modifier.height(Spacing.s7))

            StatusText(status = state.status)

            Spacer(Modifier.height(Spacing.s8))

            when (state.status) {
                FaceIdStatus.SCANNING -> {
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = AppShape.R6,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.faceid_cancel),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                FaceIdStatus.SUCCESS -> {
                    // No button in success state — auto-navigate
                }

                FaceIdStatus.FAILED -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = onRetryClick,
                            shape = AppShape.R6,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.faceid_retry),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = AppShape.R6,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.faceid_cancel),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun ScannerFrame(
    status: FaceIdStatus,
    modifier: Modifier = Modifier,
) {
    val cornerColor = when (status) {
        FaceIdStatus.SCANNING -> MaterialTheme.colorScheme.primary
        FaceIdStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        FaceIdStatus.FAILED -> MaterialTheme.colorScheme.error
    }

    val scanLineColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLine",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(AppSize.ScannerFrame)
            .drawBehind {
                val strokeWidthPx = StrokeWidth.w1.toPx()
                val cornerLength = Spacing.s7.toPx()

                // Top-left corner
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, strokeWidthPx / 2),
                    end = Offset(cornerLength, strokeWidthPx / 2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(strokeWidthPx / 2, 0f),
                    end = Offset(strokeWidthPx / 2, cornerLength),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )

                // Top-right corner
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - cornerLength, strokeWidthPx / 2),
                    end = Offset(size.width, strokeWidthPx / 2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - strokeWidthPx / 2, 0f),
                    end = Offset(size.width - strokeWidthPx / 2, cornerLength),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )

                // Bottom-left corner
                drawLine(
                    color = cornerColor,
                    start = Offset(0f, size.height - strokeWidthPx / 2),
                    end = Offset(cornerLength, size.height - strokeWidthPx / 2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(strokeWidthPx / 2, size.height - cornerLength),
                    end = Offset(strokeWidthPx / 2, size.height),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )

                // Bottom-right corner
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - cornerLength, size.height - strokeWidthPx / 2),
                    end = Offset(size.width, size.height - strokeWidthPx / 2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(size.width - strokeWidthPx / 2, size.height - cornerLength),
                    end = Offset(size.width - strokeWidthPx / 2, size.height),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )

                // Scanning line (only when scanning)
                if (status == FaceIdStatus.SCANNING) {
                    val padding = Spacing.s4.toPx()
                    val scanY = padding + (size.height - padding * 2) * scanLineProgress
                    drawLine(
                        color = scanLineColor,
                        start = Offset(padding, scanY),
                        end = Offset(size.width - padding, scanY),
                        strokeWidth = StrokeWidth.w0.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
    ) {
        when (status) {
            FaceIdStatus.SCANNING -> {
                // Empty center during scanning — camera feed goes here
            }

            FaceIdStatus.SUCCESS -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.BiometricButton)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s7),
                    )
                }
            }

            FaceIdStatus.FAILED -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.BiometricButton)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            AppShape.Full,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Spacing.s7),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(
    status: FaceIdStatus,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        val (title, subtitle) = when (status) {
            FaceIdStatus.SCANNING -> Pair(
                stringResource(R.string.faceid_instruction),
                stringResource(R.string.faceid_verifying),
            )

            FaceIdStatus.SUCCESS -> Pair(
                stringResource(R.string.faceid_success),
                stringResource(R.string.faceid_logging_in),
            )

            FaceIdStatus.FAILED -> Pair(
                stringResource(R.string.faceid_failed),
                null,
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = when (status) {
                FaceIdStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Light - Scanning")
@Composable
private fun FaceIdScreenScanningPreview() {
    BcaMobileTheme {
        FaceIdScreen(
            state = FaceIdUiState(status = FaceIdStatus.SCANNING),
            onBackClick = {},
            onRetryClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark - Scanning")
@Composable
private fun FaceIdScreenScanningDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        FaceIdScreen(
            state = FaceIdUiState(status = FaceIdStatus.SCANNING),
            onBackClick = {},
            onRetryClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun FaceIdScreenSuccessPreview() {
    BcaMobileTheme {
        FaceIdScreen(
            state = FaceIdUiState(status = FaceIdStatus.SUCCESS),
            onBackClick = {},
            onRetryClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Failed")
@Composable
private fun FaceIdScreenFailedPreview() {
    BcaMobileTheme {
        FaceIdScreen(
            state = FaceIdUiState(status = FaceIdStatus.FAILED),
            onBackClick = {},
            onRetryClick = {},
            onCancelClick = {},
        )
    }
}