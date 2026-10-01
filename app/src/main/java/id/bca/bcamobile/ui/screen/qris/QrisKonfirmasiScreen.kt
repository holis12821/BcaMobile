package id.bca.bcamobile.ui.screen.qris

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.components.AppTopBar
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

data class QrisKonfirmasiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val merchantName: String = "",
    val merchantCity: String = "",
    val sourceAccount: String = "",
    val sourceBalance: String = "",
    /** QR statis: nasabah mengisi nominal. QR dinamis: nominal terkunci dari server. */
    val isAmountEditable: Boolean = false,
    val amountInput: String = "",
    val amount: String = "",
    val adminFee: String = "",
    val total: String = "",
    val isInsufficientBalance: Boolean = false,
    val isPayEnabled: Boolean = false,
)

/**
 * Konfirmasi pembayaran QRIS sebelum PIN.
 *
 * **Catatan desain:** layar ini tidak ada di artefak Stitch — yang tersedia hanya
 * `Scan QRIS`. Susunannya mengikuti `ConfirmEWalletScreen` yang sudah ada supaya
 * konfirmasi transaksi terasa sama di seluruh aplikasi. Perlu direview pemilik desain.
 */
@Composable
fun QrisKonfirmasiScreen(
    state: QrisKonfirmasiUiState,
    onAmountChanged: (String) -> Unit,
    onPayClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.qris_konfirmasi_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
        ) {
            MerchantCard(name = state.merchantName, city = state.merchantCity)

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R6)
                    .padding(Spacing.s4),
            ) {
                DetailRow(
                    label = stringResource(R.string.qris_sumber_rekening),
                    value = state.sourceAccount,
                )
                DetailRow(
                    label = stringResource(R.string.qris_saldo),
                    value = state.sourceBalance,
                )

                if (state.isAmountEditable) {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = onAmountChanged,
                        label = { Text(stringResource(R.string.qris_nominal)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.isInsufficientBalance,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DetailRow(
                        label = stringResource(R.string.qris_nominal),
                        value = state.amount,
                    )
                }

                DetailRow(
                    label = stringResource(R.string.qris_biaya_admin),
                    value = state.adminFee,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                DetailRow(
                    label = stringResource(R.string.qris_total),
                    value = state.total,
                )
            }

            if (state.isInsufficientBalance) {
                Text(
                    text = stringResource(R.string.qris_error_saldo_kurang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(Spacing.s2))

            Button(
                onClick = onPayClick,
                enabled = state.isPayEnabled && !state.isLoading,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppSize.MinTouchTarget),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(AppSize.Icon20),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.qris_bayar),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantCard(
    name: String,
    city: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShape.R6)
            .padding(Spacing.s5),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.s10)
                .background(MaterialTheme.colorScheme.secondaryFixed, AppShape.Full),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_qris),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppSize.Icon32),
            )
        }

        Spacer(Modifier.height(Spacing.s3))

        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (city.isNotBlank()) {
            Spacer(Modifier.height(Spacing.s0))
            Text(
                text = city,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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

private val previewState = QrisKonfirmasiUiState(
    merchantName = "TOKO SEJAHTERA",
    merchantCity = "JAKARTA",
    sourceAccount = "TAHAPAN BCA (1234567890)",
    sourceBalance = "Rp50.000.000",
    amount = "Rp50.000",
    adminFee = "Rp0",
    total = "Rp50.000",
    isPayEnabled = true,
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun QrisKonfirmasiScreenPreview() {
    BcaMobileTheme {
        QrisKonfirmasiScreen(
            state = previewState,
            onAmountChanged = {},
            onPayClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun QrisKonfirmasiScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        QrisKonfirmasiScreen(
            state = previewState,
            onAmountChanged = {},
            onPayClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "QR statis + saldo kurang")
@Composable
private fun QrisKonfirmasiScreenEditablePreview() {
    BcaMobileTheme {
        QrisKonfirmasiScreen(
            state = previewState.copy(
                isAmountEditable = true,
                amountInput = "99000000",
                isInsufficientBalance = true,
                isPayEnabled = false,
            ),
            onAmountChanged = {},
            onPayClick = {},
            onBackClick = {},
        )
    }
}
