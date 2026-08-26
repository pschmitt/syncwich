package dev.pschmitt.syncwich.ui.foods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.common.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFoodClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: FoodsViewModel = hiltViewModel(),
) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDeleteFood = remember(foods, pendingDelete) { foods.find { it.id == pendingDelete } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Foods") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New food") },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RefreshErrorBanner(refreshState.errorMessage, onRetry = viewModel::refresh)
                SearchField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Search foods",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                if (foods.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.Filled.Egg,
                        title = "No foods yet",
                        subtitle =
                            "Foods are Mealie's structured ingredient catalog, separate from a " +
                                "recipe's own ingredient text. Add one to get started.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(foods, key = FoodEntity::id) { food ->
                            ListItem(
                                headlineContent = { Text(food.name) },
                                supportingContent =
                                    food.description
                                        .takeIf { it.isNotBlank() }
                                        ?.let { { Text(it) } },
                                trailingContent = {
                                    IconButton(onClick = { pendingDelete = food.id }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete ${food.name}",
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth().clickable { onFoodClick(food.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteFood != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete food?") },
            text = {
                Text(
                    "\"${pendingDeleteFood.name}\" will be removed from Mealie's ingredient " +
                        "catalog. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFood(pendingDeleteFood.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}
