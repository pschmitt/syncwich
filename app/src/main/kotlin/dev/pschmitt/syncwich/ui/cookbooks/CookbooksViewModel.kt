package dev.pschmitt.syncwich.ui.cookbooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CookbooksViewModel
@Inject
constructor(
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val cookbooks: StateFlow<List<CookbookEntity>> =
        cookbookRepository
            .observeCookbooks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Used to build a recipe's cover-image URL - see `RecipeSummaryEntity.image`'s kdoc. */
    val serverUrl: String
        get() = settingsRepository.credentials.value.serverUrl

    init {
        viewModelScope.launch { cookbookRepository.refreshCookbooks() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
