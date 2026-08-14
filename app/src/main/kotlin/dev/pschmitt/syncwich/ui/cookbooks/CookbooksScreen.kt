package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@Composable
fun CookbooksScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        title = "Cookbooks",
        subtitle = "Browse your Mealie cookbooks and their curated recipe collections.",
        modifier = modifier,
    )
}
