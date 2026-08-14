package dev.pschmitt.syncwich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.pschmitt.syncwich.ui.navigation.SyncwichNavHost
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent { SyncwichTheme { SyncwichNavHost() } }
    }
}
