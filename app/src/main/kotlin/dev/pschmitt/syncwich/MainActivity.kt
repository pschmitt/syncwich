package dev.pschmitt.syncwich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
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

        setContent {
            val fontScale by
                settingsRepository.fontScale.collectAsStateWithLifecycle(
                    initialValue = DEFAULT_FONT_SCALE
                )
            SyncwichTheme(fontScale = fontScale) {
                val initialSyncCompleted by
                    settingsRepository.initialSyncCompleted.collectAsStateWithLifecycle(
                        initialValue = null
                    )
                when {
                    !settingsRepository.isConfigured ->
                        SyncwichNavHost(startDestination = Route.Onboarding)
                    initialSyncCompleted == null -> StartupLoadingScreen()
                    initialSyncCompleted == true ->
                        SyncwichNavHost(startDestination = Route.Recipes)
                    else -> SyncwichNavHost(startDestination = Route.InitialSync)
                }
            }
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
