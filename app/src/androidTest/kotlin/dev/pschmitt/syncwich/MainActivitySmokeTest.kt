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

/** Confirms the app launches and its bottom navigation shell renders. */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun launchesToRecipesTab() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(device.wait(Until.hasObject(By.text("Recipes")), 30_000))
        check(device.hasObject(By.text("Meal Plan")))
        check(device.hasObject(By.text("Shopping")))
        check(device.hasObject(By.text("Cookbooks")))
    }
}
