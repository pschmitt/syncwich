package dev.pschmitt.syncwich.ui.settings

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _developerModeToast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val developerModeToast = _developerModeToast.asSharedFlow()

    private var buildRowTapCount = 0
    private var lastBuildRowTapAt = 0L
    private var unlockInProgress = false

    fun onBuildRowTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBuildRowTapAt > TAP_WINDOW_MILLIS) buildRowTapCount = 0
        lastBuildRowTapAt = now
        buildRowTapCount++
        if (developerMode.value || unlockInProgress) return
        if (buildRowTapCount >= REQUIRED_TAPS) {
            buildRowTapCount = 0
            unlockInProgress = true
            viewModelScope.launch {
                settingsRepository.setDeveloperMode(true)
                _developerModeToast.emit("Developer mode enabled")
                unlockInProgress = false
            }
        } else {
            _developerModeToast.tryEmit(
                "${REQUIRED_TAPS - buildRowTapCount} more taps to become a developer"
            )
        }
    }

    private companion object {
        const val REQUIRED_TAPS = 7
        const val TAP_WINDOW_MILLIS = 2_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
