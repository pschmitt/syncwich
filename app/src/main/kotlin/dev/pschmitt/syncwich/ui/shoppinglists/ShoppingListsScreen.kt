package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    onListClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListsViewModel = hiltViewModel(),
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Shopping lists") }) },
    ) { innerPadding ->
        if (lists.isEmpty()) {
            PlaceholderScreen(
                icon = Icons.Filled.ShoppingCart,
                title = "No shopping lists yet",
                subtitle = "Your household's shopping lists will show up here once synced.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(lists, key = { it.id }) { list ->
                    ShoppingListRow(list = list, onClick = { onListClick(list.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(list: ShoppingListEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(list.name) },
        leadingContent = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
