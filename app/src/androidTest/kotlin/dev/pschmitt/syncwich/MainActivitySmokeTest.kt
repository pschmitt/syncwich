package dev.pschmitt.syncwich

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Confirms the app launches. A freshly installed app has no stored server URL/API token, so it
 * lands on onboarding rather than the bottom-nav shell - see `MainActivity`'s `startDestination`
 * gating (SW-2). The pre-onboarding-gate version of this test asserted the bottom nav shell
 * directly; that now only applies once a connection is configured (exercised manually / by a later
 * SW-N's end-to-end onboarding flow test, since it requires a real reachable Mealie server).
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun launchesToOnboardingWhenUnconfigured() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(device.wait(Until.hasObject(By.text("Connect to Mealie")), 30_000))
        check(device.hasObject(By.text("Server URL")))
        check(device.hasObject(By.text("API token")))
        check(device.hasObject(By.text("Connect")))
    }
}
