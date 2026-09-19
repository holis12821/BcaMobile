package id.bca.bcamobile.ui.screen.buka_rekening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.Spacing

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