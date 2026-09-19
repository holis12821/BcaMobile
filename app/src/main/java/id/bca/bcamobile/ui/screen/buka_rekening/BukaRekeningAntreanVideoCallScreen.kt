package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// -- Data Model ---------------------------------------------------------------

data class AntreanVideoCallUiState(
    val nomorAntrean: String = "A-042",
    val jumlahAntreanDepan: Int = 2,
    val estimasiMenit: Int = 3,
    val petugasSiap: Boolean = true,
    val isKtpReady: Boolean = true,
    val isKoneksiStabil: Boolean = true,
    val isRuanganTenang: Boolean = true,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningAntreanVideoCallScreen(
    state: AntreanVideoCallUiState,
    onTungguClick: () -> Unit,
    onJadwalkanClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_antrean_title),
                onBackClick = onBackClick,
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
            // Step progress with bg
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                StepProgressIndicator(
                    currentStep = 8,
                    totalSteps = 11,
                    stepLabel = stringResource(R.string.buka_rekening_antrean_step_label),
                    modifier = Modifier.padding(
                        start = Spacing.s4,
                        end = Spacing.s4,
                        top = Spacing.s3,
                        bottom = Spacing.s4,
                    ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s6),
                modifier = Modifier.padding(Spacing.s4),
            ) {
                // Screen heading
                AntreanHeader()

                // Live queue hero card
                QueueHeroCard(state = state)

                // Preparation checklist
                PreparationChecklistCard(state = state)

                // Operating hours
                OperatingHoursCard()

                // Bottom actions
                AntreanBottomActions(
                    onTungguClick = onTungguClick,
                    onJadwalkanClick = onJadwalkanClick,
                )
            }
        }
    }
}

// -- Header -------------------------------------------------------------------

@Composable
private fun AntreanHeader(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_video_call),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconLarge),
            )
            Text(
                text = stringResource(R.string.buka_rekening_antrean_heading),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.buka_rekening_antrean_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Queue Hero Card ----------------------------------------------------------

@Composable
private fun QueueHeroCard(
    state: AntreanVideoCallUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s1,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
            modifier = Modifier.padding(Spacing.s5),
        ) {
            // Status badges row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // "Sedang Mengantre" badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            AppColor.Secondary100.copy(alpha = AppAlpha.A50),
                            AppShape.Full,
                        )
                        .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
                ) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.s2)
                            .background(MaterialTheme.colorScheme.primary, AppShape.Full),
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_antrean_status),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // "Jaringan Prima" badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            AppColor.Success100.copy(alpha = AppAlpha.A30),
                            AppShape.Full,
                        )
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wifi),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Spacing.s4),
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_antrean_jaringan),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // Ticket number display
            Surface(
                shape = AppShape.R4,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = Spacing.s2),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_antrean_nomor_label)
                            .uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.nomorAntrean,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_groups),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(AppSize.IconSmall),
                        )
                        Text(
                            text = stringResource(
                                R.string.buka_rekening_antrean_jumlah_depan,
                                state.jumlahAntreanDepan,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Estimated time badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        AppColor.Primary100.copy(alpha = AppAlpha.A30),
                        AppShape.R4,
                    )
                    .padding(Spacing.s3),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hourglass_top),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.s5),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.buka_rekening_antrean_waktu_label)
                                .uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                R.string.buka_rekening_antrean_waktu_value,
                                state.estimasiMenit,
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.buka_rekening_antrean_prioritas),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = AppAlpha.A80),
                            AppShape.R2,
                        )
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
                )
            }

            // Agent preview row
            if (state.petugasSiap) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar placeholder
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Spacing.s9)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                AppShape.Full,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Spacing.s6),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.buka_rekening_antrean_petugas_siap),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Spacing.s1)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        AppShape.Full,
                                    ),
                            )
                            Text(
                                text = stringResource(R.string.buka_rekening_antrean_petugas_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Caution notice
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        AppColor.Danger100.copy(alpha = AppAlpha.A20),
                        AppShape.R4,
                    )
                    .padding(Spacing.s2),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(AppSize.IconSmall)
                        .padding(top = Spacing.s0),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_antrean_caution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// -- Preparation Checklist Card -----------------------------------------------

@Composable
private fun PreparationChecklistCard(
    state: AntreanVideoCallUiState,
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
            modifier = Modifier.padding(Spacing.s5),
        ) {
            // Title row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_antrean_siapkan_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.buka_rekening_antrean_siapkan_count),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Checklist items
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                ChecklistItem(
                    iconRes = R.drawable.ic_credit_card,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.A10),
                    title = stringResource(R.string.buka_rekening_antrean_ktp_title),
                    description = stringResource(R.string.buka_rekening_antrean_ktp_desc),
                    isChecked = state.isKtpReady,
                )
                ChecklistItem(
                    iconRes = R.drawable.ic_signal_cellular_alt,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBgColor = MaterialTheme.colorScheme.secondary.copy(alpha = AppAlpha.A10),
                    title = stringResource(R.string.buka_rekening_antrean_koneksi_title),
                    description = stringResource(R.string.buka_rekening_antrean_koneksi_desc),
                    isChecked = state.isKoneksiStabil,
                )
                ChecklistItem(
                    iconRes = R.drawable.ic_record_voice_over,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = AppAlpha.A10),
                    title = stringResource(R.string.buka_rekening_antrean_ruangan_title),
                    description = stringResource(R.string.buka_rekening_antrean_ruangan_desc),
                    isChecked = state.isRuanganTenang,
                )
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    iconBgColor: Color,
    title: String,
    description: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R4)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s8)
                .background(iconBgColor, AppShape.R6),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Spacing.s5),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isChecked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Spacing.s5),
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Operating Hours Card -----------------------------------------------------

@Composable
private fun OperatingHoursCard(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            // Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(Spacing.s5),
                )
                Text(
                    text = stringResource(R.string.buka_rekening_antrean_jam_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Hours row
            Surface(
                shape = AppShape.R4,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.s3),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_antrean_jam_hari),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.buka_rekening_antrean_jam_waktu),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Description
            Text(
                text = stringResource(R.string.buka_rekening_antrean_jam_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Bottom Actions -----------------------------------------------------------

@Composable
private fun AntreanBottomActions(
    onTungguClick: () -> Unit,
    onJadwalkanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier,
    ) {
        // Primary: Tunggu Panggilan Sekarang
        Button(
            onClick = onTungguClick,
            shape = AppShape.R6,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.MinTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_phone_in_talk),
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.buka_rekening_antrean_tunggu),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // Secondary: Jadwalkan Panggilan Nanti
        OutlinedButton(
            onClick = onJadwalkanClick,
            shape = AppShape.R6,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSize.MinTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_today),
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResource(R.string.buka_rekening_antrean_jadwalkan),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // Encryption footer
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
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
                text = stringResource(R.string.buka_rekening_antrean_enkripsi),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Preview ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun AntreanVideoCallPreview() {
    BcaMobileTheme {
        BukaRekeningAntreanVideoCallScreen(
            state = AntreanVideoCallUiState(),
            onTungguClick = {},
            onJadwalkanClick = {},
            onBackClick = {},
        )
    }
}