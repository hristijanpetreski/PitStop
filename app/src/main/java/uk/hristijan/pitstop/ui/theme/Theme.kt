package uk.hristijan.pitstop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrgDarkPrimary,
    onPrimary = BrgDarkOnPrimary,
    primaryContainer = BrgDarkPrimaryContainer,
    onPrimaryContainer = BrgDarkOnPrimaryContainer,
    secondary = BrgDarkSecondary,
    onSecondary = BrgDarkOnSecondary,
    secondaryContainer = BrgDarkSecondaryContainer,
    onSecondaryContainer = BrgDarkOnSecondaryContainer,
    tertiary = BrgDarkTertiary,
    onTertiary = BrgDarkOnTertiary,
    tertiaryContainer = BrgDarkTertiaryContainer,
    onTertiaryContainer = BrgDarkOnTertiaryContainer,
    error = BrgDarkError,
    onError = BrgDarkOnError,
    errorContainer = BrgDarkErrorContainer,
    onErrorContainer = BrgDarkOnErrorContainer,
    background = BrgDarkBackground,
    onBackground = BrgDarkOnBackground,
    surface = BrgDarkSurface,
    onSurface = BrgDarkOnSurface,
    surfaceVariant = BrgDarkSurfaceVariant,
    onSurfaceVariant = BrgDarkOnSurfaceVariant,
    outline = BrgDarkOutline,
    outlineVariant = BrgDarkOutlineVariant,
    surfaceTint = BrgDarkSurfaceTint,
    surfaceBright = BrgDarkSurfaceBright,
    surfaceDim = BrgDarkSurfaceDim,
    surfaceContainer = BrgDarkSurfaceContainer,
    surfaceContainerLow = BrgDarkSurfaceContainerLow,
    surfaceContainerLowest = BrgDarkSurfaceContainerLowest,
    surfaceContainerHigh = BrgDarkSurfaceContainerHigh,
    surfaceContainerHighest = BrgDarkSurfaceContainerHighest,
    inverseSurface = BrgDarkInverseSurface,
    inverseOnSurface = BrgDarkInverseOnSurface,
    inversePrimary = BrgDarkInversePrimary,
    scrim = BrgDarkScrim,
)

private val LightColorScheme = lightColorScheme(
    primary = BrgLightPrimary,
    onPrimary = BrgLightOnPrimary,
    primaryContainer = BrgLightPrimaryContainer,
    onPrimaryContainer = BrgLightOnPrimaryContainer,
    secondary = BrgLightSecondary,
    onSecondary = BrgLightOnSecondary,
    secondaryContainer = BrgLightSecondaryContainer,
    onSecondaryContainer = BrgLightOnSecondaryContainer,
    tertiary = BrgLightTertiary,
    onTertiary = BrgLightOnTertiary,
    tertiaryContainer = BrgLightTertiaryContainer,
    onTertiaryContainer = BrgLightOnTertiaryContainer,
    error = BrgLightError,
    onError = BrgLightOnError,
    errorContainer = BrgLightErrorContainer,
    onErrorContainer = BrgLightOnErrorContainer,
    background = BrgLightBackground,
    onBackground = BrgLightOnBackground,
    surface = BrgLightSurface,
    onSurface = BrgLightOnSurface,
    surfaceVariant = BrgLightSurfaceVariant,
    onSurfaceVariant = BrgLightOnSurfaceVariant,
    outline = BrgLightOutline,
    outlineVariant = BrgLightOutlineVariant,
    surfaceTint = BrgLightSurfaceTint,
    surfaceBright = BrgLightSurfaceBright,
    surfaceDim = BrgLightSurfaceDim,
    surfaceContainer = BrgLightSurfaceContainer,
    surfaceContainerLow = BrgLightSurfaceContainerLow,
    surfaceContainerLowest = BrgLightSurfaceContainerLowest,
    surfaceContainerHigh = BrgLightSurfaceContainerHigh,
    surfaceContainerHighest = BrgLightSurfaceContainerHighest,
    inverseSurface = BrgLightInverseSurface,
    inverseOnSurface = BrgLightInverseOnSurface,
    inversePrimary = BrgLightInversePrimary,
    scrim = BrgLightScrim,
)

@Composable
fun PitStopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

