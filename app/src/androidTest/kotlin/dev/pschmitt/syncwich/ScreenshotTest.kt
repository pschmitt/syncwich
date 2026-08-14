package dev.pschmitt.syncwich

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * Captures Play Store listing screenshots (en-US only, see fastlane/Screengrabfile) of each
 * bottom-nav destination. Update the captured labels/journey as real screens replace the SW-1
 * placeholders in later phases.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    companion object {
        @get:ClassRule @JvmStatic val localeTestRule = LocaleTestRule()
    }

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun captureStoreScreenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // The first wait is the flaky one on cold CI emulators - boot-completed/provisioned/
        // package-service can all be ready while the app's own first Compose frame still isn't up
        // yet. Give it more room than the later waits, which run against an already-warm process.
        check(device.wait(Until.hasObject(By.text("Recipes")), 30_000))
        Screengrab.screenshot("01_recipes")

        device.findObject(By.text("Meal Plan")).click()
        check(device.wait(Until.hasObject(By.text("Meal plan calendar")), 15_000))
        Screengrab.screenshot("02_meal_plan")

        device.findObject(By.text("Shopping")).click()
        check(device.wait(Until.hasObject(By.text("Shopping lists")), 15_000))
        Screengrab.screenshot("03_shopping_lists")

        device.findObject(By.text("Cookbooks")).click()
        check(device.wait(Until.hasObject(By.textContains("curated recipe collections")), 15_000))
        Screengrab.screenshot("04_cookbooks")
    }
}
