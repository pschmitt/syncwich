package dev.pschmitt.syncwich.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTypographyTest {

    @Test
    fun `font scale applies to every Material typography style`() {
        val original = Typography()
        val scaled = original.scaled(1.2f)

        listOf(
                original.displayLarge to scaled.displayLarge,
                original.displayMedium to scaled.displayMedium,
                original.displaySmall to scaled.displaySmall,
                original.headlineLarge to scaled.headlineLarge,
                original.headlineMedium to scaled.headlineMedium,
                original.headlineSmall to scaled.headlineSmall,
                original.titleLarge to scaled.titleLarge,
                original.titleMedium to scaled.titleMedium,
                original.titleSmall to scaled.titleSmall,
                original.bodyLarge to scaled.bodyLarge,
                original.bodyMedium to scaled.bodyMedium,
                original.bodySmall to scaled.bodySmall,
                original.labelLarge to scaled.labelLarge,
                original.labelMedium to scaled.labelMedium,
                original.labelSmall to scaled.labelSmall,
            )
            .forEach { (before, after) ->
                assertEquals(before.fontSize.value * 1.2f, after.fontSize.value, 0.001f)
                assertEquals(before.lineHeight.value * 1.2f, after.lineHeight.value, 0.001f)
            }
    }

    @Test
    fun `expressive typography gives headings a stronger hierarchy`() {
        assertEquals(FontWeight.Bold, ExpressiveTypography.displayLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, ExpressiveTypography.titleMedium.fontWeight)
    }
}
