package dev.pschmitt.syncwich.ui.navigation

import android.content.Intent
import android.net.Uri
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface MealieLinkTarget {
    data class Recipe(val slug: String) : MealieLinkTarget

    data class Cookbook(val slug: String) : MealieLinkTarget
}

/** Parses public Mealie web links delivered by ACTION_VIEW or the Android share sheet. */
internal fun parseMealieLink(text: String): MealieLinkTarget? {
    val uri = runCatching { URI(text.trim()) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
    val segments =
        uri.rawPath.orEmpty().trim('/').split('/').filter(String::isNotEmpty).map(::decodeSegment)
    val markerIndex = segments.indexOfLast { it.equals("r", true) || it.equals("c", true) }
    val marker = segments.getOrNull(markerIndex)?.lowercase()
    val slug = segments.getOrNull(markerIndex + 1)?.takeIf(String::isNotBlank) ?: return null
    return when (marker) {
        "r" -> MealieLinkTarget.Recipe(slug)
        "c" -> MealieLinkTarget.Cookbook(slug)
        else -> parseLongFormLink(segments)
    }
}

internal fun parseMealieIntent(intent: Intent?): MealieLinkTarget? {
    val text =
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    return text?.let(::parseMealieLink)
        ?: if (intent?.action == Intent.ACTION_SEND) {
            extractSharedWebUrl(text)?.let(::parseMealieLink)
        } else null
}

internal fun parseSharedAssetUri(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_SEND) return null
    @Suppress("DEPRECATION")
    val streamUri: Uri? =
        intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.clipData?.getItemAt(0)?.uri
    return streamUri?.toString()?.takeIf(String::isNotBlank)
}

/** Returns a shared web URL that is not already a recognized Mealie deep link. */
internal fun parseSharedRecipeUrl(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_SEND) return null
    return parseSharedRecipeText(intent.getStringExtra(Intent.EXTRA_TEXT))
}

internal fun parseSharedRecipeText(text: String?): String? {
    val sharedUrl = extractSharedWebUrl(text) ?: return null
    if (parseMealieLink(sharedUrl) != null) return null
    val uri = runCatching { URI(sharedUrl) }.getOrNull() ?: return null
    return sharedUrl.takeIf { uri.scheme?.lowercase() in setOf("http", "https") }
}

private fun extractSharedWebUrl(text: String?): String? {
    val trimmedText = text?.trim()?.takeIf(String::isNotBlank) ?: return null
    return Regex("https?://[^\\s<>\\\"']+")
        .find(trimmedText)
        ?.value
        ?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
}

private fun parseLongFormLink(segments: List<String>): MealieLinkTarget? {
    val markerIndex = segments.indexOfLast {
        it.equals("recipe", true) ||
            it.equals("recipes", true) ||
            it.equals("cookbook", true) ||
            it.equals("cookbooks", true)
    }
    val marker = segments.getOrNull(markerIndex)?.lowercase()
    val slug = segments.getOrNull(markerIndex + 1)?.takeIf(String::isNotBlank) ?: return null
    return when (marker) {
        "recipe",
        "recipes" -> MealieLinkTarget.Recipe(slug)
        "cookbook",
        "cookbooks" -> MealieLinkTarget.Cookbook(slug)
        else -> null
    }
}

private fun decodeSegment(segment: String): String = runCatching {
    URLDecoder.decode(segment, StandardCharsets.UTF_8.name())
}.getOrDefault(segment)
