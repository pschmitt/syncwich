package dev.pschmitt.syncwich.data.settings

import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.MealPlanRepository
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class NavigationBarCacheState(
    val hasMealPlanData: Boolean = false,
    val hasShoppingLists: Boolean = false,
    val hasCookbooks: Boolean = false,
)

/** Room-backed feature presence used only to choose bottom-navigation defaults. */
interface NavigationBarCacheAvailability {
    val state: Flow<NavigationBarCacheState>
}

@Singleton
class RoomNavigationBarCacheAvailability
@Inject
constructor(
    mealPlanRepository: MealPlanRepository,
    shoppingListRepository: ShoppingListRepository,
    cookbookRepository: CookbookRepository,
) : NavigationBarCacheAvailability {
    override val state: Flow<NavigationBarCacheState> =
        combine(
            mealPlanRepository.observeHasCachedEntries(),
            shoppingListRepository.observeHasCachedLists(),
            cookbookRepository.observeHasCachedCookbooks(),
        ) { hasMealPlanData, hasShoppingLists, hasCookbooks ->
            NavigationBarCacheState(
                hasMealPlanData = hasMealPlanData,
                hasShoppingLists = hasShoppingLists,
                hasCookbooks = hasCookbooks,
            )
        }
}
