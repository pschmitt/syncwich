package dev.pschmitt.syncwich.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = SyncwichTerracotta40,
        secondary = SyncwichBasil40,
        tertiary = SyncwichSaffron40,
        error = SyncwichError40,
    )

private val DarkColors =
    darkColorScheme(
        primary = SyncwichTerracotta80,
        secondary = SyncwichBasil80,
        tertiary = SyncwichSaffron80,
        error = SyncwichError80,
    )

/**
 * Full Material You theming: dynamic, wallpaper-derived color on Android 12+, falling back to a
 * warm, food-inspired hand-picked palette on older devices.
 */
@Composable
fun SyncwichTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
