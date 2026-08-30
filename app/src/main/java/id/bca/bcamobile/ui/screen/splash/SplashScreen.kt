package id.bca.bcamobile.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.bca.bcamobile.R
import id.bca.bcamobile.ui.theme.AppAlpha
import id.bca.bcamobile.ui.theme.AppColor
import id.bca.bcamobile.ui.theme.AppSize
import id.bca.bcamobile.ui.theme.BcaMobileTheme
import id.bca.bcamobile.ui.theme.Spacing

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        // Centered logo
        Image(
            painter = painterResource(R.drawable.bca_logo_white),
            contentDescription = stringResource(R.string.cd_bca_logo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(AppSize.SplashLogo)
                .align(Alignment.Center),
        )

        // Bottom branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = Spacing.s4)
                .padding(bottom = Spacing.s9),
        ) {
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.headlineMedium,
                color = AppColor.Neutral100,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.s2))

            Text(
                text = stringResource(R.string.splash_copyright).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColor.Neutral100.copy(alpha = AppAlpha.A60),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Splash - Light")
@Composable
private fun SplashScreenPreview() {
    BcaMobileTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true, name = "Splash - Dark")
@Composable
private fun SplashScreenDarkPreview() {
    BcaMobileTheme(darkTheme = true) {
        SplashScreen()
    }
}