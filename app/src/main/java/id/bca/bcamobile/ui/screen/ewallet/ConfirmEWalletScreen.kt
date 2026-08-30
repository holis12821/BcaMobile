package id.bca.bcamobile.ui.screen.ewallet

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

// ── State Model ──────────────────────────────────────────────────────────

data class ConfirmEWalletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val destinationName: String = "",
    val destinationProvider: String = "",
    val destinationPhone: String = "",
    val sourceAccount: String = "",
    val nominalAmount: String = "",
    val adminFee: String = "",
    val totalAmount: String = "",
)

// ── Main Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmEWalletScreen(
    state: ConfirmEWalletUiState,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ewallet_confirm_title),
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(Spacing.s4),
            ) {
                Button(
                    onClick = onConfirmClick,
                    enabled = !state.isLoading,
                    shape = AppShape.R6,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.ewallet_konfirmasi),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(Spacing.s2))
                    Icon(
                        imageVector = Icons.Default.Check,
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
                    verticalArrangement = Arrangement.spacedBy(Spacing.s6),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.s4),
                ) {
                    ConfirmationHeader()
                    TransactionDetailsCard(
                        destinationName = state.destinationName,
                        destinationProvider = state.destinationProvider,
                        destinationPhone = state.destinationPhone,
                        sourceAccount = state.sourceAccount,
                        nominalAmount = state.nominalAmount,
                        adminFee = state.adminFee,
                        totalAmount = state.totalAmount,
                    )
                    WarningNote()
                }
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun ConfirmationHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s7, bottom = Spacing.s4),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppSize.BiometricButton)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    AppShape.Full,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wallet),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.s7),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        ) {
            Text(
                text = stringResource(R.string.ewallet_konfirmasi_topup),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.ewallet_pastikan_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionDetailsCard(
    destinationName: String,
    destinationProvider: String,
    destinationPhone: String,
    sourceAccount: String,
    nominalAmount: String,
    adminFee: String,
    totalAmount: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        shape = AppShape.R6,
        border = BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Target info header
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant
                        .copy(alpha = AppAlpha.A50),
                )
                .padding(Spacing.s4),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Spacing.s8)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        AppShape.Full,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.s5),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.ewallet_tujuan_ewallet),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.ewallet_destination_format,
                        destinationName,
                        destinationProvider,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = destinationPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Amount breakdown
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.padding(Spacing.s4),
        ) {
            DetailRow(
                label = stringResource(R.string.ewallet_sumber_rekening_label),
                value = sourceAccount,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow(
                label = stringResource(R.string.ewallet_nominal_topup_label),
                value = nominalAmount,
            )
            DetailRow(
                label = stringResource(R.string.ewallet_biaya_admin),
                value = adminFee,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.s2),
            ) {
                Text(
                    text = stringResource(R.string.ewallet_total),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = totalAmount,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WarningNote(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                AppShape.R6,
            )
            .padding(Spacing.s4),
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = Spacing.s0)
                .size(Spacing.s5),
        )
        Text(
            text = stringResource(R.string.ewallet_warning_irreversible),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

private val previewState = ConfirmEWalletUiState(
    destinationName = "Dafa",
    destinationProvider = "DANA",
    destinationPhone = "081234567890",
    sourceAccount = "1234 5678",
    nominalAmount = "Rp 50.000",
    adminFee = "Rp 0",
    totalAmount = "Rp 50.000",
)

@Preview(showBackground = true, name = "Light - Success")
@Composable
private fun ConfirmEWalletScreenPreview() {
    BcaMobileTheme {
        ConfirmEWalletScreen(
            state = previewState,
            onBackClick = {},
            onConfirmClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark - Success")
@Composable
private fun ConfirmEWalletScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        ConfirmEWalletScreen(
            state = previewState,
            onBackClick = {},
            onConfirmClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun ConfirmEWalletScreenLoadingPreview() {
    BcaMobileTheme {
        ConfirmEWalletScreen(
            state = ConfirmEWalletUiState(isLoading = true),
            onBackClick = {},
            onConfirmClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ConfirmEWalletScreenErrorPreview() {
    BcaMobileTheme {
        ConfirmEWalletScreen(
            state = ConfirmEWalletUiState(
                error = stringResource(R.string.error_general_retry),
            ),
            onBackClick = {},
            onConfirmClick = {},
            onRetry = {},
        )
    }
}