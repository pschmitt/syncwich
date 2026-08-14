package dev.pschmitt.syncwich.ui.settings

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AboutSettingsViewModel @Inject constructor(private val settingsRepository: SettingsRepository) :
    ViewModel() {

    val developerMode: StateFlow<Boolean> =
        settingsRepository.developerMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            false,
        )

    private var buildRowTapCount = 0
    private var lastBuildRowTapAt = 0L

    fun onBuildRowTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBuildRowTapAt > TAP_WINDOW_MILLIS) buildRowTapCount = 0
        lastBuildRowTapAt = now
        buildRowTapCount++
        if (buildRowTapCount >= REQUIRED_TAPS && !developerMode.value) {
            buildRowTapCount = 0
            viewModelScope.launch { settingsRepository.setDeveloperMode(true) }
        }
    }

    private companion object {
        const val REQUIRED_TAPS = 7
        const val TAP_WINDOW_MILLIS = 2_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
