package dev.pschmitt.syncwich.ui.onboarding

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.onboarding.OnboardingError
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidationException
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidator
import dev.pschmitt.syncwich.data.onboarding.PasswordTokenMinter
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState

    data object Validating : OnboardingUiState

    data class Error(val message: String) : OnboardingUiState

    data object Success : OnboardingUiState
}

/** Which of onboarding's two paths an in-flight connection attempt (or its error) belongs to. */
enum class OnboardingMode {
    Token,
    Password,
}

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val validator: OnboardingValidator,
    private val passwordTokenMinter: PasswordTokenMinter,
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun connect(serverUrl: String, apiToken: String) {
        if (serverUrl.isBlank() || apiToken.isBlank()) {
            _uiState.value = OnboardingUiState.Error("Enter both the server URL and the API token")
            return
        }
        _uiState.value = OnboardingUiState.Validating
        viewModelScope.launch {
            validator
                .validate(serverUrl, apiToken)
                .onSuccess { persistAndSucceed(serverUrl, apiToken) }
                .onFailure { error ->
                    _uiState.value =
                        OnboardingUiState.Error(error.toUserMessage(OnboardingMode.Token))
                }
        }
    }

    /**
     * The username/password path: never persists the password or the short-lived JWT it exchanges
     * it for - only the long-lived API token minted on the user's behalf via [PasswordTokenMinter],
     * named after this device so it's identifiable/revocable independently of Syncwich from
     * Mealie's own Profile -> API Tokens page.
     */
    fun connectWithPassword(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = OnboardingUiState.Error("Enter the server URL, username, and password")
            return
        }
        _uiState.value = OnboardingUiState.Validating
        viewModelScope.launch {
            passwordTokenMinter
                .mintToken(serverUrl, username, password, tokenName = syncwichTokenName())
                .onSuccess { apiToken -> persistAndSucceed(serverUrl, apiToken) }
                .onFailure { error ->
                    _uiState.value =
                        OnboardingUiState.Error(error.toUserMessage(OnboardingMode.Password))
                }
        }
    }

    private fun persistAndSucceed(serverUrl: String, apiToken: String) {
        // Only persisted - and only now starts being read by the network layer's interceptors -
        // once the server has actually confirmed this token works.
        settingsRepository.save(serverUrl, apiToken)
        // The first pass is run in the blocking InitialSyncScreen. Cancel the startup request
        // queued by Application so it cannot race that foreground pass.
        syncScheduler.cancelStartup()
        _uiState.value = OnboardingUiState.Success
    }

    private fun Throwable.toUserMessage(mode: OnboardingMode): String =
        when (this) {
            is OnboardingValidationException ->
                when (error) {
                    OnboardingError.MalformedUrl ->
                        "Enter a valid server URL, e.g. https://demo.mealie.io"
                    OnboardingError.Unauthorized ->
                        when (mode) {
                            OnboardingMode.Token ->
                                "That server rejected the API token. Generate a new long-lived " +
                                    "token in Mealie under Profile → API Tokens and try again."
                            OnboardingMode.Password -> "Incorrect username or password."
                        }
                    OnboardingError.Unreachable ->
                        "Couldn't reach that server. Check the URL and your network connection."
                    is OnboardingError.ServerError ->
                        "The server responded with an error (HTTP ${error.code})."
                }
            else -> message?.takeIf { it.isNotBlank() } ?: "Couldn't connect to that server."
        }
}

private fun syncwichTokenName(): String {
    val deviceName = Build.MODEL.trim().ifBlank { Build.DEVICE }
    return "Syncwich ($deviceName)"
}
