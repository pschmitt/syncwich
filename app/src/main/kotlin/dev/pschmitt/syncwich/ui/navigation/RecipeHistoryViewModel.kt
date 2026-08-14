package dev.pschmitt.syncwich.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.RecipeHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Records recipe-detail navigations without coupling history to the detail screen's UI state. */
@HiltViewModel
class RecipeHistoryViewModel
@Inject
constructor(private val recipeHistoryRepository: RecipeHistoryRepository) : ViewModel() {

    fun recordOpen(recipeId: String) {
        viewModelScope.launch { recipeHistoryRepository.recordOpen(recipeId) }
    }
}
