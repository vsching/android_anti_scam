/**
 * SafeAnotTheme composable providing Material 3 dark color scheme for the entire app.
 */
package com.safeanot.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SafeAnotDarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = DarkBackground,
    primaryContainer = BlueAccent.copy(alpha = 0.15f),
    onPrimaryContainer = BlueAccent,
    secondary = PurpleAccent,
    onSecondary = DarkBackground,
    secondaryContainer = PurpleAccent.copy(alpha = 0.15f),
    onSecondaryContainer = PurpleAccent,
    tertiary = GreenAccent,
    onTertiary = DarkBackground,
    tertiaryContainer = GreenAccent.copy(alpha = 0.15f),
    onTertiaryContainer = GreenAccent,
    error = RedAccent,
    onError = DarkBackground,
    errorContainer = RedAccent.copy(alpha = 0.15f),
    onErrorContainer = RedAccent,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = TextTertiary,
)

@Composable
fun SafeAnotTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkSurface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SafeAnotDarkColorScheme,
        typography = SafeAnotTypography,
        content = content,
    )
}
