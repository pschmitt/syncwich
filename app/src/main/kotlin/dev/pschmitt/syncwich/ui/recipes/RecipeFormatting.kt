package dev.pschmitt.syncwich.ui.recipes

import kotlin.math.roundToInt

/** Mealie ratings are 0-5 in whole or half-star steps; drops a trailing ".0" for whole ratings. */
fun formatRating(rating: Double): String {
    val roundedToOneDecimal = (rating * 10).roundToInt() / 10.0
    return if (roundedToOneDecimal == roundedToOneDecimal.toInt().toDouble()) {
        roundedToOneDecimal.toInt().toString()
    } else {
        roundedToOneDecimal.toString()
    }
}
