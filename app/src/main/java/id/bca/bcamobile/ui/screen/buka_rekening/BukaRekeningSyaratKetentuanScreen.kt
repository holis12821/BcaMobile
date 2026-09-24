package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// -- Data Model ---------------------------------------------------------------

private data class TncSection(
    val iconRes: Int,
    val title: String,
    val body: String,
)

// -- Main Screen ---------------------------------------------------------------

@Composable
fun BukaRekeningSyaratKetentuanScreen(
    onAgreeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isChecked by remember { mutableStateOf(false) }

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
                AgreeButton(
                    enabled = isChecked,
                    onClick = onAgreeClick,
                    modifier = Modifier.padding(Spacing.s4),
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Step Progress + Heading
            Column(
                modifier = Modifier.padding(
                    start = Spacing.s4,
                    end = Spacing.s4,
                    top = Spacing.s2,
                    bottom = Spacing.s4,
                ),
            ) {
                StepProgressIndicator(
                    currentStep = 3,
                    totalSteps = 7,
                    stepLabel = stringResource(R.string.buka_rekening_sk_step_label),
                )
                Spacer(Modifier.height(Spacing.s3))
                Text(
                    text = stringResource(R.string.buka_rekening_sk_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = stringResource(R.string.buka_rekening_sk_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Trust Banner
            TrustBanner(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s4))

            // TnC Container (static card, no internal scroll)
            TncContainer(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )

            Spacer(Modifier.height(Spacing.s4))

            // Notice + Checkbox
            Column(
                modifier = Modifier.padding(horizontal = Spacing.s4),
            ) {
                PentingNoticeBox()

                Spacer(Modifier.height(Spacing.s4))

                AgreementCheckbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                )

                Spacer(Modifier.height(Spacing.s4))
            }
        }
    }
}

// -- Trust Banner -------------------------------------------------------------

@Composable
private fun TrustBanner(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s9)
                .background(AppColor.Primary100, AppShape.R4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified_user),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s6),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.buka_rekening_sk_trust_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.buka_rekening_sk_trust_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- TnC Container (scrollable) -----------------------------------------------

@Composable
private fun TncContainer(modifier: Modifier = Modifier) {
    val sections = tncSections()

    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s4),
        ) {
            sections.forEachIndexed { index, section ->
                TncSectionItem(section = section)
                if (index < sections.lastIndex) {
                    HorizontalDivider(
                        thickness = StrokeWidth.w0,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.padding(vertical = Spacing.s4),
                    )
                }
            }
        }
    }
}

@Composable
private fun tncSections(): List<TncSection> = listOf(
    TncSection(
        iconRes = R.drawable.ic_account_box,
        title = stringResource(R.string.buka_rekening_sk_section_1_title),
        body = stringResource(R.string.buka_rekening_sk_section_1_body),
    ),
    TncSection(
        iconRes = R.drawable.ic_verified_user,
        title = stringResource(R.string.buka_rekening_sk_section_2_title),
        body = stringResource(R.string.buka_rekening_sk_section_2_body),
    ),
    TncSection(
        iconRes = R.drawable.ic_video_call,
        title = stringResource(R.string.buka_rekening_sk_section_3_title),
        body = stringResource(R.string.buka_rekening_sk_section_3_body),
    ),
    TncSection(
        iconRes = R.drawable.ic_savings,
        title = stringResource(R.string.buka_rekening_sk_section_4_title),
        body = stringResource(R.string.buka_rekening_sk_section_4_body),
    ),
    TncSection(
        iconRes = R.drawable.ic_lock,
        title = stringResource(R.string.buka_rekening_sk_section_5_title),
        body = stringResource(R.string.buka_rekening_sk_section_5_body),
    ),
)

@Composable
private fun TncSectionItem(section: TncSection) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(section.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
            Text(
                text = section.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.s6),
        )
    }
}

// -- PENTING Notice Box -------------------------------------------------------

@Composable
private fun PentingNoticeBox(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.Primary100, AppShape.R6)
            .padding(Spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .background(MaterialTheme.colorScheme.primary, AppShape.R4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_security),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(AppSize.IconSmall),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.buka_rekening_sk_penting),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.buka_rekening_sk_penting_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// -- Agreement Checkbox -------------------------------------------------------

@Composable
private fun AgreementCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppShape.R6,
        shadowElevation = Spacing.s0,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.R6)
                .clickable { onCheckedChange(!checked) }
                .padding(Spacing.s3),
        ) {
            // Custom checkbox
            val checkboxBg = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s5)
                    .background(checkboxBg, AppShape.R2),
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Spacing.s4),
                    )
                }
            }

            // Agreement text
            val prefix = stringResource(R.string.buka_rekening_sk_checkbox_prefix)
            val link = stringResource(R.string.buka_rekening_sk_checkbox_link)
            val suffix = stringResource(R.string.buka_rekening_sk_checkbox_suffix)
            Text(
                text = buildAnnotatedString {
                    append(prefix)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(link)
                    }
                    append(suffix)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -- Agree Button -------------------------------------------------------------

@Composable
private fun AgreeButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShape.R6,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.s9),
    ) {
        Text(
            text = stringResource(R.string.buka_rekening_sk_setuju),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.size(Spacing.s2))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(AppSize.IconSmall),
        )
    }
}

// -- Previews ------------------------------------------------------------------

@Preview(showBackground = true, name = "Syarat & Ketentuan")
@Composable
private fun BukaRekeningSyaratKetentuanPreview() {
    BcaMobileTheme {
        BukaRekeningSyaratKetentuanScreen(
            onAgreeClick = {},
            onBackClick = {},
        )
    }
}