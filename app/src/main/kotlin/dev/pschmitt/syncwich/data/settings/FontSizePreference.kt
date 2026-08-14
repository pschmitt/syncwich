package dev.pschmitt.syncwich.data.settings

const val DEFAULT_FONT_SCALE = 1f
const val MIN_FONT_SCALE = 0.85f
const val MAX_FONT_SCALE = 1.30f
const val FONT_SCALE_STEPS = 8

fun sanitizeFontScale(scale: Float): Float =
    if (scale.isFinite()) scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
    else DEFAULT_FONT_SCALE
