package dev.pschmitt.syncwich.ui.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.data.repository.MealPlanRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MealPlanUiState(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val entries: List<MealPlanEntryEntity> = emptyList(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(private val mealPlanRepository: MealPlanRepository) :
    ViewModel() {

    private val weekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MealPlanUiState> =
        combine(weekStart, isRefreshing) { start, refreshing -> start to refreshing }
            .flatMapLatest { (start, refreshing) ->
                val end = start.plusDays(6)
                mealPlanRepository.observeMealPlan(start, end).map { entries ->
                    MealPlanUiState(
                        weekStart = start,
                        weekEnd = end,
                        entries = entries,
                        isRefreshing = refreshing,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    MealPlanUiState(weekStart = weekStart.value, weekEnd = weekStart.value.plusDays(6)),
            )

    init {
        refresh()
    }

    fun showPreviousWeek() {
        weekStart.value = weekStart.value.minusWeeks(1)
        refresh()
    }

    fun showNextWeek() {
        weekStart.value = weekStart.value.plusWeeks(1)
        refresh()
    }

    fun showCurrentWeek() {
        weekStart.value = LocalDate.now().with(DayOfWeek.MONDAY)
        refresh()
    }

    fun refresh() {
        val start = weekStart.value
        val end = start.plusDays(6)
        viewModelScope.launch {
            isRefreshing.value = true
            mealPlanRepository.refreshMealPlan(start, end)
            isRefreshing.value = false
        }
    }
}
