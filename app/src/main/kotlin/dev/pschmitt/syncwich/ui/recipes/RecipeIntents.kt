package dev.pschmitt.syncwich.ui.recipes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Builds the public Mealie web URL used by browser/share actions. */
internal fun recipeWebUrl(serverUrl: String, slug: String): String? {
    if (slug.isBlank()) return null
    val base = serverUrl.trimEnd('/').toHttpUrlOrNull() ?: return null
    return base.newBuilder().addPathSegments("g/home/r/$slug").build().toString()
}

internal fun shareRecipe(context: Context, recipeName: String, url: String?) {
    if (url == null) return
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipeName)
            putExtra(Intent.EXTRA_TEXT, "$recipeName\n$url")
        }
    ContextCompat.startActivity(context, Intent.createChooser(shareIntent, "Share recipe"), null)
}

internal fun openRecipeInBrowser(context: Context, url: String?) {
    if (url == null) return
    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), null)
}
