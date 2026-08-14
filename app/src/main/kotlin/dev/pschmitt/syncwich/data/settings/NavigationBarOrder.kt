package dev.pschmitt.syncwich.data.settings

import kotlinx.coroutines.flow.Flow

/** Stable, persisted keys for the destinations that can appear in the bottom navigation bar. */
object NavigationBarItemKeys {
    const val HOME = "home"
    const val RECIPES = "recipes"
    const val FAVORITES = "favorites"
    const val MEAL_PLAN = "meal_plan"
    const val SHOPPING_LISTS = "shopping_lists"
    const val COOKBOOKS = "cookbooks"
    const val SETTINGS = "settings"
}

/** Preferences used by the cache-aware bottom-navigation resolver. */
interface NavigationBarPreferences {
    val navigationBarOrder: Flow<List<String>>
    val navigationBarHiddenItems: Flow<Set<String>>
    val navigationBarShownItems: Flow<Set<String>>
}

fun navigationBarOrderToString(order: List<String>): String =
    order.map(String::trim).filter(String::isNotEmpty).distinct().joinToString(",")

fun navigationBarOrderFromString(value: String?): List<String> =
    value.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).distinct()

/**
 * Merges user preferences with the destinations currently shipped by the app.
 *
 * Unknown persisted keys are discarded, duplicate keys are collapsed, and destinations added in a
 * later version are appended in natural order. Pinned destinations cannot be hidden; the final
 * fallback also guarantees one visible item whenever [natural] is non-empty.
 */
fun resolveNavBarOrder(
    natural: List<String>,
    persisted: List<String>,
    hidden: Set<String>,
    pinned: Set<String> = emptySet(),
    defaultHidden: Set<String> = emptySet(),
    explicitlyShown: Set<String> = emptySet(),
): List<String> {
    val naturalDistinct = natural.distinct()
    val available = naturalDistinct.toSet()
    val persistedAvailable = persisted.filter { it in available }.distinct()
    val ordered = buildList {
        addAll(persistedAvailable)
        addAll(naturalDistinct.filterNot { it in persistedAvailable })
    }
    // An explicit hide wins over every other visibility source. Explicit show only overrides a
    // cache-derived default, so users can opt into an empty destination without changing the
    // meaning of the existing hidden preference.
    val effectiveHidden = (defaultHidden - explicitlyShown) + hidden
    val visible = ordered.filter { it !in effectiveHidden || it in pinned }

    return visible.ifEmpty { ordered.take(1) }
}
