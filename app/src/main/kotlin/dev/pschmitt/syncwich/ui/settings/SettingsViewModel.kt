package dev.pschmitt.syncwich.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.UsersApi
import dev.pschmitt.syncwich.data.api.dto.UserDto
import dev.pschmitt.syncwich.data.onboarding.OnboardingError
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidationException
import dev.pschmitt.syncwich.data.onboarding.OnboardingValidator
import dev.pschmitt.syncwich.data.onboarding.OidcAuthClient
import dev.pschmitt.syncwich.data.onboarding.OidcLoginException
import dev.pschmitt.syncwich.data.onboarding.PasswordTokenMinter
import dev.pschmitt.syncwich.data.repository.AccountRepository
import dev.pschmitt.syncwich.data.settings.DEFAULT_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.DEFAULT_SYNC_INTERVAL_HOURS
import dev.pschmitt.syncwich.data.settings.DEFAULT_SYNC_ON_APP_START
import dev.pschmitt.syncwich.data.settings.MealieCredentials
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.data.settings.ThemeMode
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface ConnectionUpdateState {
    data object Idle : ConnectionUpdateState

    data object Validating : ConnectionUpdateState

    data object Saved : ConnectionUpdateState

    data class Error(val message: String) : ConnectionUpdateState
}

sealed interface CredentialsTestState {
    data object Idle : CredentialsTestState

    data object Testing : CredentialsTestState

    data class Success(val userDisplayName: String) : CredentialsTestState

    data class Error(val message: String) : CredentialsTestState
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val validator: OnboardingValidator,
    private val passwordTokenMinter: PasswordTokenMinter,
    private val oidcAuthClient: OidcAuthClient,
    private val usersApi: UsersApi,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val credentials: StateFlow<MealieCredentials> = settingsRepository.credentials

    private val _connectionUpdateState =
        MutableStateFlow<ConnectionUpdateState>(ConnectionUpdateState.Idle)
    val connectionUpdateState: StateFlow<ConnectionUpdateState> =
        _connectionUpdateState.asStateFlow()

    private val _credentialsTestState =
        MutableStateFlow<CredentialsTestState>(CredentialsTestState.Idle)
    val credentialsTestState: StateFlow<CredentialsTestState> = _credentialsTestState.asStateFlow()

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

    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.themeMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ThemeMode.SYSTEM,
        )

    val ingredientChecklistEnabled: StateFlow<Boolean> =
        settingsRepository.ingredientChecklistEnabled.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            false,
        )

    val syncOnlyOnWifi: StateFlow<Boolean> =
        settingsRepository.syncOnlyOnWifi.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            false,
        )

    val syncWhileRoaming: StateFlow<Boolean> =
        settingsRepository.syncWhileRoaming.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            DEFAULT_SYNC_ON_APP_START,
        )

    val syncIntervalHours: StateFlow<Int> =
        settingsRepository.syncIntervalHours.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            DEFAULT_SYNC_INTERVAL_HOURS,
        )

    val syncOnAppStart: StateFlow<Boolean> =
        settingsRepository.syncOnAppStart.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            true,
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

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.saveThemeMode(mode) }
    }

    fun setIngredientChecklistEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setIngredientChecklistEnabled(enabled) }
    }

    fun setSyncOnlyOnWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncOnlyOnWifi(enabled)
            syncScheduler.schedulePeriodic()
        }
    }

    fun setSyncWhileRoaming(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncWhileRoaming(enabled)
            syncScheduler.schedulePeriodic()
        }
    }

    fun setSyncIntervalHours(hours: Int) {
        viewModelScope.launch {
            settingsRepository.setSyncIntervalHours(hours)
            syncScheduler.schedulePeriodic()
        }
    }

    fun setSyncOnAppStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncOnAppStart(enabled)
            if (!enabled) syncScheduler.cancelStartup()
        }
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

    fun beginOidc(serverUrl: String): String? =
        oidcAuthClient.authorizationUrl(serverUrl).getOrElse { error ->
            _connectionUpdateState.value =
                ConnectionUpdateState.Error(error.message ?: "Enter a valid server URL")
            null
        }

    fun updateConnectionWithOidc(serverUrl: String, callbackUrl: String, cookies: String) {
        _connectionUpdateState.value = ConnectionUpdateState.Validating
        viewModelScope.launch {
            oidcAuthClient
                .exchangeCallback(serverUrl, callbackUrl, cookies)
                .onSuccess { persistConnection(serverUrl, it, oidc = true) }
                .onFailure { error ->
                    _connectionUpdateState.value =
                        ConnectionUpdateState.Error(
                            (error as? OidcLoginException)?.message
                                ?: "Couldn't finish OIDC sign-in."
                        )
                }
        }
    }

    fun failOidc(message: String) {
        _connectionUpdateState.value = ConnectionUpdateState.Error(message)
    }

    fun testCredentials() {
        if (_credentialsTestState.value is CredentialsTestState.Testing) return
        _credentialsTestState.value = CredentialsTestState.Testing
        viewModelScope.launch {
            runCatching { usersApi.getSelf() }
                .onSuccess { user ->
                    _credentialsTestState.value = CredentialsTestState.Success(user.displayName())
                }
                .onFailure { error ->
                    _credentialsTestState.value = CredentialsTestState.Error(error.toTestMessage())
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

    private suspend fun persistConnection(
        serverUrl: String,
        apiToken: String,
        oidc: Boolean = false,
    ) {
        val normalizedUrl = serverUrl.trim().trimEnd('/')
        val currentCredentials = settingsRepository.credentials.value
        if (currentCredentials.isValid && currentCredentials.serverUrl != normalizedUrl) {
            // A Room cache belongs to one Mealie server. Clear it before switching servers so an
            // offline launch can never present recipes from the previous account as current.
            accountRepository.signOut()
        }
        if (oidc) settingsRepository.saveOidc(normalizedUrl, apiToken)
        else settingsRepository.save(normalizedUrl, apiToken)
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
                        else
                            "That server rejected the API token. Generate a new token and try again."
                    OnboardingError.Unreachable ->
                        "Couldn't reach that server. Check the URL and your network connection."
                    is OnboardingError.ServerError ->
                        "The server responded with an error (HTTP ${error.code})."
                }
            else -> message?.takeIf { it.isNotBlank() } ?: "Couldn't connect to that server."
        }

    private fun Throwable.toTestMessage(): String =
        when (this) {
            is HttpException -> "The server rejected the saved credentials (HTTP ${code()})."
            else ->
                "Couldn't verify the saved credentials. Check the server connection and try again."
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        fun syncwichTokenName(): String {
            val deviceName = Build.MODEL.trim().ifBlank { Build.DEVICE }
            return "Syncwich ($deviceName)"
        }
    }
}

internal fun UserDto.displayName(): String =
    listOf(fullName, username, email, id).firstOrNull { !it.isNullOrBlank() }
        ?: "authenticated user"
