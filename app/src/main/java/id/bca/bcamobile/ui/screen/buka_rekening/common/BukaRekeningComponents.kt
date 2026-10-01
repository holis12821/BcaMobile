package id.bca.bcamobile.ui.screen.buka_rekening.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.Spacing
import id.bca.bcamobile.ui.theme.StrokeWidth

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.buka_rekening_langkah_format, currentStep, totalSteps)
                    .uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stepLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(totalSteps) { index ->
                val color = if (index < currentStep) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.s1)
                        .background(color, AppShape.Full),
                )
            }
        }
    }
}

/** Lingkaran pilihan pada kartu — dipakai layar pilih jenis rekening dan pilih kartu. */
@Composable
fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Spacing.s6)
            .background(bgColor, AppShape.Full),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Spacing.s4),
        )
    }
}

/**
 * Enam kotak digit OTP.
 *
 * Kotaknya hanya tampilan — input sebenarnya ditangani satu `BasicTextField`
 * tersembunyi di layar pemanggil, supaya keyboard angka, tempel, dan autofill
 * SMS tetap bekerja seperti field biasa. Tiga keadaan per kotak mengikuti desain:
 * terisi, sedang aktif (kursor berkedip), dan kosong (titik).
 */
@Composable
fun OtpDigitBoxes(
    code: String,
    length: Int,
    modifier: Modifier = Modifier,
    isFocused: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(length) { index ->
            val digit = code.getOrNull(index)
            val isActive = isFocused && index == code.length
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(
                        color = if (digit != null || isActive) {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        shape = AppShape.R6,
                    ),
            ) {
                when {
                    digit != null -> Text(
                        text = digit.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    isActive -> Box(
                        modifier = Modifier
                            .width(StrokeWidth.w1)
                            .height(Spacing.s6)
                            .background(MaterialTheme.colorScheme.primary),
                    )

                    else -> Box(
                        modifier = Modifier
                            .size(Spacing.s2)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                AppShape.Full,
                            ),
                    )
                }
            }
        }
    }
}
