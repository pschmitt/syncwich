package dev.pschmitt.syncwich.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.backup.BackupFormatException
import dev.pschmitt.syncwich.data.backup.BackupManager
import dev.pschmitt.syncwich.data.backup.BackupPasswordRequiredException
import dev.pschmitt.syncwich.data.backup.BackupScheduler
import dev.pschmitt.syncwich.data.backup.BackupWrongPasswordException
import dev.pschmitt.syncwich.data.settings.BackupFrequency
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupOperationState {
    data object Idle : BackupOperationState
    data object Working : BackupOperationState
    data class PasswordRequired(val uri: Uri) : BackupOperationState
    data class Success(val message: String) : BackupOperationState
    data class Error(val message: String) : BackupOperationState
}

@HiltViewModel
class BackupSettingsViewModel
@Inject
constructor(
    val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val backupScheduler: BackupScheduler,
) : ViewModel() {
    private val _operation = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
    val operation: StateFlow<BackupOperationState> = _operation.asStateFlow()

    fun export(uri: Uri, password: String?) {
        viewModelScope.launch {
            _operation.value = BackupOperationState.Working
            runCatching { backupManager.write(uri, password) }
                .onSuccess {
                    settingsRepository.recordBackupSuccess()
                    _operation.value = BackupOperationState.Success("Backup created")
                }
                .onFailure { _operation.value = BackupOperationState.Error(it.toUserMessage()) }
        }
    }

    fun restore(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _operation.value = BackupOperationState.Working
            try {
                val manifest = backupManager.restore(uri, password)
                backupScheduler.schedule()
                _operation.value =
                    BackupOperationState.Success(
                        "Restored Syncwich backup from ${manifest.appVersionName}"
                    )
            } catch (_: BackupPasswordRequiredException) {
                _operation.value = BackupOperationState.PasswordRequired(uri)
            } catch (error: Exception) {
                _operation.value = BackupOperationState.Error(error.toUserMessage())
            }
        }
    }

    fun consumeOperation() {
        _operation.value = BackupOperationState.Idle
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setScheduledBackupEnabled(enabled)
            backupScheduler.schedule()
        }
    }

    fun setFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            settingsRepository.setScheduledBackupFrequency(frequency)
            backupScheduler.schedule()
        }
    }

    fun setFolderUri(uri: Uri?) {
        viewModelScope.launch {
            uri?.let {
                runCatching {
                    settingsRepository.setScheduledBackupFolderUri(it.toString())
                }
            } ?: settingsRepository.setScheduledBackupFolderUri(null)
            backupScheduler.schedule()
        }
    }

    fun setPassword(password: String) {
        settingsRepository.setScheduledBackupPassword(password.takeIf(String::isNotEmpty))
    }

    private fun Throwable.toUserMessage(): String =
        when (this) {
            is BackupPasswordRequiredException -> "This backup needs its password."
            is BackupWrongPasswordException -> "That backup password is incorrect."
            is BackupFormatException -> message ?: "The backup file is not valid."
            else -> message?.takeIf(String::isNotBlank) ?: "The backup operation failed."
        }
}
