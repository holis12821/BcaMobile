package id.bca.bcamobile.ui.screen.finger_print

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Model ──────────────────────────────────────────────────────────

enum class TouchIdStatus {
    IDLE,
    SCANNING,
    SUCCESS,
    FAILED,
}

data class TouchIdUiState(
    val status: TouchIdStatus = TouchIdStatus.IDLE,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun TouchIdScreen(
    state: TouchIdUiState,
    onFingerprintPress: () -> Unit,
    onUseAccessCode: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.s4),
    ) {
        TitleSection()

        Spacer(Modifier.height(Spacing.s10))

        FingerprintScanner(
            status = state.status,
            onPress = onFingerprintPress,
        )

        Spacer(Modifier.height(Spacing.s10))

        StatusText(status = state.status)

        Spacer(Modifier.height(Spacing.s10))

        BottomActions(
            status = state.status,
            onRetryClick = onRetryClick,
            onUseAccessCode = onUseAccessCode,
        )
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun TitleSection(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.finger_print_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.finger_print_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FingerprintScanner(
    status: TouchIdStatus,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAnimating = status == TouchIdStatus.IDLE || status == TouchIdStatus.SCANNING
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(AppSize.SplashLogo),
    ) {
        // Animated ping ring — only runs when status is IDLE or SCANNING
        if (isAnimating) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ringScale",
            )
            val ringAlpha by infiniteTransition.animateFloat(
                initialValue = AppAlpha.A30,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ringAlpha",
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .border(
                        width = StrokeWidth.w0,
                        color = primaryColor,
                        shape = AppShape.Full,
                    ),
            )
        }

        // Outer circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppAlpha.A30),
                    AppShape.Full,
                )
                .border(
                    width = StrokeWidth.w0,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppAlpha.A50),
                    shape = AppShape.Full,
                ),
        ) {
            // Inner decorative ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(Spacing.s4)
                    .fillMaxSize()
                    .border(
                        width = StrokeWidth.w0,
                        color = primaryColor.copy(alpha = AppAlpha.A20),
                        shape = AppShape.Full,
                    ),
            ) {
                // Center fingerprint button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppSize.LogoContainer)
                        .background(MaterialTheme.colorScheme.surface, AppShape.Full)
                        .border(
                            width = StrokeWidth.w0,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = AppShape.Full,
                        )
                        .clip(AppShape.Full)
                        .clickable(
                            onClick = onPress,
                            role = Role.Button,
                        ),
                ) {
                    when (status) {
                        TouchIdStatus.IDLE,
                        TouchIdStatus.SCANNING -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_fingerprint),
                                contentDescription = stringResource(R.string.cd_finger_print_scan),
                                tint = primaryColor,
                                modifier = Modifier.size(AppSize.BiometricButton),
                            )
                        }

                        TouchIdStatus.SUCCESS -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(AppSize.BiometricButton),
                            )
                        }

                        TouchIdStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(AppSize.BiometricButton),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(
    status: TouchIdStatus,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.padding(horizontal = Spacing.s6),
    ) {
        val (mainText, mainColor) = when (status) {
            TouchIdStatus.IDLE -> Pair(
                stringResource(R.string.finger_print_instruction),
                MaterialTheme.colorScheme.onSurface,
            )

            TouchIdStatus.SCANNING -> Pair(
                stringResource(R.string.finger_print_scanning),
                MaterialTheme.colorScheme.primary,
            )

            TouchIdStatus.SUCCESS -> Pair(
                stringResource(R.string.finger_print_success),
                MaterialTheme.colorScheme.primary,
            )

            TouchIdStatus.FAILED -> Pair(
                stringResource(R.string.finger_print_failed),
                MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = mainText,
            style = MaterialTheme.typography.headlineMedium,
            color = mainColor,
            textAlign = TextAlign.Center,
        )

        if (status == TouchIdStatus.IDLE) {
            Text(
                text = stringResource(R.string.finger_print_alt_method),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BottomActions(
    status: TouchIdStatus,
    onRetryClick: () -> Unit,
    onUseAccessCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (status == TouchIdStatus.FAILED) {
            Button(
                onClick = onRetryClick,
                shape = AppShape.R6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.finger_print_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        TextButton(
            onClick = onUseAccessCode,
            shape = AppShape.R6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.finger_print_use_access_code),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Light - Idle")
@Composable
private fun TouchIdScreenIdlePreview() {
    BcaMobileTheme {
        TouchIdScreen(
            state = TouchIdUiState(status = TouchIdStatus.IDLE),
            onFingerprintPress = {},
            onUseAccessCode = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark - Idle")
@Composable
private fun TouchIdScreenIdleDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        TouchIdScreen(
            state = TouchIdUiState(status = TouchIdStatus.IDLE),
            onFingerprintPress = {},
            onUseAccessCode = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Scanning")
@Composable
private fun TouchIdScreenScanningPreview() {
    BcaMobileTheme {
        TouchIdScreen(
            state = TouchIdUiState(status = TouchIdStatus.SCANNING),
            onFingerprintPress = {},
            onUseAccessCode = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun TouchIdScreenSuccessPreview() {
    BcaMobileTheme {
        TouchIdScreen(
            state = TouchIdUiState(status = TouchIdStatus.SUCCESS),
            onFingerprintPress = {},
            onUseAccessCode = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Failed")
@Composable
private fun TouchIdScreenFailedPreview() {
    BcaMobileTheme {
        TouchIdScreen(
            state = TouchIdUiState(status = TouchIdStatus.FAILED),
            onFingerprintPress = {},
            onUseAccessCode = {},
            onRetryClick = {},
        )
    }
}