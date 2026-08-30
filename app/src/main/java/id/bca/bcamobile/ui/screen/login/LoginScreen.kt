package id.bca.bcamobile.ui.screen.login

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.AppTypography
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State ────────────────────────────────────────────────────────────────

data class LoginUiState(
    val userName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    state: LoginUiState,
    onMbcaLoginClick: () -> Unit,
    onFaceIdClick: () -> Unit,
    onFingerprintClick: () -> Unit,
    onBukaRekeningClick: () -> Unit,
    onGantiKodeAksesClick: () -> Unit,
    onInfoBcaClick: () -> Unit,
    onFlazzClick: () -> Unit,
    onKlikBcaClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.s6)
                .padding(top = Spacing.s10, bottom = Spacing.s8),
        ) {
            Spacer(Modifier.height(Spacing.s4))

            GreetingSection()

            Spacer(Modifier.weight(1f))

            if (state.error != null) {
                ErrorBanner(message = state.error, onRetry = onRetry)
                Spacer(Modifier.height(Spacing.s4))
            }

            LoginActionsSection(
                isLoading = state.isLoading,
                onMbcaLoginClick = onMbcaLoginClick,
                onFingerprintClick = onFingerprintClick,
                onFaceIdClick = onFaceIdClick,
                onBukaRekeningClick = onBukaRekeningClick,
                onGantiKodeAksesClick = onGantiKodeAksesClick,
            )

            Spacer(Modifier.weight(0.6f))

            QuickLinksSection(
                onInfoBcaClick = onInfoBcaClick,
                onFlazzClick = onFlazzClick,
                onKlikBcaClick = onKlikBcaClick,
            )

            Spacer(Modifier.height(Spacing.s4))
        }
    }
}

// ── Greeting Section ────────────────────────────────────────────────────

@Composable
private fun GreetingSection(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.login_selamat_datang),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColor.Neutral100.copy(alpha = AppAlpha.A90),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.s8))

        // Logo container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.BiometricButton)
                .clip(AppShape.R7)
                .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10))
                .border(
                    width = StrokeWidth.w0,
                    color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                    shape = AppShape.R7,
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.bca_logo_white),
                contentDescription = stringResource(R.string.cd_bca_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(Spacing.s3))

        Text(
            text = stringResource(R.string.login_bca_title),
            style = AppTypography.displaySmall,
            color = AppColor.Neutral100,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.login_mobile_subtitle),
            style = MaterialTheme.typography.headlineMedium,
            color = AppColor.Neutral100.copy(alpha = AppAlpha.A90),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Login Actions Section ───────────────────────────────────────────────

@Composable
private fun LoginActionsSection(
    isLoading: Boolean,
    onMbcaLoginClick: () -> Unit,
    onFingerprintClick: () -> Unit,
    onFaceIdClick: () -> Unit,
    onBukaRekeningClick: () -> Unit,
    onGantiKodeAksesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        modifier = modifier.fillMaxWidth(),
    ) {
        PrimaryLoginButton(
            isLoading = isLoading,
            onLoginClick = onMbcaLoginClick,
            onFingerprintClick = onFingerprintClick,
            onFaceIdClick = onFaceIdClick,
        )

        SecondaryActionButton(
            text = stringResource(R.string.login_buka_rekening),
            onClick = onBukaRekeningClick,
        )

        SecondaryActionButton(
            text = stringResource(R.string.login_ganti_kode_akses),
            onClick = onGantiKodeAksesClick,
        )
    }
}

// ── Primary Login Button ────────────────────────────────────────────────

@Composable
private fun PrimaryLoginButton(
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onFingerprintClick: () -> Unit,
    onFaceIdClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onLoginClick,
        enabled = !isLoading,
        shape = AppShape.R6,
        color = AppColor.Neutral100,
        shadowElevation = Spacing.s2,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Spacing.s4, horizontal = Spacing.s6),
        ) {
            // Green status indicator
            Box(
                modifier = Modifier
                    .size(Spacing.s2)
                    .background(AppColor.Success500, AppShape.Full),
            )

            Spacer(Modifier.width(Spacing.s3))

            Text(
                text = stringResource(R.string.login_mbca_login),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.s4)
                    .width(StrokeWidth.w0)
                    .height(Spacing.s6)
                    .background(AppColor.Neutral200),
            )

            // Biometric icons
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = stringResource(R.string.cd_login_fingerprint),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A60),
                modifier = Modifier.size(Spacing.s6),
            )

            Spacer(Modifier.width(Spacing.s2))

            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = stringResource(R.string.cd_login_face_id),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A60),
                modifier = Modifier.size(Spacing.s6),
            )
        }
    }
}

// ── Secondary Action Button ─────────────────────────────────────────────

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R6,
        color = AppColor.Neutral100.copy(alpha = AppAlpha.A10),
        modifier = modifier
            .fillMaxWidth()
            .height(AppSize.MinTouchTarget)
            .border(
                width = StrokeWidth.w0,
                color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                shape = AppShape.R6,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = AppColor.Neutral100,
            )
        }
    }
}

// ── Error Banner ────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = AppColor.Danger600.copy(alpha = AppAlpha.A20),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColor.Neutral100,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRetry,
                shape = AppShape.R4,
                color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
            ) {
                Text(
                    text = stringResource(R.string.mutasi_coba_lagi),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColor.Neutral100,
                    modifier = Modifier.padding(
                        horizontal = Spacing.s3,
                        vertical = Spacing.s1,
                    ),
                )
            }
        }
    }
}

// ── Quick Links Section ─────────────────────────────────────────────────

@Composable
private fun QuickLinksSection(
    onInfoBcaClick: () -> Unit,
    onFlazzClick: () -> Unit,
    onKlikBcaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s9),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        QuickLinkItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColor.Neutral100,
                    modifier = Modifier.size(Spacing.s6),
                )
            },
            label = stringResource(R.string.login_info_bca),
            onClick = onInfoBcaClick,
        )
        QuickLinkItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_credit_card),
                    contentDescription = null,
                    tint = AppColor.Neutral100,
                    modifier = Modifier.size(Spacing.s6),
                )
            },
            label = stringResource(R.string.login_flazz),
            onClick = onFlazzClick,
        )
        QuickLinkItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_language),
                    contentDescription = null,
                    tint = AppColor.Neutral100,
                    modifier = Modifier.size(Spacing.s6),
                )
            },
            label = stringResource(R.string.login_klikbca),
            onClick = onKlikBcaClick,
        )
    }
}

@Composable
private fun QuickLinkItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppSize.MinTouchTarget)
                    .background(
                        color = AppColor.Neutral100.copy(alpha = AppAlpha.A10),
                        shape = AppShape.Full,
                    )
                    .border(
                        width = StrokeWidth.w0,
                        color = AppColor.Neutral100.copy(alpha = AppAlpha.A10),
                        shape = AppShape.Full,
                    ),
            ) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColor.Neutral100.copy(alpha = AppAlpha.A80),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Login - Default")
@Composable
private fun LoginScreenPreview() {
    BcaMobileTheme {
        LoginScreen(
            state = LoginUiState(userName = "Jonathan Doe"),
            onMbcaLoginClick = {},
            onFaceIdClick = {},
            onFingerprintClick = {},
            onBukaRekeningClick = {},
            onGantiKodeAksesClick = {},
            onInfoBcaClick = {},
            onFlazzClick = {},
            onKlikBcaClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Login - Dark")
@Composable
private fun LoginScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        LoginScreen(
            state = LoginUiState(userName = "Jonathan Doe"),
            onMbcaLoginClick = {},
            onFaceIdClick = {},
            onFingerprintClick = {},
            onBukaRekeningClick = {},
            onGantiKodeAksesClick = {},
            onInfoBcaClick = {},
            onFlazzClick = {},
            onKlikBcaClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Login - Error")
@Composable
private fun LoginScreenErrorPreview() {
    BcaMobileTheme {
        LoginScreen(
            state = LoginUiState(
                userName = "Jonathan Doe",
                error = stringResource(R.string.error_connection_failed),
            ),
            onMbcaLoginClick = {},
            onFaceIdClick = {},
            onFingerprintClick = {},
            onBukaRekeningClick = {},
            onGantiKodeAksesClick = {},
            onInfoBcaClick = {},
            onFlazzClick = {},
            onKlikBcaClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Login - Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    BcaMobileTheme {
        LoginScreen(
            state = LoginUiState(
                userName = "Jonathan Doe",
                isLoading = true,
            ),
            onMbcaLoginClick = {},
            onFaceIdClick = {},
            onFingerprintClick = {},
            onBukaRekeningClick = {},
            onGantiKodeAksesClick = {},
            onInfoBcaClick = {},
            onFlazzClick = {},
            onKlikBcaClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Login - Long Name", widthDp = 360)
@Composable
private fun LoginScreenLongNamePreview() {
    BcaMobileTheme {
        LoginScreen(
            state = LoginUiState(userName = "Muhammad Alexander Jonathan Doe bin Abdullah"),
            onMbcaLoginClick = {},
            onFaceIdClick = {},
            onFingerprintClick = {},
            onBukaRekeningClick = {},
            onGantiKodeAksesClick = {},
            onInfoBcaClick = {},
            onFlazzClick = {},
            onKlikBcaClick = {},
            onRetry = {},
        )
    }
}