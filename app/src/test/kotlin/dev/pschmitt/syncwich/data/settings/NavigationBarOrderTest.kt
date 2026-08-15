package dev.pschmitt.syncwich.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationBarOrderTest {

    @Test
    fun `missing preferences preserve natural order`() {
        assertEquals(
            listOf("recipes", "meal_plan", "shopping_lists"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan", "shopping_lists"),
                persisted = emptyList(),
                hidden = emptySet(),
            ),
        )
    }

    @Test
    fun `persisted order drops stale keys and appends new destinations`() {
        assertEquals(
            listOf("shopping_lists", "recipes", "meal_plan", "cookbooks"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan", "shopping_lists", "cookbooks"),
                persisted = listOf("shopping_lists", "missing", "recipes", "shopping_lists"),
                hidden = emptySet(),
            ),
        )
    }

    @Test
    fun `hidden destinations are removed after order is resolved`() {
        assertEquals(
            listOf("cookbooks", "recipes"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan", "shopping_lists", "cookbooks"),
                persisted = listOf("cookbooks", "recipes", "meal_plan", "shopping_lists"),
                hidden = setOf("meal_plan", "shopping_lists"),
            ),
        )
    }

    @Test
    fun `pinned destination remains visible even when persisted as hidden`() {
        assertEquals(
            listOf("recipes"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan"),
                persisted = listOf("meal_plan", "recipes"),
                hidden = setOf("recipes", "meal_plan"),
                pinned = setOf("recipes"),
            ),
        )
    }

    @Test
    fun `all hidden destinations fall back to one natural item`() {
        assertEquals(
            listOf("recipes"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan"),
                persisted = emptyList(),
                hidden = setOf("recipes", "meal_plan"),
            ),
        )
    }

    @Test
    fun `cache-derived defaults hide empty destinations`() {
        assertEquals(
            listOf("recipes"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan", "shopping_lists", "cookbooks"),
                persisted = emptyList(),
                hidden = emptySet(),
                pinned = setOf("recipes"),
                defaultHidden = setOf("meal_plan", "shopping_lists", "cookbooks"),
            ),
        )
    }

    @Test
    fun `explicit show overrides an empty cache default`() {
        assertEquals(
            listOf("recipes", "meal_plan"),
            resolveNavBarOrder(
                natural = listOf("recipes", "meal_plan"),
                persisted = emptyList(),
                hidden = emptySet(),
                pinned = setOf("recipes"),
                defaultHidden = setOf("meal_plan"),
                explicitlyShown = setOf("meal_plan"),
            ),
        )
    }

    @Test
    fun `backup restores destinations that were visible through cache defaults`() {
        val restored =
            SettingsBackupSnapshot(
                navigationBarHiddenItems = setOf(NavigationBarItemKeys.COOKBOOKS),
                navigationBarVisibleItems = setOf(NavigationBarItemKeys.COOKBOOKS),
            )

        val visibility = restored.restoredNavigationBarVisibility()

        assertEquals(emptySet<String>(), visibility.hiddenItems)
        assertEquals(setOf(NavigationBarItemKeys.COOKBOOKS), visibility.shownItems)
    }

    @Test
    fun `settings and favorites can be opted into without changing existing defaults`() {
        val natural =
            listOf(
                NavigationBarItemKeys.HOME,
                NavigationBarItemKeys.RECIPES,
                NavigationBarItemKeys.FAVORITES,
                NavigationBarItemKeys.SETTINGS,
            )

        assertEquals(
            listOf(NavigationBarItemKeys.HOME, NavigationBarItemKeys.RECIPES),
            resolveNavBarOrder(
                natural = natural,
                persisted = emptyList(),
                hidden = emptySet(),
                pinned = setOf(NavigationBarItemKeys.RECIPES),
                defaultHidden =
                    setOf(NavigationBarItemKeys.FAVORITES, NavigationBarItemKeys.SETTINGS),
            ),
        )
        assertEquals(
            listOf(
                NavigationBarItemKeys.HOME,
                NavigationBarItemKeys.RECIPES,
                NavigationBarItemKeys.FAVORITES,
                NavigationBarItemKeys.SETTINGS,
            ),
            resolveNavBarOrder(
                natural = natural,
                persisted = emptyList(),
                hidden = emptySet(),
                pinned = setOf(NavigationBarItemKeys.RECIPES),
                defaultHidden =
                    setOf(NavigationBarItemKeys.FAVORITES, NavigationBarItemKeys.SETTINGS),
                explicitlyShown =
                    setOf(NavigationBarItemKeys.FAVORITES, NavigationBarItemKeys.SETTINGS),
            ),
        )
    }

    @Test
    fun `preference encoding trims and deduplicates keys`() {
        val encoded = navigationBarOrderToString(listOf(" recipes ", "", "meal_plan", "recipes"))

        assertEquals("recipes,meal_plan", encoded)
        assertEquals(listOf("recipes", "meal_plan"), navigationBarOrderFromString(encoded))
    }
}
