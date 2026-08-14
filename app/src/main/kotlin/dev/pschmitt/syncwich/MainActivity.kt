package dev.pschmitt.syncwich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import dev.pschmitt.syncwich.ui.navigation.SyncwichNavHost
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read once at launch, not observed reactively - a mid-session sign-out (SW-7) navigates
        // back to onboarding explicitly rather than relying on this recomposing.
        val startDestination =
            if (settingsRepository.isConfigured) Route.Recipes else Route.Onboarding

        setContent { SyncwichTheme { SyncwichNavHost(startDestination = startDestination) } }
    }
}
