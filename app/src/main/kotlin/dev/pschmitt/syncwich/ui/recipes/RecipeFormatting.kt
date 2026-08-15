package dev.pschmitt.syncwich.ui.recipes

import java.math.BigDecimal
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

/** Displays Mealie's serving count without noisy trailing zeroes. */
fun formatServings(servings: Double): String =
    BigDecimal.valueOf(servings).stripTrailingZeros().toPlainString()
