package id.bca.bcamobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColor.Primary700,
    onPrimary = AppColor.Neutral100,
    primaryContainer = AppColor.Primary100,
    onPrimaryContainer = AppColor.Primary1000,
    secondary = AppColor.Secondary700,
    onSecondary = AppColor.Neutral100,
    secondaryContainer = AppColor.Secondary100,
    onSecondaryContainer = AppColor.Secondary1000,
    error = AppColor.Danger600,
    onError = AppColor.Neutral100,
    errorContainer = AppColor.Danger100,
    onErrorContainer = AppColor.Danger1000,
    background = AppColor.Neutral100,
    onBackground = AppColor.Neutral1000,
    surface = AppColor.Neutral100,
    onSurface = AppColor.Neutral1000,
    surfaceVariant = AppColor.Neutral200,
    onSurfaceVariant = AppColor.Neutral800,
    outline = AppColor.Neutral400,
    outlineVariant = AppColor.Neutral300,
)

@Composable
fun BcaMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Skema gelap belum didefinisikan desain — kunci ke terang.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}