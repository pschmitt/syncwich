package dev.pschmitt.syncwich.ui.settings

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AboutSettingsViewModel
@Inject
constructor(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val developerMode: StateFlow<Boolean> =
        settingsRepository.developerMode.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false,
        )

    private val _developerModeToast =
        MutableSharedFlow<String>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val developerModeToast = _developerModeToast.asSharedFlow()

    private val tapState = DeveloperModeTapState()
    private var developerModeUnlocked = false
    private var unlockInProgress = false

    fun onBuildRowTap() {
        if (unlockInProgress) return
        when (
            val action =
                tapState.onTap(SystemClock.elapsedRealtime(), developerMode.value || developerModeUnlocked)
        ) {
            DeveloperModeTapAction.AlreadyDeveloper ->
                _developerModeToast.tryEmit(ALREADY_DEVELOPER_MESSAGE)
            is DeveloperModeTapAction.Progress ->
                _developerModeToast.tryEmit(
                    "${action.remainingTaps} more taps to become a developer"
                )
            DeveloperModeTapAction.Unlock -> {
                unlockInProgress = true
                viewModelScope.launch {
                    var enabled = false
                    try {
                        settingsRepository.setDeveloperMode(true)
                        enabled = true
                        _developerModeToast.tryEmit(DEVELOPER_MODE_ENABLED_MESSAGE)
                    } finally {
                        if (enabled) developerModeUnlocked = true
                        unlockInProgress = false
                    }
                }
            }
        }
    }

    private companion object {
        const val ALREADY_DEVELOPER_MESSAGE = "You are already a developer"
        const val DEVELOPER_MODE_ENABLED_MESSAGE = "Developer mode enabled"
    }
}

internal sealed interface DeveloperModeTapAction {
    data object AlreadyDeveloper : DeveloperModeTapAction
    data object Unlock : DeveloperModeTapAction
    data class Progress(val remainingTaps: Int) : DeveloperModeTapAction
}

internal class DeveloperModeTapState(
    private val requiredTaps: Int = 7,
    private val tapWindowMillis: Long = 2_000L,
) {
    private var tapCount = 0
    private var lastTapAt = 0L

    fun onTap(now: Long, developerModeEnabled: Boolean): DeveloperModeTapAction {
        if (developerModeEnabled) return DeveloperModeTapAction.AlreadyDeveloper
        if (now - lastTapAt > tapWindowMillis) tapCount = 0
        lastTapAt = now
        tapCount++
        return if (tapCount >= requiredTaps) {
            tapCount = 0
            DeveloperModeTapAction.Unlock
        } else {
            DeveloperModeTapAction.Progress(requiredTaps - tapCount)
        }
    }
}
