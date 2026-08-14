package dev.pschmitt.syncwich

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.math.roundToInt
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

        val titleTop = device.findObject(By.text("Connect to Mealie")).visibleBounds.top
        val statusBarHeight =
            android.content.res.Resources.getSystem()
                .getDimensionPixelSize(
                    android.content.res.Resources.getSystem()
                        .getIdentifier("status_bar_height", "dimen", "android")
                )
        val density =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .resources
                .displayMetrics
                .density

        // MainActivity uses edge-to-edge, so onboarding's scaffold should apply one status-bar
        // inset. A second inset from the outer navigation scaffold would push this title beyond
        // the 120dp content-start budget; keep the lower bound to guard the safety requirement.
        check(titleTop >= statusBarHeight) {
            "Onboarding content overlaps the status bar: top=$titleTop, status=$statusBarHeight"
        }
        check(titleTop < statusBarHeight + (120 * density).roundToInt()) {
            "Onboarding content has an excessive top inset: top=$titleTop, status=$statusBarHeight"
        }
    }
}
