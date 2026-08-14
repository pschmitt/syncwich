package dev.pschmitt.syncwich.ui.settings

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
class SettingsViewModel @Inject constructor(private val settingsRepository: SettingsRepository) :
    ViewModel() {

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

    fun saveNavigationBarOrder(order: List<String>) {
        viewModelScope.launch { settingsRepository.saveNavigationBarOrder(order) }
    }

    fun setNavigationBarItemHidden(key: String, hidden: Boolean) {
        viewModelScope.launch { settingsRepository.setNavigationBarItemHidden(key, hidden) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
