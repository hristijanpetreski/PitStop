package uk.hristijan.pitstop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RacingRedDark,
    secondary = Silver,
    tertiary = FuelGreen,
    background = NightSurface,
    surface = Asphalt,
    onPrimary = Asphalt,
    onBackground = GarageWhite,
    onSurface = GarageWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = RacingRed,
    secondary = Graphite,
    tertiary = FuelGreen,
    background = GarageWhite,
    surface = GarageWhite,
    onPrimary = GarageWhite,
    onBackground = Asphalt,
    onSurface = Asphalt,
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
