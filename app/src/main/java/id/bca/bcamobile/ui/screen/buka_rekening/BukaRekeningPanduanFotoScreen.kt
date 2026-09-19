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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// -- Data Model ---------------------------------------------------------------

private data class Requirement(
    val title: String,
    val description: String,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningPanduanFotoScreen(
    onMulaiAmbilFoto: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.buka_rekening_baru_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = Spacing.s1,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Button(
                    onClick = onMulaiAmbilFoto,
                    shape = AppShape.R6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.s4),
                ) {
                    Text(
                        text = stringResource(R.string.buka_rekening_foto_mulai),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(Spacing.s2))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4),
        ) {
            // Step Progress + Heading
            Column(
                modifier = Modifier.padding(top = Spacing.s2, bottom = Spacing.s4),
            ) {
                StepProgressIndicator(
                    currentStep = 3,
                    totalSteps = 8,
                    stepLabel = stringResource(R.string.buka_rekening_foto_step_label),
                )
                Spacer(Modifier.height(Spacing.s3))
                Text(
                    text = stringResource(R.string.buka_rekening_foto_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_foto_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Central Visual Card
            CentralVisualCard()

            Spacer(Modifier.height(Spacing.s6))

            // Comparison Section
            ComparisonSection()

            Spacer(Modifier.height(Spacing.s6))

            // Security Banner
            SecurityBanner()

            Spacer(Modifier.height(Spacing.s3))

            // Tips Button
            TextButton(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.buka_rekening_foto_tips),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.height(Spacing.s4))
        }
    }
}

// -- Central Visual Card ------------------------------------------------------

@Composable
private fun CentralVisualCard(modifier: Modifier = Modifier) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s5),
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
        ) {
            KtpMockupFrame()
            RequirementsChecklist()
        }
    }
}

// -- KTP Mockup Frame ---------------------------------------------------------

@Composable
private fun KtpMockupFrame(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s4),
    ) {
        // KTP card shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(Spacing.s0, AppShape.R4)
                .clip(AppShape.R4)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(Spacing.s3),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.58f),
            ) {
                // Header row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
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
                    Text(
                        text = stringResource(R.string.buka_rekening_foto_ktp_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                                .fillMaxWidth(0.75f)
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
                                .fillMaxWidth(0.83f)
                                .height(Spacing.s1)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    AppShape.Full,
                                ),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.67f)
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .width(Spacing.s8)
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
                        Text(
                            text = stringResource(R.string.buka_rekening_foto_ktp_valid),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }

        // Scan hint badge
        Spacer(Modifier.height(Spacing.s3))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.Full,
                )
                .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crop_free),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s3),
            )
            Text(
                text = stringResource(R.string.buka_rekening_foto_scan_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Requirements Checklist ---------------------------------------------------

@Composable
private fun RequirementsChecklist(modifier: Modifier = Modifier) {
    val requirements = listOf(
        Requirement(
            title = stringResource(R.string.buka_rekening_foto_req_1_title),
            description = stringResource(R.string.buka_rekening_foto_req_1_desc),
        ),
        Requirement(
            title = stringResource(R.string.buka_rekening_foto_req_2_title),
            description = stringResource(R.string.buka_rekening_foto_req_2_desc),
        ),
        Requirement(
            title = stringResource(R.string.buka_rekening_foto_req_3_title),
            description = stringResource(R.string.buka_rekening_foto_req_3_desc),
        ),
        Requirement(
            title = stringResource(R.string.buka_rekening_foto_req_4_title),
            description = stringResource(R.string.buka_rekening_foto_req_4_desc),
        ),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier,
    ) {
        requirements.forEach { req ->
            RequirementItem(requirement = req)
        }
    }
}

@Composable
private fun RequirementItem(requirement: Requirement) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s5)
                .background(AppColor.Success100, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Spacing.s3),
            )
        }
        Column {
            Text(
                text = requirement.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = requirement.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Comparison Section -------------------------------------------------------

@Composable
private fun ComparisonSection(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_foto_comparison_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.buka_rekening_foto_comparison_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            ComparisonCard(
                isCorrect = true,
                title = stringResource(R.string.buka_rekening_foto_benar_title),
                description = stringResource(R.string.buka_rekening_foto_benar_desc),
                badgeLabel = stringResource(R.string.buka_rekening_foto_benar),
                modifier = Modifier.weight(1f),
            )
            ComparisonCard(
                isCorrect = false,
                title = stringResource(R.string.buka_rekening_foto_salah_title),
                description = stringResource(R.string.buka_rekening_foto_salah_desc),
                badgeLabel = stringResource(R.string.buka_rekening_foto_salah),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    isCorrect: Boolean,
    title: String,
    description: String,
    badgeLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            modifier = Modifier.padding(Spacing.s3),
        ) {
            // Illustration area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        AppShape.R4,
                    ),
            ) {
                // Simplified KTP illustration
                val cardModifier = if (isCorrect) {
                    Modifier
                } else {
                    Modifier.rotate(3f)
                }
                val barColor = if (isCorrect) {
                    AppColor.Primary100
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                val barAlpha = if (isCorrect) 1f else 0.6f

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = cardModifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1.58f)
                        .background(
                            if (isCorrect) AppColor.Secondary100
                            else MaterialTheme.colorScheme.surfaceContainer,
                            AppShape.R2,
                        )
                        .padding(Spacing.s2),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            Modifier
                                .width(Spacing.s3)
                                .height(Spacing.s0)
                                .background(barColor.copy(alpha = barAlpha), AppShape.Full),
                        )
                        Box(
                            Modifier
                                .size(Spacing.s1)
                                .background(barColor.copy(alpha = barAlpha), AppShape.Full),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(width = Spacing.s4, height = Spacing.s5)
                                .background(barColor.copy(alpha = 0.3f), AppShape.R1),
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.s0),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(Spacing.s0)
                                    .background(barColor.copy(alpha = 0.4f), AppShape.Full),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(Spacing.s0)
                                    .background(barColor.copy(alpha = 0.3f), AppShape.Full),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(Spacing.s0)
                            .background(barColor.copy(alpha = 0.3f), AppShape.Full),
                    )
                }

                // Glare overlay for incorrect
                if (!isCorrect) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                AppShape.R4,
                            ),
                    )
                }

                // Badge
                val badgeBg = if (isCorrect) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
                val badgeContentColor = if (isCorrect) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
                val badgeIcon = if (isCorrect) {
                    R.drawable.ic_check_circle
                } else {
                    R.drawable.ic_cancel
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s0),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.s1)
                        .background(badgeBg, AppShape.Full)
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s0),
                ) {
                    Icon(
                        painter = painterResource(badgeIcon),
                        contentDescription = null,
                        tint = badgeContentColor,
                        modifier = Modifier.size(Spacing.s3),
                    )
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeContentColor,
                    )
                }
            }

            // Labels
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// -- Security Banner ----------------------------------------------------------

@Composable
private fun SecurityBanner(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(AppColor.Primary100, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.buka_rekening_foto_keamanan_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.buka_rekening_foto_keamanan_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun BukaRekeningPanduanFotoPreview() {
    BcaMobileTheme {
        BukaRekeningPanduanFotoScreen(
            onMulaiAmbilFoto = {},
            onBackClick = {},
        )
    }
}