package dev.pschmitt.syncwich.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.pschmitt.syncwich.ui.cookbooks.CookbookDetailScreen
import dev.pschmitt.syncwich.ui.cookbooks.CookbookEditorScreen
import dev.pschmitt.syncwich.ui.cookbooks.CookbooksScreen
import dev.pschmitt.syncwich.ui.initialsync.InitialSyncScreen
import dev.pschmitt.syncwich.ui.home.HomeScreen
import dev.pschmitt.syncwich.ui.mealplan.MealPlanScreen
import dev.pschmitt.syncwich.ui.onboarding.OnboardingScreen
import dev.pschmitt.syncwich.ui.recipes.RecipeDetailScreen
import dev.pschmitt.syncwich.ui.recipes.RecipeTimelineScreen
import dev.pschmitt.syncwich.ui.recipes.RecipesScreen
import dev.pschmitt.syncwich.ui.settings.SettingsScreen
import dev.pschmitt.syncwich.ui.settings.ConnectionSettingsScreen
import dev.pschmitt.syncwich.ui.settings.SettingsCategoryScreen
import dev.pschmitt.syncwich.ui.shoppinglists.ShoppingListDetailScreen
import dev.pschmitt.syncwich.ui.shoppinglists.ShoppingListsScreen

private data class TopLevelNavItem(
    val destination: TopLevelDestination,
    val icon: ImageVector,
    val label: String,
)

private val topLevelNavItems =
    listOf(
        TopLevelNavItem(TopLevelDestination.HOME, Icons.Filled.Home, "Home"),
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
 * The app's main scaffold: a Material 3 bottom navigation bar switching between the five v1
 * top-level destinations, plus a Settings entry point reachable from each screen's own top app
 * bar (see SW-11 - each top-level screen owns its `TopAppBar`, not this outer `Scaffold`).
 *
 * @param startDestination [Route.Onboarding] until a server URL + API token are saved, otherwise
 *   [Route.Home] - see `MainActivity`, which reads `SettingsRepository.isConfigured` for this.
 */
@Composable
fun SyncwichNavHost(modifier: Modifier = Modifier, startDestination: Route = Route.Home) {
    val navController = rememberNavController()
    val navigationBarViewModel: NavigationBarViewModel = hiltViewModel()
    val visibleNavBarItemKeys by
        navigationBarViewModel.visibleItemKeys.collectAsStateWithLifecycle()
    val resolvedTopLevelNavItems =
        visibleNavBarItemKeys.mapNotNull { key ->
            topLevelNavItems.firstOrNull { it.destination.key == key }
        }

    Scaffold(
        modifier = modifier,
        // Each destination owns its content scaffold and applies the status-bar inset alongside
        // its own top app bar. Applying the outer scaffold's default system-bar insets here would
        // offset that whole destination a second time; NavigationBar still contributes its full
        // height (including the navigation-bar inset) to the content padding below.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            // No bottom nav during onboarding - none of the four tabs are usable yet without a
            // connected server.
            val onOnboarding =
                currentDestination?.hasRoute(Route.Onboarding::class) == true ||
                    currentDestination?.hasRoute(Route.InitialSync::class) == true

            if (!onOnboarding) {
                NavigationBar {
                    resolvedTopLevelNavItems.forEach { item ->
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
                        navController.navigate(Route.InitialSync) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<Route.InitialSync> {
                InitialSyncScreen(
                    onFinished = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.InitialSync) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCancel = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.InitialSync) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<Route.Home> {
                HomeScreen(
                    onRecipeClick = { recipe ->
                        navController.navigate(Route.RecipeDetail(recipe.id, recipe.slug))
                    },
                    onRecipesClick = { navController.navigate(Route.Recipes) },
                    onMealPlanClick = { navController.navigate(Route.MealPlan) },
                    onShoppingListsClick = { navController.navigate(Route.ShoppingLists) },
                    onCookbooksClick = { navController.navigate(Route.Cookbooks) },
                    onCookbookClick = { cookbookId ->
                        navController.navigate(Route.CookbookDetail(cookbookId))
                    },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Recipes> {
                RecipesScreen(
                    onRecipeClick = { recipe ->
                        navController.navigate(Route.RecipeDetail(recipe.id, recipe.slug))
                    },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.MealPlan> {
                MealPlanScreen(
                    onRecipeClick = { recipeId, slug ->
                        navController.navigate(Route.RecipeDetail(recipeId, slug))
                    },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.ShoppingLists> {
                ShoppingListsScreen(
                    onListClick = { listId ->
                        navController.navigate(Route.ShoppingListDetail(listId))
                    },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Cookbooks> {
                CookbooksScreen(
                    onCookbookClick = { cookbookId ->
                        navController.navigate(Route.CookbookDetail(cookbookId))
                    },
                    onCreateClick = { navController.navigate(Route.CookbookEditor()) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.CookbookDetail> {
                CookbookDetailScreen(
                    onRecipeClick = { recipeId, slug ->
                        navController.navigate(Route.RecipeDetail(recipeId, slug))
                    },
                    onEditClick = { cookbookId ->
                        navController.navigate(Route.CookbookEditor(cookbookId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.CookbookEditor> {
                CookbookEditorScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onCategoryClick = { category ->
                        navController.navigate(Route.SettingsCategory(category))
                    },
                )
            }
            composable<Route.SettingsCategory> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.SettingsCategory>()
                SettingsCategoryScreen(
                    category = route.category,
                    onBack = { navController.popBackStack() },
                    onChangeConnection = { navController.navigate(Route.SettingsConnection) },
                    onSignedOut = {
                        navController.navigate(Route.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.SettingsConnection> {
                ConnectionSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    viewModel = hiltViewModel(),
                )
            }
            composable<Route.RecipeDetail> {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTimeline = { recipeId ->
                        navController.navigate(Route.RecipeTimeline(recipeId))
                    },
                )
            }
            composable<Route.RecipeTimeline> {
                RecipeTimelineScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.ShoppingListDetail> {
                ShoppingListDetailScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
