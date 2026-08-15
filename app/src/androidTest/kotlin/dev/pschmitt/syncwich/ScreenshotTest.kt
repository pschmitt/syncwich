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
 * Captures Play Store listing screenshots (en-US only, see fastlane/Screengrabfile) from the
 * current Home, Recipes, and Settings destinations against the disposable Mealie fixture started by
 * CI. No real Mealie server or user data is ever used.
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

    private val syncScheduler by lazy {
        EntryPointAccessors.fromApplication(
                ApplicationProvider.getApplicationContext(),
                SettingsEntryPoint::class.java,
            )
            .syncScheduler()
    }

    @Before
    fun seedConnection() {
        val arguments = InstrumentationRegistry.getArguments()
        val baseUrl = arguments.getString("e2e_base_url") ?: error("e2e_base_url is required")
        val token = arguments.getString("e2e_token") ?: error("e2e_token is required")
        settingsRepository.save(baseUrl, token)
        syncScheduler.syncAll()
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
            if (device.wait(Until.hasObject(By.text("Allow")), 5_000)) {
                device.findObject(By.text("Allow")).click()
            }
            check(device.wait(Until.hasObject(By.text("Home")), 30_000))
            check(device.wait(Until.hasObject(By.text("Synced")), 60_000))
            Screengrab.screenshot("01_home")

            device.findObject(By.text("Recipes")).click()
            check(device.wait(Until.hasObject(By.text("Search recipes")), 15_000))
            Screengrab.screenshot("02_recipes")

            device.findObject(By.desc("Settings")).click()
            check(device.wait(Until.hasObject(By.text("Settings")), 15_000))
            Screengrab.screenshot("03_settings")
        }
    }
}
