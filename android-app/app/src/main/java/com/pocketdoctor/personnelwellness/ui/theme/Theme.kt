package com.pocketdoctor.personnelwellness.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CoolBlueLight,
    onPrimary = NavyBackgroundDark,
    primaryContainer = NavySurfaceDark,
    onPrimaryContainer = ElectricCyan,
    secondary = CoolBlue,
    onSecondary = Color.White,
    tertiary = ElectricCyan,
    background = NavyBackgroundDark,
    onBackground = GlassTextPrimaryDark,
    surface = NavySurfaceDark,
    onSurface = GlassTextPrimaryDark,
    surfaceVariant = NavyCardDark,
    onSurfaceVariant = GlassTextSecondaryDark,
    outline = NavyCardBorderDark,
    error = HighRiskRed,
    errorContainer = HighRiskBg,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CoolBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = CoolBlue,
    secondary = CoolBlueLight,
    onSecondary = NavyBackgroundDark,
    tertiary = ElectricCyan,
    background = LightBackground,
    onBackground = GlassTextPrimaryLight,
    surface = LightSurface,
    onSurface = GlassTextPrimaryLight,
    surfaceVariant = LightCardGlass,
    onSurfaceVariant = GlassTextSecondaryLight,
    outline = LightCardBorder,
    error = HighRiskRed,
    errorContainer = HighRiskBg,
    onError = Color.White
)

@Composable
fun PersonnelWellnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
