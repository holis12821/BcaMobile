package id.bca.bcamobile.ui.screen.akun

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State ────────────────────────────────────────────────────────────────

enum class AkunMenuItem {
    UBAH_PIN,
    ATUR_LIMIT,
    NOTIFIKASI_PUSH,
    EMAIL_STATEMENT,
    PUSAT_BANTUAN,
    HUBUNGI_CS,
    TENTANG_APLIKASI,
}

data class AkunUiState(
    val userName: String = "",
    val phoneNumber: String = "",
    val appVersion: String = "",
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun AkunScreen(
    state: AkunUiState,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLihatProfilClick: () -> Unit,
    onMenuItemClick: (AkunMenuItem) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onKeluarClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AkunTopBar(
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(Spacing.s4))
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.mutasi_coba_lagi))
                    }
                }
            }

            else -> {
                AkunContent(
                    state = state,
                    onLihatProfilClick = onLihatProfilClick,
                    onMenuItemClick = onMenuItemClick,
                    onBiometricToggle = onBiometricToggle,
                    onKeluarClick = onKeluarClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

// ── Content ──────────────────────────────────────────────────────────────

@Composable
private fun AkunContent(
    state: AkunUiState,
    onLihatProfilClick: () -> Unit,
    onMenuItemClick: (AkunMenuItem) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onKeluarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s4)
            .padding(top = Spacing.s4),
    ) {
        ProfileHeaderCard(
            userName = state.userName,
            phoneNumber = state.phoneNumber,
            onClick = onLihatProfilClick,
        )

        Spacer(Modifier.height(Spacing.s6))

        // ── AKUN ──
        SettingsSection(label = stringResource(R.string.akun_section_akun)) {
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                label = stringResource(R.string.akun_ubah_pin),
                onClick = { onMenuItemClick(AkunMenuItem.UBAH_PIN) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_atur_limit),
                onClick = { onMenuItemClick(AkunMenuItem.ATUR_LIMIT) },
                iconBackgroundColor = AppColor.Secondary100.copy(alpha = AppAlpha.A20),
                iconTintColor = MaterialTheme.colorScheme.secondary,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_fingerprint),
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_login_biometrik),
                onClick = { onBiometricToggle(!state.isBiometricEnabled) },
                iconBackgroundColor = AppColor.Success700.copy(alpha = AppAlpha.A20),
                iconTintColor = AppColor.Success700,
                trailing = {
                    Switch(
                        checked = state.isBiometricEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = AppColor.Neutral100,
                        ),
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── NOTIFIKASI ──
        SettingsSection(label = stringResource(R.string.akun_section_notifikasi)) {
            SettingsRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                    )
                },
                label = stringResource(R.string.akun_notifikasi_push),
                subtitle = stringResource(R.string.akun_notifikasi_subtitle),
                onClick = { onMenuItemClick(AkunMenuItem.NOTIFIKASI_PUSH) },
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                label = stringResource(R.string.akun_email_estatement),
                onClick = { onMenuItemClick(AkunMenuItem.EMAIL_STATEMENT) },
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── BANTUAN & INFO ──
        SettingsSection(label = stringResource(R.string.akun_section_bantuan)) {
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                label = stringResource(R.string.akun_pusat_bantuan),
                onClick = { onMenuItemClick(AkunMenuItem.PUSAT_BANTUAN) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                label = stringResource(R.string.akun_hubungi_cs),
                onClick = { onMenuItemClick(AkunMenuItem.HUBUNGI_CS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                label = stringResource(R.string.akun_tentang_aplikasi),
                onClick = { onMenuItemClick(AkunMenuItem.TENTANG_APLIKASI) },
                trailing = {
                    Text(
                        text = state.appVersion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.s6))

        // ── Logout ──
        Button(
            onClick = onKeluarClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = AppShape.R6,
            contentPadding = PaddingValues(horizontal = Spacing.s6, vertical = Spacing.s4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.akun_keluar),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(Spacing.s7))
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AkunTopBar(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                Image(
                    painter = painterResource(R.drawable.bca_logo_white),
                    contentDescription = stringResource(R.string.cd_bca_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(Spacing.s6),
                )
                Text(
                    text = stringResource(R.string.nav_akun),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.cd_notifikasi),
                )
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Spacing.s8)
                        .border(
                            width = StrokeWidth.w1,
                            color = AppColor.Neutral100.copy(alpha = AppAlpha.A20),
                            shape = AppShape.Full,
                        )
                        .clip(AppShape.Full)
                        .background(AppColor.Neutral100.copy(alpha = AppAlpha.A10)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        modifier = Modifier.size(Spacing.s6),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = AppColor.Neutral100,
            actionIconContentColor = AppColor.Neutral100,
        ),
        modifier = modifier,
    )
}

// ── Profile Header Card ─────────────────────────────────────────────────

@Composable
private fun ProfileHeaderCard(
    userName: String,
    phoneNumber: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.s0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s10)
                    .clip(AppShape.Full)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.s7),
                )
            }

            Spacer(Modifier.width(Spacing.s4))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.s1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.akun_lihat_profil),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s4),
                    )
                }
            }
        }
    }
}

// ── Settings Components ─────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s2),
        )
        Spacer(Modifier.height(Spacing.s3))
        Surface(
            shape = AppShape.R6,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Spacing.s0,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = AppColor.Primary100.copy(alpha = AppAlpha.A20),
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.s4),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .clip(AppShape.Full)
                .background(iconBackgroundColor),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides iconTintColor,
            ) {
                icon()
            }
        }

        Spacer(Modifier.width(Spacing.s3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        trailing()
    }
}

@Composable
private fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier.padding(start = Spacing.s4 + Spacing.s8),
    )
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AkunScreenPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(
                userName = "Budi Santoso",
                phoneNumber = "0812 3456 7890",
                appVersion = "v2.4.1",
                isBiometricEnabled = true,
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onBiometricToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AkunScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        AkunScreen(
            state = AkunUiState(
                userName = "Budi Santoso",
                phoneNumber = "0812 3456 7890",
                appVersion = "v2.4.1",
                isBiometricEnabled = true,
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onBiometricToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AkunScreenLoadingPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(isLoading = true),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onBiometricToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AkunScreenErrorPreview() {
    BcaMobileTheme {
        AkunScreen(
            state = AkunUiState(
                error = stringResource(R.string.error_general_retry),
            ),
            onNotificationClick = {},
            onProfileClick = {},
            onLihatProfilClick = {},
            onMenuItemClick = {},
            onBiometricToggle = {},
            onKeluarClick = {},
            onRetry = {},
        )
    }
}