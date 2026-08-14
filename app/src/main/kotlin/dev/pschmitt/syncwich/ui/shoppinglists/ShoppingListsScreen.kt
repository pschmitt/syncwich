package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@Composable
fun ShoppingListsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.ShoppingCart,
        title = "Shopping lists",
        subtitle = "Your household's shopping lists, available offline once synced.",
        modifier = modifier,
    )
}
