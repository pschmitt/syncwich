package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A menu of Mealie's data-catalog verticals (Foods/Categories/Tags/Tools today; Units/Labels/
 * Recipe Actions land here as their own rows once built - see SW-139/SW-140 in TODO.md), each
 * reached from Settings' "Data Management" row rather than the flat top-level Settings list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    onFoodsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onTagsClick: () -> Unit,
    onToolsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
        ) {
            item {
                DataManagementRow(
                    icon = Icons.Filled.Egg,
                    title = "Foods",
                    subtitle = "Mealie's structured ingredient catalog",
                    onClick = onFoodsClick,
                )
            }
            item {
                DataManagementRow(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    subtitle = "Organize your recipes into categories",
                    onClick = onCategoriesClick,
                )
            }
            item {
                DataManagementRow(
                    icon = Icons.Filled.Label,
                    title = "Tags",
                    subtitle = "Label recipes for easier searching",
                    onClick = onTagsClick,
                )
            }
            item {
                DataManagementRow(
                    icon = Icons.Filled.Build,
                    title = "Tools",
                    subtitle = "Kitchen equipment recipes can call for",
                    onClick = onToolsClick,
                )
            }
        }
    }
}

@Composable
private fun DataManagementRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SettingsListItem(
        modifier =
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).semantics {
                contentDescription = "$title: $subtitle"
                role = Role.Button
            },
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
    )
}
