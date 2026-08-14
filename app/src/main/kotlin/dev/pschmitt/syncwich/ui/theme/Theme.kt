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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.sanitizeFontScale

internal val LightColors =
    lightColorScheme(
        primary = SyncwichTerracotta40,
        secondary = SyncwichBasil40,
        tertiary = SyncwichSaffron40,
        error = SyncwichError40,
    )

internal val DarkColors =
    darkColorScheme(
        // Keep the fallback accent consistent with the launcher and native splash background.
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
fun SyncwichTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = DEFAULT_FONT_SCALE,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography().scaled(sanitizeFontScale(fontScale)),
        content = content,
    )
}

internal fun Typography.scaled(scale: Float): Typography =
    copy(
        displayLarge = displayLarge.scaled(scale),
        displayMedium = displayMedium.scaled(scale),
        displaySmall = displaySmall.scaled(scale),
        headlineLarge = headlineLarge.scaled(scale),
        headlineMedium = headlineMedium.scaled(scale),
        headlineSmall = headlineSmall.scaled(scale),
        titleLarge = titleLarge.scaled(scale),
        titleMedium = titleMedium.scaled(scale),
        titleSmall = titleSmall.scaled(scale),
        bodyLarge = bodyLarge.scaled(scale),
        bodyMedium = bodyMedium.scaled(scale),
        bodySmall = bodySmall.scaled(scale),
        labelLarge = labelLarge.scaled(scale),
        labelMedium = labelMedium.scaled(scale),
        labelSmall = labelSmall.scaled(scale),
    )

private fun TextStyle.scaled(scale: Float): TextStyle =
    copy(fontSize = fontSize.scaled(scale), lineHeight = lineHeight.scaled(scale))

private fun TextUnit.scaled(scale: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else this * scale
