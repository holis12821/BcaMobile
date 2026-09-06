package id.bca.bcamobile.ui.screen.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State ───────────────────────────────────────────────────────────────

data class TransferAntarRekeningUiState(
    val currentStep: Int = 1,
    val sourceAccountType: String = "",
    val sourceAccountNumber: String = "",
    val sourceBalance: String = "",
    val destinationAccount: String = "",
    val amount: String = "",
    val notes: String = "",
    val isLanjutEnabled: Boolean = false,
)

// ── Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferAntarRekeningScreen(
    state: TransferAntarRekeningUiState,
    onBackClick: () -> Unit,
    onSourceAccountClick: () -> Unit,
    onDestinationAccountChange: (String) -> Unit,
    onContactsClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onLanjutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.transfer_detail_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_transfer_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surfaceBright.copy(alpha = AppAlpha.A90),
                                MaterialTheme.colorScheme.surfaceBright,
                            ),
                        )
                    )
                    .padding(top = Spacing.s8),
            ) {
                Button(
                    onClick = onLanjutClick,
                    enabled = state.isLanjutEnabled,
                    shape = AppShape.Full,
                    contentPadding = PaddingValues(
                        horizontal = Spacing.s6,
                        vertical = Spacing.s4,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.s4)
                        .padding(bottom = Spacing.s4),
                ) {
                    Text(
                        text = stringResource(R.string.transfer_lanjut),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(Spacing.s2))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(AppSize.IconSmall),
                    )
                }
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            StepIndicator(currentStep = state.currentStep)

            Spacer(Modifier.height(Spacing.s2))

            // Dari Rekening
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionLabel(stringResource(R.string.transfer_dari_rekening))
                SourceAccountCard(
                    accountType = state.sourceAccountType,
                    accountNumber = state.sourceAccountNumber,
                    balance = state.sourceBalance,
                    onClick = onSourceAccountClick,
                )
            }

            // Ke Rekening
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionLabel(stringResource(R.string.transfer_ke_rekening))
                TextField(
                    value = state.destinationAccount,
                    onValueChange = onDestinationAccountChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.transfer_nomor_tujuan_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.A50),
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onContactsClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_contacts),
                                contentDescription = stringResource(R.string.cd_transfer_kontak),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    singleLine = true,
                    shape = AppShape.R6,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Jumlah
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionLabel(stringResource(R.string.transfer_jumlah))
                TextField(
                    value = state.amount,
                    onValueChange = onAmountChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.transfer_amount_placeholder),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.A50),
                        )
                    },
                    prefix = {
                        Text(
                            text = stringResource(R.string.transfer_rp_prefix),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    shape = AppShape.R6,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Catatan (Opsional)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionLabel(stringResource(R.string.transfer_catatan_label))
                TextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.transfer_catatan_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.A50),
                        )
                    },
                    singleLine = true,
                    shape = AppShape.R6,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Step Indicator ──────────────────────────────────────────────────────

@Composable
private fun StepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s4),
    ) {
        // Step 1
        StepCircle(
            number = 1,
            label = stringResource(R.string.transfer_step_input),
            isActive = currentStep >= 1,
        )

        // Connecting line
        HorizontalDivider(
            color = if (currentStep > 1) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            thickness = StrokeWidth.w0,
            modifier = Modifier.weight(1f),
        )

        // Step 2
        StepCircle(
            number = 2,
            label = stringResource(R.string.transfer_step_konfirmasi),
            isActive = currentStep >= 2,
        )
    }
}

@Composable
private fun StepCircle(
    number: Int,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.alpha(if (isActive) 1f else AppAlpha.A50),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s7)
                .clip(AppShape.Full)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Section Label ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = Spacing.s1),
    )
}

// ── Source Account Card ─────────────────────────────────────────────────

@Composable
private fun SourceAccountCard(
    accountType: String,
    accountNumber: String,
    balance: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.R6,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.s4),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = accountType,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = accountNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Spacing.s1),
                ) {
                    Text(
                        text = stringResource(R.string.transfer_saldo_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = balance,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.cd_transfer_pilih_rekening),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Transfer Antar Rekening - Default")
@Composable
private fun TransferAntarRekeningPreview() {
    BcaMobileTheme {
        TransferAntarRekeningScreen(
            state = TransferAntarRekeningUiState(
                currentStep = 1,
                sourceAccountType = "Tahapan BCA",
                sourceAccountNumber = "1234567890",
                sourceBalance = "Rp 12.500.000",
            ),
            onBackClick = {},
            onSourceAccountClick = {},
            onDestinationAccountChange = {},
            onContactsClick = {},
            onAmountChange = {},
            onNotesChange = {},
            onLanjutClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Transfer Antar Rekening - Filled")
@Composable
private fun TransferAntarRekeningFilledPreview() {
    BcaMobileTheme {
        TransferAntarRekeningScreen(
            state = TransferAntarRekeningUiState(
                currentStep = 1,
                sourceAccountType = "Tahapan BCA",
                sourceAccountNumber = "1234567890",
                sourceBalance = "Rp 12.500.000",
                destinationAccount = "0987654321",
                amount = "500000",
                notes = "Bayar Makan Siang",
                isLanjutEnabled = true,
            ),
            onBackClick = {},
            onSourceAccountClick = {},
            onDestinationAccountChange = {},
            onContactsClick = {},
            onAmountChange = {},
            onNotesChange = {},
            onLanjutClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Transfer Antar Rekening - Dark")
@Composable
private fun TransferAntarRekeningDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        TransferAntarRekeningScreen(
            state = TransferAntarRekeningUiState(
                currentStep = 1,
                sourceAccountType = "Tahapan BCA",
                sourceAccountNumber = "1234567890",
                sourceBalance = "Rp 12.500.000",
            ),
            onBackClick = {},
            onSourceAccountClick = {},
            onDestinationAccountChange = {},
            onContactsClick = {},
            onAmountChange = {},
            onNotesChange = {},
            onLanjutClick = {},
        )
    }
}