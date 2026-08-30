package id.bca.bcamobile.ui.screen.kode_akses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class KodeAksesUiState(
    val enteredDigits: Int = 0,
    val maxDigits: Int = 6,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)

// ── Keypad Model ─────────────────────────────────────────────────────────

private sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey
    data object Delete : KeypadKey
    data object Empty : KeypadKey
}

private val keypadKeys = listOf(
    KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3),
    KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6),
    KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9),
    KeypadKey.Empty, KeypadKey.Digit(0), KeypadKey.Delete,
)

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun KodeAksesScreen(
    state: KodeAksesUiState,
    onDigitClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onForgotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s8, start = Spacing.s4, end = Spacing.s4),
        ) {
            Text(
                text = stringResource(R.string.kode_akses_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            PinDots(
                enteredCount = state.enteredDigits,
                totalCount = state.maxDigits,
                isError = state.isError,
            )

            if (state.isError && state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            TextButton(onClick = onForgotClick) {
                Text(
                    text = stringResource(R.string.kode_akses_lupa),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Keypad section
        KeypadSection(
            isPinComplete = state.enteredDigits == state.maxDigits,
            onDigitClick = onDigitClick,
            onDeleteClick = onDeleteClick,
            onCancelClick = onCancelClick,
            onSubmitClick = onSubmitClick,
        )
    }
}

// ── Section Composables ──────────────────────────────────────────────────

@Composable
private fun PinDots(
    enteredCount: Int,
    totalCount: Int,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(totalCount) { index ->
            val isFilled = index < enteredCount
            Box(
                modifier = Modifier
                    .size(Spacing.s4)
                    .then(
                        if (isFilled) {
                            Modifier.background(
                                if (isError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                AppShape.Full,
                            )
                        } else {
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    AppShape.Full,
                                )
                                .border(
                                    width = StrokeWidth.w0,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = AppShape.Full,
                                )
                        },
                    ),
            )
        }
    }
}

@Composable
private fun KeypadSection(
    isPinComplete: Boolean,
    onDigitClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keypadShape = RoundedCornerShape(topStart = Spacing.s6, topEnd = Spacing.s6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, keypadShape)
            .padding(Spacing.s6),
    ) {
        // Number grid
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.s8),
        ) {
            keypadKeys.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { key ->
                        when (key) {
                            is KeypadKey.Digit -> NumberButton(
                                digit = key.value,
                                onClick = { onDigitClick(key.value) },
                            )

                            is KeypadKey.Delete -> DeleteButton(
                                onClick = onDeleteClick,
                            )

                            is KeypadKey.Empty -> Spacer(
                                Modifier.size(AppSize.BiometricButton),
                            )
                        }
                    }
                }
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                shape = AppShape.R6,
                border = BorderStroke(StrokeWidth.w0, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(
                    horizontal = Spacing.s6,
                    vertical = Spacing.s4,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.kode_akses_batal),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onSubmitClick,
                enabled = isPinComplete,
                shape = AppShape.R6,
                contentPadding = PaddingValues(
                    horizontal = Spacing.s6,
                    vertical = Spacing.s4,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.kode_akses_lanjut),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NumberButton(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(AppSize.BiometricButton)
            .clip(AppShape.Full)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppAlpha.A50),
            )
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(AppSize.BiometricButton)
            .clip(AppShape.Full)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.cd_kode_akses_hapus),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.s7),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Light - Empty")
@Composable
private fun KodeAksesScreenEmptyPreview() {
    BcaMobileTheme {
        KodeAksesScreen(
            state = KodeAksesUiState(),
            onDigitClick = {},
            onDeleteClick = {},
            onCancelClick = {},
            onSubmitClick = {},
            onForgotClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark - Empty")
@Composable
private fun KodeAksesScreenEmptyDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        KodeAksesScreen(
            state = KodeAksesUiState(),
            onDigitClick = {},
            onDeleteClick = {},
            onCancelClick = {},
            onSubmitClick = {},
            onForgotClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Partial Entry")
@Composable
private fun KodeAksesScreenPartialPreview() {
    BcaMobileTheme {
        KodeAksesScreen(
            state = KodeAksesUiState(enteredDigits = 3),
            onDigitClick = {},
            onDeleteClick = {},
            onCancelClick = {},
            onSubmitClick = {},
            onForgotClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Complete")
@Composable
private fun KodeAksesScreenCompletePreview() {
    BcaMobileTheme {
        KodeAksesScreen(
            state = KodeAksesUiState(enteredDigits = 6),
            onDigitClick = {},
            onDeleteClick = {},
            onCancelClick = {},
            onSubmitClick = {},
            onForgotClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun KodeAksesScreenErrorPreview() {
    BcaMobileTheme {
        KodeAksesScreen(
            state = KodeAksesUiState(
                enteredDigits = 6,
                isError = true,
                errorMessage = "Kode akses salah. Silakan coba lagi.",
            ),
            onDigitClick = {},
            onDeleteClick = {},
            onCancelClick = {},
            onSubmitClick = {},
            onForgotClick = {},
        )
    }
}