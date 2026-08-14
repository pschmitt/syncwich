package dev.pschmitt.syncwich

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.SyncNotifier
import dev.pschmitt.syncwich.ui.navigation.Route
import dev.pschmitt.syncwich.ui.navigation.SyncwichNavHost
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var syncNotifier: SyncNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
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
                        SyncwichNavHost(startDestination = Route.Home)
                    else -> SyncwichNavHost(startDestination = Route.InitialSync)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncNotifier.onAppForeground()
    }

    override fun onStop() {
        syncNotifier.onAppBackground()
        super.onStop()
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
