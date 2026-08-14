package dev.pschmitt.syncwich.data.settings

/** Stable, persisted keys for the destinations that can appear in the bottom navigation bar. */
object NavigationBarItemKeys {
    const val RECIPES = "recipes"
    const val MEAL_PLAN = "meal_plan"
    const val SHOPPING_LISTS = "shopping_lists"
    const val COOKBOOKS = "cookbooks"
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
): List<String> {
    val naturalDistinct = natural.distinct()
    val available = naturalDistinct.toSet()
    val persistedAvailable = persisted.filter { it in available }.distinct()
    val ordered = buildList {
        addAll(persistedAvailable)
        addAll(naturalDistinct.filterNot { it in persistedAvailable })
    }
    val visible = ordered.filter { it !in hidden || it in pinned }

    return visible.ifEmpty { ordered.take(1) }
}
