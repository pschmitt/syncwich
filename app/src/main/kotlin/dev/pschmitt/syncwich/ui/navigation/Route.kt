package dev.pschmitt.syncwich.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose destinations (see MainActivity/SyncwichNavHost). */
sealed interface Route {
    @Serializable data object Onboarding : Route

    @Serializable data object Recipes : Route

    // slug is carried alongside recipeId because Mealie's full-detail endpoint is keyed by slug,
    // not id (see RecipesApi.getRecipeDetailRaw) - the tapped recipe card already has both, so no
    // extra Room lookup is needed to fetch/refresh detail after navigating here.
    @Serializable data class RecipeDetail(val recipeId: String, val slug: String) : Route

    @Serializable data object MealPlan : Route

    @Serializable data object ShoppingLists : Route

    @Serializable data class ShoppingListDetail(val listId: String) : Route

    @Serializable data object Cookbooks : Route

    @Serializable data class CookbookDetail(val cookbookId: String) : Route

    @Serializable data object Settings : Route
}

/** The four bottom-navigation destinations, in display order. */
enum class TopLevelDestination(val route: Route) {
    RECIPES(Route.Recipes),
    MEAL_PLAN(Route.MealPlan),
    SHOPPING_LISTS(Route.ShoppingLists),
    COOKBOOKS(Route.Cookbooks),
}
