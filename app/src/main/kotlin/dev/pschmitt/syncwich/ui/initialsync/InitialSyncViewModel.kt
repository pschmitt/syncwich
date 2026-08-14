package dev.pschmitt.syncwich.ui.initialsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.InitialSyncException
import dev.pschmitt.syncwich.sync.InitialSyncProgress
import dev.pschmitt.syncwich.sync.InitialSyncRunner
import dev.pschmitt.syncwich.sync.InitialSyncStage
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface InitialSyncUiState {
    data object Starting : InitialSyncUiState

    data class Syncing(val progress: InitialSyncProgress) : InitialSyncUiState

    data class Failed(val stage: InitialSyncStage, val message: String) : InitialSyncUiState

    data object Completed : InitialSyncUiState

    data object Cancelled : InitialSyncUiState
}

@HiltViewModel
class InitialSyncViewModel
@Inject
constructor(
    private val runner: InitialSyncRunner,
    private val settingsRepository: SettingsRepository,
    syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InitialSyncUiState>(InitialSyncUiState.Starting)
    val uiState: StateFlow<InitialSyncUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    init {
        // Application schedules this work eagerly for ordinary launches. If this is the first
        // login, the foreground runner owns the pass and must not race a WorkManager copy.
        syncScheduler.cancelStartup()
        startSync()
    }

    fun retry() {
        if (_uiState.value is InitialSyncUiState.Failed) startSync()
    }

    fun cancel() {
        syncJob?.cancel()
        _uiState.value = InitialSyncUiState.Cancelled
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        _uiState.value = InitialSyncUiState.Starting
        syncJob = viewModelScope.launch {
            if (settingsRepository.initialSyncCompleted.first()) {
                _uiState.value = InitialSyncUiState.Completed
                return@launch
            }

            runner
                .run { progress -> _uiState.value = InitialSyncUiState.Syncing(progress) }
                .fold(
                    onSuccess = {
                        settingsRepository.recordInitialSyncSuccess()
                        _uiState.value = InitialSyncUiState.Completed
                    },
                    onFailure = { error ->
                        if (isActive) {
                            val initialSyncError = error as? InitialSyncException
                            _uiState.value =
                                InitialSyncUiState.Failed(
                                    stage = initialSyncError?.stage ?: InitialSyncStage.Recipes,
                                    message =
                                        initialSyncError?.cause?.message?.takeIf { it.isNotBlank() }
                                            ?: "Couldn't finish the first sync.",
                                )
                        }
                    },
                )
        }
    }
}
