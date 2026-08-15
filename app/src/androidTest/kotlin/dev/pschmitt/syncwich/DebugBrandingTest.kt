package dev.pschmitt.syncwich

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugBrandingTest {

    @Test
    fun debugVariantUsesDistinctLauncherBranding() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("Syncwich (debug)", context.getString(R.string.app_name))
        assertEquals(
            Color.rgb(106, 27, 154),
            ContextCompat.getColor(context, R.color.icon_background),
        )
    }
}
