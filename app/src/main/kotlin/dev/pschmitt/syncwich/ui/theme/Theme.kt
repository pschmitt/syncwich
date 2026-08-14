package dev.pschmitt.syncwich.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.ThemeMode
import dev.pschmitt.syncwich.data.settings.sanitizeFontScale

internal val LightColors =
    lightColorScheme(
        primary = SyncwichTerracotta40,
        secondary = SyncwichBasil40,
        tertiary = SyncwichSaffron40,
        error = SyncwichError40,
        background = SyncwichLightBackground,
        surface = SyncwichLightSurface,
        surfaceContainer = SyncwichLightSurfaceContainer,
    )

internal val DarkColors =
    darkColorScheme(
        // Keep the fallback accent consistent with the launcher and native splash background.
        primary = SyncwichTerracotta80,
        secondary = SyncwichBasil80,
        tertiary = SyncwichSaffron80,
        error = SyncwichError80,
        background = SyncwichDarkBackground,
        surface = SyncwichDarkSurface,
        surfaceContainer = SyncwichDarkSurfaceContainer,
    )

/** A deliberately varied shape scale: expressive cards are soft, while controls stay compact. */
internal val ExpressiveShapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
    )

internal val ExpressiveTypography =
    Typography().copy(
        displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.Bold),
        headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = Typography().labelLarge.copy(letterSpacing = 0.1.sp),
    )

/**
 * Full Material You theming: dynamic, wallpaper-derived color on Android 12+, falling back to a
 * warm, food-inspired hand-picked palette on older devices.
 */
@Composable
fun SyncwichTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = DEFAULT_FONT_SCALE,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography.scaled(sanitizeFontScale(fontScale)),
        shapes = ExpressiveShapes,
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
