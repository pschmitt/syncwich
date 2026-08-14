package dev.pschmitt.syncwich.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationTest {

    @Test
    fun `home is the only destination that resets its child stack`() {
        assertTrue(shouldResetHomeStack(TopLevelDestination.HOME))
        assertFalse(shouldResetHomeStack(TopLevelDestination.RECIPES))
        assertFalse(shouldResetHomeStack(TopLevelDestination.SETTINGS))
    }

    @Test
    fun `home tap is a no-op when home is already selected`() {
        assertFalse(shouldNavigateToHome(isAlreadyOnHome = true))
        assertTrue(shouldNavigateToHome(isAlreadyOnHome = false))
    }

    @Test
    fun `top-level tap returns to the list from a detail destination`() {
        assertTrue(shouldNavigateToTopLevel(isAlreadyOnList = false))
        assertFalse(shouldNavigateToTopLevel(isAlreadyOnList = true))
    }

    @Test
    fun `nested destinations keep their parent navigation item active`() {
        assertTrue(
            TopLevelDestination.COOKBOOKS.routeTypes.contains(Route.CookbookDetail::class)
        )
        assertTrue(TopLevelDestination.RECIPES.routeTypes.contains(Route.RecipeDetail::class))
        assertTrue(TopLevelDestination.SHOPPING_LISTS.routeTypes.contains(Route.ShoppingListDetail::class))
        assertTrue(TopLevelDestination.SETTINGS.routeTypes.contains(Route.SettingsCategory::class))
    }
}
