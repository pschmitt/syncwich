package dev.pschmitt.syncwich

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.EntryPointAccessors
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.di.SettingsEntryPoint
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * Captures Play Store listing screenshots (en-US only, see fastlane/Screengrabfile) of each
 * bottom-nav destination. Update the captured labels/journey as real screens replace the SW-1
 * placeholders in later phases.
 *
 * Seeds a placeholder connection directly via [SettingsRepository] before launching the activity
 * (SW-2's onboarding gate would otherwise land a fresh install on onboarding, not the bottom-nav
 * shell) - safe because every SW-1 placeholder screen captured here is still static and doesn't
 * touch the network, so no real Mealie server is needed to render them.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    companion object {
        @get:ClassRule @JvmStatic val localeTestRule = LocaleTestRule()
    }

    private val settingsRepository: SettingsRepository by lazy {
        EntryPointAccessors.fromApplication(
                ApplicationProvider.getApplicationContext(),
                SettingsEntryPoint::class.java,
            )
            .settingsRepository()
    }

    @Before
    fun seedConnection() {
        settingsRepository.save("https://mealie.invalid", "screenshot-test-token")
    }

    @After
    fun clearConnection() {
        settingsRepository.clear()
    }

    @Test
    fun captureStoreScreenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        ActivityScenario.launch(MainActivity::class.java).use {
            // The first wait is the flaky one on cold CI emulators - boot-completed/provisioned/
            // package-service can all be ready while the app's own first Compose frame still
            // isn't up yet. Give it more room than the later waits, which run against an
            // already-warm process.
            check(device.wait(Until.hasObject(By.text("Recipes")), 30_000))
            Screengrab.screenshot("01_recipes")

            device.findObject(By.text("Meal Plan")).click()
            check(device.wait(Until.hasObject(By.text("Meal plan calendar")), 15_000))
            Screengrab.screenshot("02_meal_plan")

            device.findObject(By.text("Shopping")).click()
            check(device.wait(Until.hasObject(By.text("Shopping lists")), 15_000))
            Screengrab.screenshot("03_shopping_lists")

            device.findObject(By.text("Cookbooks")).click()
            check(
                device.wait(Until.hasObject(By.textContains("curated recipe collections")), 15_000)
            )
            Screengrab.screenshot("04_cookbooks")
        }
    }
}
