package smart.study.planner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLightVariant,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color.White,
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = AccentTeal.copy(alpha = 0.2f),
    onSecondaryContainer = AccentTeal,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreen.copy(alpha = 0.2f),
    onTertiaryContainer = AccentGreen,
    error = ErrorLight,
    onError = ErrorRed,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    background = NeutralGray900,
    onBackground = NeutralGray50,
    surface = NeutralGray900,
    onSurface = NeutralGray50,
    surfaceVariant = NeutralGray700,
    onSurfaceVariant = NeutralGray300,
    outline = NeutralGray500,
    outlineVariant = NeutralGray700
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLightVariant.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = AccentTeal.copy(alpha = 0.1f),
    onSecondaryContainer = AccentTeal,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreen.copy(alpha = 0.1f),
    onTertiaryContainer = AccentGreen,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorRed,
    background = NeutralGray50,
    onBackground = NeutralGray900,
    surface = Color.White,
    onSurface = NeutralGray900,
    surfaceVariant = NeutralGray100,
    onSurfaceVariant = NeutralGray700,
    outline = NeutralGray500,
    outlineVariant = NeutralGray300
)

@Composable
fun LTMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}