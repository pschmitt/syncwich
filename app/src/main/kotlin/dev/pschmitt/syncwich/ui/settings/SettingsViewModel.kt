package dev.pschmitt.syncwich.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import android.os.Build
import dev.pschmitt.syncwich.data.onboarding.OnboardingError
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidationException
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidator
import dev.pschmitt.syncwich.data.onboarding.PasswordTokenMinter
import dev.pschmitt.syncwich.data.repository.AccountRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.data.settings.MealieCredentials
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionUpdateState {
    data object Idle : ConnectionUpdateState

    data object Validating : ConnectionUpdateState

    data object Saved : ConnectionUpdateState

    data class Error(val message: String) : ConnectionUpdateState
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val validator: OnboardingValidator,
    private val passwordTokenMinter: PasswordTokenMinter,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val credentials: StateFlow<MealieCredentials> = settingsRepository.credentials

    private val _connectionUpdateState =
        MutableStateFlow<ConnectionUpdateState>(ConnectionUpdateState.Idle)
    val connectionUpdateState: StateFlow<ConnectionUpdateState> =
        _connectionUpdateState.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    val navigationBarOrder: StateFlow<List<String>> =
        settingsRepository.navigationBarOrder.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            emptyList(),
        )

    val navigationBarHiddenItems: StateFlow<Set<String>> =
        settingsRepository.navigationBarHiddenItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            emptySet(),
        )

    val fontScale: StateFlow<Float> =
        settingsRepository.fontScale.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            DEFAULT_FONT_SCALE,
        )

    fun saveNavigationBarOrder(order: List<String>) {
        viewModelScope.launch { settingsRepository.saveNavigationBarOrder(order) }
    }

    fun setNavigationBarItemHidden(key: String, hidden: Boolean) {
        viewModelScope.launch { settingsRepository.setNavigationBarItemHidden(key, hidden) }
    }

    fun saveFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.saveFontScale(scale) }
    }

    fun updateConnection(serverUrl: String, apiToken: String) {
        if (serverUrl.isBlank() || apiToken.isBlank()) {
            _connectionUpdateState.value =
                ConnectionUpdateState.Error("Enter both the server URL and the API token")
            return
        }
        _connectionUpdateState.value = ConnectionUpdateState.Validating
        viewModelScope.launch {
            validator
                .validate(serverUrl, apiToken)
                .onSuccess { persistConnection(serverUrl, apiToken) }
                .onFailure { error ->
                    _connectionUpdateState.value =
                        ConnectionUpdateState.Error(error.toUserMessage(passwordMode = false))
                }
        }
    }

    fun updateConnectionWithPassword(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _connectionUpdateState.value =
                ConnectionUpdateState.Error("Enter the server URL, username, and password")
            return
        }
        _connectionUpdateState.value = ConnectionUpdateState.Validating
        viewModelScope.launch {
            passwordTokenMinter
                .mintToken(serverUrl, username, password, tokenName = syncwichTokenName())
                .onSuccess { apiToken -> persistConnection(serverUrl, apiToken) }
                .onFailure { error ->
                    _connectionUpdateState.value =
                        ConnectionUpdateState.Error(error.toUserMessage(passwordMode = true))
                }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        if (_isSigningOut.value) return
        _isSigningOut.value = true
        viewModelScope.launch {
            try {
                accountRepository.signOut()
                onSignedOut()
            } finally {
                _isSigningOut.value = false
            }
        }
    }

    private suspend fun persistConnection(serverUrl: String, apiToken: String) {
        val normalizedUrl = serverUrl.trim().trimEnd('/')
        val currentCredentials = settingsRepository.credentials.value
        if (currentCredentials.isValid && currentCredentials.serverUrl != normalizedUrl) {
            // A Room cache belongs to one Mealie server. Clear it before switching servers so an
            // offline launch can never present recipes from the previous account as current.
            accountRepository.signOut()
        }
        settingsRepository.save(normalizedUrl, apiToken)
        // A replacement token can keep using the existing cache; a server switch starts a
        // best-effort refresh without making this settings flow depend on network availability.
        syncScheduler.scheduleStartup()
        _connectionUpdateState.value = ConnectionUpdateState.Saved
    }

    private fun Throwable.toUserMessage(passwordMode: Boolean): String =
        when (this) {
            is OnboardingValidationException ->
                when (error) {
                    OnboardingError.MalformedUrl ->
                        "Enter a valid server URL, e.g. https://demo.mealie.io"
                    OnboardingError.Unauthorized ->
                        if (passwordMode) "Incorrect username or password."
                        else "That server rejected the API token. Generate a new token and try again."
                    OnboardingError.Unreachable ->
                        "Couldn't reach that server. Check the URL and your network connection."
                    is OnboardingError.ServerError ->
                        "The server responded with an error (HTTP ${error.code})."
                }
            else -> message?.takeIf { it.isNotBlank() } ?: "Couldn't connect to that server."
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        fun syncwichTokenName(): String {
            val deviceName = Build.MODEL.trim().ifBlank { Build.DEVICE }
            return "Syncwich ($deviceName)"
        }
    }
}
