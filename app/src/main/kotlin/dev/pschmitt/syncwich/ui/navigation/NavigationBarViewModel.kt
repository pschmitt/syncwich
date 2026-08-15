package dev.pschmitt.syncwich.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.settings.NavigationBarCacheAvailability
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import dev.pschmitt.syncwich.data.settings.NavigationBarPreferences
import dev.pschmitt.syncwich.data.settings.defaultHiddenItems
import dev.pschmitt.syncwich.data.settings.resolveNavBarOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class NavigationBarViewModel
@Inject
constructor(
    preferences: NavigationBarPreferences,
    cacheAvailability: NavigationBarCacheAvailability,
) : ViewModel() {

    val visibleItemKeys: StateFlow<List<String>> =
        combine(
                preferences.navigationBarOrder,
                preferences.navigationBarHiddenItems,
                preferences.navigationBarShownItems,
                cacheAvailability.state,
            ) { persisted, hidden, explicitlyShown, cache ->
                resolveNavBarOrder(
                    natural = NavigationBarItemKeys.all,
                    persisted = persisted,
                    hidden = hidden,
                    pinned = setOf(NavigationBarItemKeys.RECIPES),
                    defaultHidden = cache.defaultHiddenItems(),
                    explicitlyShown = explicitlyShown,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                listOf(NavigationBarItemKeys.HOME, NavigationBarItemKeys.RECIPES),
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
