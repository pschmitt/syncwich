package dev.pschmitt.syncwich.ui.mealplan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@Composable
fun MealPlanScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.CalendarMonth,
        title = "Meal plan calendar",
        subtitle = "See what's planned for the week, synced from your household's Mealie meal plan.",
        modifier = modifier,
    )
}
