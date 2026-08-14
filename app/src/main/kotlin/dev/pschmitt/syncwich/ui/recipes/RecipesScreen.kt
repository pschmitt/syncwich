package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@Composable
fun RecipesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.Restaurant,
        title = "Your recipes will live here",
        subtitle = "Connect a Mealie server to browse, search, and cook fully offline.",
        modifier = modifier,
    )
}
