package dev.pschmitt.syncwich.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pschmitt.syncwich.ui.cookbooks.CookbooksScreen
import dev.pschmitt.syncwich.ui.mealplan.MealPlanScreen
import dev.pschmitt.syncwich.ui.onboarding.OnboardingScreen
import dev.pschmitt.syncwich.ui.recipes.RecipeDetailScreen
import dev.pschmitt.syncwich.ui.recipes.RecipesScreen
import dev.pschmitt.syncwich.ui.settings.SettingsScreen
import dev.pschmitt.syncwich.ui.shoppinglists.ShoppingListDetailScreen
import dev.pschmitt.syncwich.ui.shoppinglists.ShoppingListsScreen

private data class TopLevelNavItem(
    val destination: TopLevelDestination,
    val icon: ImageVector,
    val label: String,
)

private val topLevelNavItems =
    listOf(
        TopLevelNavItem(TopLevelDestination.RECIPES, Icons.Filled.Restaurant, "Recipes"),
        TopLevelNavItem(TopLevelDestination.MEAL_PLAN, Icons.Filled.CalendarMonth, "Meal Plan"),
        TopLevelNavItem(TopLevelDestination.SHOPPING_LISTS, Icons.Filled.ShoppingCart, "Shopping"),
        TopLevelNavItem(
            TopLevelDestination.COOKBOOKS,
            Icons.AutoMirrored.Filled.MenuBook,
            "Cookbooks",
        ),
    )

/**
 * The app's main scaffold: a Material 3 bottom navigation bar switching between the four v1
 * top-level destinations, plus a Settings entry point reachable from each screen's top app bar
 * (added alongside real per-screen top bars in SW-3+ - not wired yet in this scaffold).
 *
 * @param startDestination [Route.Onboarding] until a server URL + API token are saved, otherwise
 *   [Route.Recipes] - see `MainActivity`, which reads `SettingsRepository.isConfigured` for this.
 */
@Composable
fun SyncwichNavHost(modifier: Modifier = Modifier, startDestination: Route = Route.Recipes) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            // No bottom nav during onboarding - none of the four tabs are usable yet without a
            // connected server.
            val onOnboarding = currentDestination?.hasRoute(Route.Onboarding::class) == true

            if (!onOnboarding) {
                NavigationBar {
                    topLevelNavItems.forEach { item ->
                        val selected =
                            currentDestination?.hierarchy?.any {
                                it.hasRoute(item.destination.route::class)
                            } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onConnected = {
                        navController.navigate(Route.Recipes) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<Route.Recipes> {
                RecipesScreen(
                    onRecipeClick = { recipe ->
                        navController.navigate(Route.RecipeDetail(recipe.id, recipe.slug))
                    }
                )
            }
            composable<Route.MealPlan> {
                MealPlanScreen(
                    onRecipeClick = { recipeId, slug ->
                        navController.navigate(Route.RecipeDetail(recipeId, slug))
                    }
                )
            }
            composable<Route.ShoppingLists> {
                ShoppingListsScreen(
                    onListClick = { listId ->
                        navController.navigate(Route.ShoppingListDetail(listId))
                    }
                )
            }
            composable<Route.Cookbooks> { CookbooksScreen() }
            composable<Route.Settings> { SettingsScreen() }
            composable<Route.RecipeDetail> {
                RecipeDetailScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.ShoppingListDetail> {
                ShoppingListDetailScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
