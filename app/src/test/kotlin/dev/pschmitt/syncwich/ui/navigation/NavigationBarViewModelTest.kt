package dev.pschmitt.syncwich.ui.navigation

import dev.pschmitt.syncwich.data.settings.NavigationBarCacheAvailability
import dev.pschmitt.syncwich.data.settings.NavigationBarCacheState
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import dev.pschmitt.syncwich.data.settings.NavigationBarPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationBarViewModelTest {

    @Test
    fun `empty Room-backed caches keep Home and Recipes as the defaults`() = runTest {
        val viewModel = NavigationBarViewModel(FakePreferences(), FakeCache())

        assertEquals(
            listOf(NavigationBarItemKeys.HOME, NavigationBarItemKeys.RECIPES),
            viewModel.visibleItemKeys.first {
                it == listOf(NavigationBarItemKeys.HOME, NavigationBarItemKeys.RECIPES)
            },
        )
    }

    @Test
    fun `cached data restores destinations without needing a network refresh`() = runTest {
        val cache = FakeCache()
        val viewModel = NavigationBarViewModel(FakePreferences(), cache)

        cache.state.value =
            NavigationBarCacheState(
                hasMealPlanData = true,
                hasShoppingLists = true,
                hasCookbooks = true,
            )

        assertEquals(
            listOf(
                NavigationBarItemKeys.HOME,
                NavigationBarItemKeys.RECIPES,
                NavigationBarItemKeys.MEAL_PLAN,
                NavigationBarItemKeys.SHOPPING_LISTS,
                NavigationBarItemKeys.COOKBOOKS,
            ),
            viewModel.visibleItemKeys.first {
                it ==
                    listOf(
                        NavigationBarItemKeys.HOME,
                        NavigationBarItemKeys.RECIPES,
                        NavigationBarItemKeys.MEAL_PLAN,
                        NavigationBarItemKeys.SHOPPING_LISTS,
                        NavigationBarItemKeys.COOKBOOKS,
                    )
            },
        )
    }

    @Test
    fun `explicitly shown destination stays visible with an empty cache`() = runTest {
        val preferences = FakePreferences(shown = setOf(NavigationBarItemKeys.COOKBOOKS))
        val viewModel = NavigationBarViewModel(preferences, FakeCache())

        assertEquals(
            listOf(
                NavigationBarItemKeys.HOME,
                NavigationBarItemKeys.RECIPES,
                NavigationBarItemKeys.COOKBOOKS,
            ),
            viewModel.visibleItemKeys.first {
                it ==
                    listOf(
                        NavigationBarItemKeys.HOME,
                        NavigationBarItemKeys.RECIPES,
                        NavigationBarItemKeys.COOKBOOKS,
                    )
            },
        )
    }

    private class FakePreferences(
        private val order: List<String> = emptyList(),
        private val hidden: Set<String> = emptySet(),
        private val shown: Set<String> = emptySet(),
    ) : NavigationBarPreferences {
        override val navigationBarOrder: Flow<List<String>> = MutableStateFlow(order)
        override val navigationBarHiddenItems: Flow<Set<String>> = MutableStateFlow(hidden)
        override val navigationBarShownItems: Flow<Set<String>> = MutableStateFlow(shown)
    }

    private class FakeCache : NavigationBarCacheAvailability {
        override val state = MutableStateFlow(NavigationBarCacheState())
    }
}
