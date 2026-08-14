package dev.pschmitt.syncwich.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose destinations (see MainActivity/SyncwichNavHost). */
sealed interface Route {
    @Serializable data object Onboarding : Route

    @Serializable data object InitialSync : Route

    @Serializable data object Home : Route

    @Serializable data object Recipes : Route

    @Serializable data object Favorites : Route

    // slug is carried alongside recipeId because Mealie's full-detail endpoint is keyed by slug,
    // not id (see RecipesApi.getRecipeDetailRaw) - the tapped recipe card already has both, so no
    // extra Room lookup is needed to fetch/refresh detail after navigating here.
    @Serializable data class RecipeDetail(val recipeId: String = "", val slug: String) : Route

    /** Empty [recipeId] opens a create draft; a cached id opens an edit draft. */
    @Serializable
    data class RecipeEditor(val recipeId: String = "", val sharedAssetUri: String? = null) : Route

    /** A single recipe's confirmed "I made this" cooking-event history - see SW-30. */
    @Serializable data class RecipeTimeline(val recipeId: String) : Route

    @Serializable data object MealPlan : Route

    @Serializable data object ShoppingLists : Route

    @Serializable data class ShoppingListDetail(val listId: String) : Route

    @Serializable data object Cookbooks : Route

    @Serializable
    data class CookbookDetail(val cookbookId: String = "", val slug: String = "") : Route

    /** Empty [cookbookId] opens a create draft; a cached id opens an edit draft. */
    @Serializable data class CookbookEditor(val cookbookId: String = "") : Route

    @Serializable data object Settings : Route

    @Serializable
    data class SettingsCategory(val category: dev.pschmitt.syncwich.ui.settings.SettingsCategory) :
        Route

    @Serializable data object SettingsConnection : Route
}

/** The five bottom-navigation destinations, in display order. */
enum class TopLevelDestination(
    val key: String,
    val route: Route,
    val label: String,
    val icon: ImageVector,
) {
    HOME(NavigationBarItemKeys.HOME, Route.Home, "Home", Icons.Filled.Home),
    RECIPES(NavigationBarItemKeys.RECIPES, Route.Recipes, "Recipes", Icons.Filled.Restaurant),
    FAVORITES(NavigationBarItemKeys.FAVORITES, Route.Favorites, "Favorites", Icons.Filled.Favorite),
    MEAL_PLAN(
        NavigationBarItemKeys.MEAL_PLAN,
        Route.MealPlan,
        "Meal Plan",
        Icons.Filled.CalendarMonth,
    ),
    SHOPPING_LISTS(
        NavigationBarItemKeys.SHOPPING_LISTS,
        Route.ShoppingLists,
        "Shopping",
        Icons.Filled.ShoppingCart,
    ),
    COOKBOOKS(
        NavigationBarItemKeys.COOKBOOKS,
        Route.Cookbooks,
        "Cookbooks",
        Icons.AutoMirrored.Filled.MenuBook,
    ),
    SETTINGS(NavigationBarItemKeys.SETTINGS, Route.Settings, "Settings", Icons.Filled.Settings),
}
