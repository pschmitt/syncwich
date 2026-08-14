package dev.pschmitt.syncwich.data.api

/**
 * Builds a recipe's cover image URL - confirmed live against a v3.22.0 Mealie instance (SW-3
 * verification): `GET {serverUrl}/api/media/recipes/{recipeId}/images/min-original.webp`, served
 * with no auth required on the verification instance, though Coil's request still carries whatever
 * `Authorization` header the app's OkHttpClient adds since a locked-down deployment may require it.
 *
 * Mealie's `image` field on both the recipe list and detail responses is `null` *or* the literal
 * string `"no image"` when a recipe has no cover - never a URL fragment itself, just a
 * cache-busting marker that a real image exists. Both cases mean "don't render anything".
 */
fun recipeImageUrl(serverUrl: String, recipeId: String, image: String?): String? {
    if (serverUrl.isBlank() || image.isNullOrBlank() || image == "no image") return null
    return "$serverUrl/api/media/recipes/$recipeId/images/min-original.webp"
}
