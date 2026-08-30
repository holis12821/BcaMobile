package id.bca.bcamobile.ui.screen.ewallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Models ─────────────────────────────────────────────────────────

data class EWalletOption(
    val id: String,
    val name: String,
    val brandColor: Color,
)

data class PresetAmount(
    val value: Long,
    val label: String,
)

data class TopUpEWalletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountName: String = "",
    val accountNumber: String = "",
    val balance: String = "",
    val walletOptions: List<EWalletOption> = emptyList(),
    val selectedWalletId: String? = null,
    val phoneNumber: String = "",
    val presetAmounts: List<PresetAmount> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val manualAmount: String = "",
    val isFormValid: Boolean = false,
    val insufficientBalance: Boolean = false,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpEWalletScreen(
    state: TopUpEWalletUiState,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onWalletSelected: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onContactPickerClick: () -> Unit,
    onPresetAmountSelected: (Int) -> Unit,
    onManualAmountChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ewallet_topup_title),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_ewallet_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Button(
                    onClick = onContinueClick,
                    enabled = state.isFormValid && !state.isLoading,
                    shape = AppShape.R6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.s4),
                ) {
                    Text(
                        text = stringResource(R.string.ewallet_lanjut),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(Spacing.s2))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.s5),
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.s4),
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.s4))
                    TextButton(onClick = onRetry) {
                        Text(
                            text = stringResource(R.string.ewallet_coba_lagi),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    AccountSourceSection(
                        accountName = state.accountName,
                        accountNumber = state.accountNumber,
                        balance = state.balance,
                        onClick = onAccountClick,
                    )
                    SectionDivider()
                    EWalletSelectionSection(
                        walletOptions = state.walletOptions,
                        selectedWalletId = state.selectedWalletId,
                        phoneNumber = state.phoneNumber,
                        onWalletSelected = onWalletSelected,
                        onPhoneNumberChanged = onPhoneNumberChanged,
                        onContactPickerClick = onContactPickerClick,
                    )
                    SectionDivider()
                    AmountSection(
                        presetAmounts = state.presetAmounts,
                        selectedPresetIndex = state.selectedPresetIndex,
                        manualAmount = state.manualAmount,
                        insufficientBalance = state.insufficientBalance,
                        onPresetSelected = onPresetAmountSelected,
                        onManualAmountChanged = onManualAmountChanged,
                    )
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.s2)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun AccountSourceSection(
    accountName: String,
    accountNumber: String,
    balance: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.ewallet_sumber_rekening),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.s3))
        OutlinedCard(
            onClick = onClick,
            shape = AppShape.R6,
            border = BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Spacing.s8)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer
                                    .copy(alpha = AppAlpha.A20),
                                shape = AppShape.Full,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_wallet),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Spacing.s6),
                        )
                    }
                    Column {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = accountNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.ewallet_saldo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = balance,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_ewallet_pilih_rekening),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .padding(top = Spacing.s1)
                            .size(Spacing.s5),
                    )
                }
            }
        }
    }
}

@Composable
private fun EWalletSelectionSection(
    walletOptions: List<EWalletOption>,
    selectedWalletId: String?,
    phoneNumber: String,
    onWalletSelected: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onContactPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.ewallet_pilih_ewallet),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.s4))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            walletOptions.chunked(4).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowItems.forEach { wallet ->
                        EWalletItem(
                            wallet = wallet,
                            isSelected = wallet.id == selectedWalletId,
                            onClick = { onWalletSelected(wallet.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.s6))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChanged,
            label = {
                Text(stringResource(R.string.ewallet_nomor_handphone))
            },
            prefix = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.ewallet_phone_prefix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = Spacing.s2)
                            .height(Spacing.s6),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            },
            trailingIcon = {
                IconButton(onClick = onContactPickerClick) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_ewallet_pilih_kontak),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = AppShape.R6,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EWalletItem(
    wallet: EWalletOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s10)
                .background(MaterialTheme.colorScheme.surface, AppShape.Full)
                .border(
                    width = if (isSelected) StrokeWidth.w1 else StrokeWidth.w0,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = AppShape.Full,
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.s7)
                    .background(wallet.brandColor, AppShape.Full),
            )
        }
        Text(
            text = wallet.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AmountSection(
    presetAmounts: List<PresetAmount>,
    selectedPresetIndex: Int,
    manualAmount: String,
    insufficientBalance: Boolean,
    onPresetSelected: (Int) -> Unit,
    onManualAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = Spacing.s6)) {
        Text(
            text = stringResource(R.string.ewallet_nominal_topup),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s4),
        )
        Spacer(Modifier.height(Spacing.s4))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4),
        ) {
            presetAmounts.forEachIndexed { index, amount ->
                AmountChip(
                    label = amount.label,
                    isSelected = index == selectedPresetIndex,
                    onClick = { onPresetSelected(index) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.s6))

        Column(modifier = Modifier.padding(horizontal = Spacing.s4)) {
            OutlinedTextField(
                value = manualAmount,
                onValueChange = onManualAmountChanged,
                label = {
                    Text(stringResource(R.string.ewallet_manual_amount_label))
                },
                prefix = {
                    Text(
                        text = stringResource(R.string.ewallet_rp_prefix),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = insufficientBalance,
                supportingText = {
                    Text(
                        text = if (insufficientBalance) {
                            stringResource(R.string.ewallet_saldo_tidak_cukup)
                        } else {
                            stringResource(R.string.ewallet_min_topup_info)
                        },
                    )
                },
                shape = AppShape.R6,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppShape.Full,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) {
            null
        } else {
            BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = Spacing.s6, vertical = Spacing.s3),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

private val sampleWallets = listOf(
    EWalletOption("gopay", "Gopay", AppColor.Primary500),
    EWalletOption("ovo", "OVO", AppColor.Secondary1000),
    EWalletOption("dana", "Dana", AppColor.Primary600),
    EWalletOption("linkaja", "LinkAja", AppColor.Danger500),
    EWalletOption("shopeepay", "ShopeePay", AppColor.Danger400),
)

private val samplePresets = listOf(
    PresetAmount(50_000, "Rp 50.000"),
    PresetAmount(100_000, "Rp 100.000"),
    PresetAmount(200_000, "Rp 200.000"),
    PresetAmount(500_000, "Rp 500.000"),
)

private val previewState = TopUpEWalletUiState(
    accountName = "Tahapan BCA",
    accountNumber = "1234 5678 90",
    balance = "Rp 12.500.000",
    walletOptions = sampleWallets,
    selectedWalletId = "gopay",
    phoneNumber = "",
    presetAmounts = samplePresets,
    selectedPresetIndex = 1,
    manualAmount = "100.000",
    isFormValid = true,
)

@Preview(showBackground = true, name = "Light - Success")
@Composable
private fun TopUpEWalletScreenPreview() {
    BcaMobileTheme {
        TopUpEWalletScreen(
            state = previewState,
            onBackClick = {},
            onAccountClick = {},
            onWalletSelected = {},
            onPhoneNumberChanged = {},
            onContactPickerClick = {},
            onPresetAmountSelected = {},
            onManualAmountChanged = {},
            onContinueClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark - Success")
@Composable
private fun TopUpEWalletScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        TopUpEWalletScreen(
            state = previewState,
            onBackClick = {},
            onAccountClick = {},
            onWalletSelected = {},
            onPhoneNumberChanged = {},
            onContactPickerClick = {},
            onPresetAmountSelected = {},
            onManualAmountChanged = {},
            onContinueClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun TopUpEWalletScreenLoadingPreview() {
    BcaMobileTheme {
        TopUpEWalletScreen(
            state = TopUpEWalletUiState(isLoading = true),
            onBackClick = {},
            onAccountClick = {},
            onWalletSelected = {},
            onPhoneNumberChanged = {},
            onContactPickerClick = {},
            onPresetAmountSelected = {},
            onManualAmountChanged = {},
            onContinueClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun TopUpEWalletScreenErrorPreview() {
    BcaMobileTheme {
        TopUpEWalletScreen(
            state = TopUpEWalletUiState(
                error = stringResource(R.string.error_general_retry),
            ),
            onBackClick = {},
            onAccountClick = {},
            onWalletSelected = {},
            onPhoneNumberChanged = {},
            onContactPickerClick = {},
            onPresetAmountSelected = {},
            onManualAmountChanged = {},
            onContinueClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Insufficient Balance")
@Composable
private fun TopUpEWalletInsufficientBalancePreview() {
    BcaMobileTheme {
        TopUpEWalletScreen(
            state = previewState.copy(
                insufficientBalance = true,
                manualAmount = "15.000.000",
            ),
            onBackClick = {},
            onAccountClick = {},
            onWalletSelected = {},
            onPhoneNumberChanged = {},
            onContactPickerClick = {},
            onPresetAmountSelected = {},
            onManualAmountChanged = {},
            onContinueClick = {},
            onRetry = {},
        )
    }
}