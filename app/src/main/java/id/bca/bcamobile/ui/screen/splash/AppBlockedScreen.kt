package id.bca.bcamobile.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppShape
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

/**
 * Layar penutup saat server menyatakan aplikasi tidak boleh dipakai:
 * `maintenance_mode` atau `force_update` dari `GET /health`.
 *
 * Sengaja tanpa jalan pintas melanjutkan — keputusannya milik server. Yang ada
 * hanya satu aksi: coba lagi (pemeliharaan) atau perbarui aplikasi.
 */
@Composable
fun AppBlockedScreen(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.s6),
        ) {
            Image(
                painter = painterResource(R.drawable.bca_logo_white_transparent),
                contentDescription = stringResource(R.string.cd_bca_logo),
                modifier = Modifier.size(AppSize.LogoContainer),
            )

            Spacer(Modifier.height(Spacing.s7))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.s3))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.s7))

            Button(
                onClick = onAction,
                shape = AppShape.R6,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppSize.MinTouchTarget),
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true, name = "Pemeliharaan")
@Composable
private fun AppBlockedMaintenancePreview() {
    BcaMobileTheme {
        AppBlockedScreen(
            title = stringResource(R.string.gate_maintenance_title),
            message = stringResource(R.string.gate_maintenance_default_message),
            actionLabel = stringResource(R.string.gate_coba_lagi),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Wajib perbarui")
@Composable
private fun AppBlockedUpdatePreview() {
    BcaMobileTheme {
        AppBlockedScreen(
            title = stringResource(R.string.gate_update_title),
            message = stringResource(R.string.gate_update_message, "5.9.0"),
            actionLabel = stringResource(R.string.gate_perbarui),
            onAction = {},
        )
    }
}
