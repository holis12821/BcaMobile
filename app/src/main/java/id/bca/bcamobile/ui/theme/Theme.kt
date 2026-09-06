package id.bca.bcamobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = M3Color.Primary,
    onPrimary = M3Color.OnPrimary,
    primaryContainer = M3Color.PrimaryContainer,
    onPrimaryContainer = M3Color.OnPrimaryContainer,
    inversePrimary = M3Color.InversePrimary,
    secondary = M3Color.Secondary,
    onSecondary = M3Color.OnSecondary,
    secondaryContainer = M3Color.SecondaryContainer,
    onSecondaryContainer = M3Color.OnSecondaryContainer,
    tertiary = M3Color.Tertiary,
    onTertiary = M3Color.OnTertiary,
    tertiaryContainer = M3Color.TertiaryContainer,
    onTertiaryContainer = M3Color.OnTertiaryContainer,
    error = M3Color.Error,
    onError = M3Color.OnError,
    errorContainer = M3Color.ErrorContainer,
    onErrorContainer = M3Color.OnErrorContainer,
    background = M3Color.Background,
    onBackground = M3Color.OnBackground,
    surface = M3Color.Surface,
    onSurface = M3Color.OnSurface,
    surfaceVariant = M3Color.SurfaceVariant,
    onSurfaceVariant = M3Color.OnSurfaceVariant,
    surfaceTint = M3Color.SurfaceTint,
    inverseSurface = M3Color.InverseSurface,
    inverseOnSurface = M3Color.InverseOnSurface,
    outline = M3Color.Outline,
    outlineVariant = M3Color.OutlineVariant,
    surfaceBright = M3Color.SurfaceBright,
    surfaceDim = M3Color.SurfaceDim,
    surfaceContainer = M3Color.SurfaceContainer,
    surfaceContainerHigh = M3Color.SurfaceContainerHigh,
    surfaceContainerHighest = M3Color.SurfaceContainerHighest,
    surfaceContainerLow = M3Color.SurfaceContainerLow,
    surfaceContainerLowest = M3Color.SurfaceContainerLowest,
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