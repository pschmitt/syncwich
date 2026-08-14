package dev.pschmitt.syncwich.ui.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tapas
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    viewModel: MealPlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WeekHeader(
                    weekStart = uiState.weekStart,
                    weekEnd = uiState.weekEnd,
                    isRefreshing = uiState.refreshState.isRefreshing,
                    onPreviousWeek = viewModel::showPreviousWeek,
                    onNextWeek = viewModel::showNextWeek,
                    onToday = viewModel::showCurrentWeek,
                )
                RefreshErrorBanner(
                    errorMessage = uiState.refreshState.errorMessage,
                    onRetry = viewModel::refresh,
                )
                if (uiState.entries.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.Filled.CalendarMonth,
                        title =
                            if (uiState.refreshState.isRefreshing) "Loading meal plan"
                            else "Nothing planned this week",
                        subtitle =
                            if (uiState.refreshState.errorMessage != null) {
                                "Showing the saved meal plan. Connect to Mealie and try again to refresh it."
                            } else {
                                "Meal plan entries synced from your household's Mealie meal plan show up here."
                            },
                        modifier = Modifier.fillMaxSize(),
                        isLoading = uiState.refreshState.isRefreshing,
                        onRetry = viewModel::refresh,
                    )
                } else {
                    val entriesByDate = uiState.entries.groupBy { it.date }
                    val days = generateSequence(uiState.weekStart) { it.plusDays(1) }.take(7).toList()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(days) { day ->
                            DayCard(
                                day = day,
                                entries = entriesByDate[day.toString()].orEmpty(),
                                onRecipeClick = onRecipeClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    isRefreshing: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 8.dp).size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Row {
            IconButton(onClick = onToday) {
                Icon(Icons.Filled.Today, contentDescription = "Jump to this week")
            }
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next week")
            }
        }
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    entries: List<MealPlanEntryEntity>,
    onRecipeClick: (String, String) -> Unit,
) {
    // Reads through LocalConfiguration (observable, recomposes on a config change) rather than
    // java.util.Locale.getDefault() - see Android Lint's NonObservableLocale check.
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = day.format(DateTimeFormatter.ofPattern("MMMM d")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entries.isEmpty()) {
                Text(
                    text = "Nothing planned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entries.forEach { entry -> MealPlanEntryRow(entry, onRecipeClick) }
                }
            }
        }
    }
}

@Composable
private fun MealPlanEntryRow(entry: MealPlanEntryEntity, onRecipeClick: (String, String) -> Unit) {
    val clickableModifier =
        if (entry.recipeId != null && entry.recipeSlug != null) {
            Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                onRecipeClick(entry.recipeId, entry.recipeSlug)
            }
        } else {
            Modifier
        }
    Row(
        modifier = Modifier.fillMaxWidth().then(clickableModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(32.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entryTypeIcon(entry.entryType),
                contentDescription = entry.entryType,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = entry.entryType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    entry.recipeName ?: entry.title.ifBlank { entry.text.ifBlank { "Untitled" } },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun entryTypeIcon(entryType: String): ImageVector =
    when (entryType) {
        "breakfast" -> Icons.Filled.FreeBreakfast
        "lunch" -> Icons.Filled.LunchDining
        "dinner" -> Icons.Filled.DinnerDining
        "side" -> Icons.Filled.Tapas
        "snack" -> Icons.Filled.Cookie
        "drink" -> Icons.Filled.LocalBar
        "dessert" -> Icons.Filled.Icecream
        else -> Icons.Filled.Restaurant
    }
