package dev.pschmitt.syncwich.data.onboarding

import dev.pschmitt.syncwich.data.api.dto.MealieAuthTokenDto
import dev.pschmitt.syncwich.data.settings.MealieAuthMethod
import dev.pschmitt.syncwich.data.settings.MealieCredentials
import java.io.IOException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Completes Mealie's server-side OIDC flow. Mealie sends the identity provider back to its web
 * `/login?code=...` page, so the in-app login window passes that URL and the OIDC state cookie here
 * instead of attempting to implement the provider protocol itself.
 */
@Singleton
class OidcAuthClient
@Inject
constructor(
    @dev.pschmitt.syncwich.di.ValidationClient private val client: OkHttpClient,
    private val json: Json,
) {

    fun authorizationUrl(serverUrl: String): Result<String> {
        val baseUrl =
            parseBaseUrl(serverUrl)
                ?: return Result.failure(OidcLoginException("Enter a valid server URL"))
        return Result.success(appendPath(baseUrl, "api/auth/oauth").toString())
    }

    suspend fun exchangeCallback(
        serverUrl: String,
        callbackUrl: String,
        cookies: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val baseUrl = parseBaseUrl(serverUrl)
            val callback = callbackUrl.toHttpUrlOrNull()
            if (baseUrl == null || callback == null || !sameOrigin(baseUrl, callback)) {
                return@withContext Result.failure(OidcLoginException("Invalid OIDC callback"))
            }
            val expectedLoginPath = appendPath(baseUrl, "login").encodedPath
            if (callback.encodedPath != expectedLoginPath) {
                return@withContext Result.failure(OidcLoginException("Invalid OIDC callback"))
            }
            if (callback.queryParameter("error") != null) {
                return@withContext Result.failure(
                    OidcLoginException("The identity provider rejected the sign-in request.")
                )
            }
            if (
                callback.queryParameter("code") == null || callback.queryParameter("state") == null
            ) {
                return@withContext Result.failure(
                    OidcLoginException("OIDC callback was incomplete")
                )
            }

            val requestUrl =
                appendPath(baseUrl, "api/auth/oauth/callback")
                    .newBuilder()
                    .also { builder ->
                        for (index in 0 until callback.querySize) {
                            builder.addQueryParameter(
                                callback.queryParameterName(index),
                                callback.queryParameterValue(index),
                            )
                        }
                    }
                    .build()
            val requestBuilder = Request.Builder().url(requestUrl)
            if (cookies.isNotBlank()) requestBuilder.header("Cookie", cookies)
            executeTokenRequest(requestBuilder.get().build())
        }

    /** Refreshes a Mealie OIDC JWT without returning to the identity provider. */
    suspend fun refresh(credentials: MealieCredentials): Result<String> =
        withContext(Dispatchers.IO) {
            val baseUrl = parseBaseUrl(credentials.serverUrl)
            if (baseUrl == null || credentials.authMethod != MealieAuthMethod.Oidc) {
                return@withContext Result.failure(OidcLoginException("OIDC is not configured"))
            }
            val request =
                Request.Builder()
                    .url(appendPath(baseUrl, "api/auth/refresh"))
                    .header("Authorization", "Bearer ${credentials.apiToken}")
                    .get()
                    .build()
            executeTokenRequest(request)
        }

    /** Returns true when an OIDC JWT should be renewed before the next network refresh. */
    fun isExpiringSoon(token: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val expiresAt =
            runCatching {
                val payload = token.split('.').getOrNull(1) ?: return false
                val decoded = Base64.getUrlDecoder().decode(payload.withBase64Padding())
                json
                    .parseToJsonElement(decoded.decodeToString())
                    .jsonObject["exp"]
                    ?.toString()
                    ?.toLongOrNull()
            }
                .getOrNull() ?: return true
        return expiresAt * 1_000L - nowMillis <= REFRESH_WINDOW_MILLIS
    }

    private fun executeTokenRequest(request: Request): Result<String> =
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        OidcLoginException(
                            "Mealie rejected the OIDC session (HTTP ${response.code})"
                        )
                    )
                }
                val body = response.body?.string().orEmpty()
                val token = json.decodeFromString<MealieAuthTokenDto>(body).accessToken
                if (token.isBlank()) {
                    Result.failure(OidcLoginException("Mealie returned an empty OIDC token"))
                } else {
                    Result.success(token)
                }
            }
        } catch (error: IOException) {
            Result.failure(
                OidcLoginException("Couldn't reach Mealie to finish OIDC sign-in", error)
            )
        } catch (error: Exception) {
            Result.failure(OidcLoginException("Mealie returned an invalid OIDC token", error))
        }

    private fun parseBaseUrl(serverUrl: String): HttpUrl? =
        serverUrl.trim().trimEnd('/').toHttpUrlOrNull()?.takeIf { it.scheme in HTTP_SCHEMES }

    private fun appendPath(baseUrl: HttpUrl, path: String): HttpUrl =
        baseUrl.newBuilder().addPathSegments(path).build()

    private fun sameOrigin(first: HttpUrl, second: HttpUrl): Boolean =
        first.scheme == second.scheme && first.host == second.host && first.port == second.port

    private companion object {
        val HTTP_SCHEMES = setOf("http", "https")
        const val REFRESH_WINDOW_MILLIS = 5 * 60 * 1_000L
    }
}

private fun String.withBase64Padding(): String = this + "=".repeat((4 - length % 4) % 4)

class OidcLoginException(message: String, cause: Throwable? = null) : Exception(message, cause)
