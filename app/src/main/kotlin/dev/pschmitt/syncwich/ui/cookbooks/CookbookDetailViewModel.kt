package dev.pschmitt.syncwich.ui.cookbooks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CookbookDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val cookbookId = savedStateHandle.toRoute<Route.CookbookDetail>().cookbookId

    /** Used to build a recipe's cover-image URL - see `RecipeSummaryEntity.image`'s kdoc. */
    val serverUrl: String
        get() = settingsRepository.credentials.value.serverUrl

    val cookbook: StateFlow<CookbookEntity?> =
        cookbookRepository
            .observeCookbook(cookbookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val recipes: StateFlow<List<RecipeSummaryEntity>> =
        cookbookRepository
            .observeCookbookRecipes(cookbookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        // No per-cookbook refresh endpoint exists server-side (see CookbookRepository's kdoc) - a
        // full refresh is cheap enough (a handful of cookbooks, at most) to just re-run here too, in
        // case this screen is opened via a deep link before CookbooksScreen's own refresh ran.
        viewModelScope.launch { cookbookRepository.refreshCookbooks() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
