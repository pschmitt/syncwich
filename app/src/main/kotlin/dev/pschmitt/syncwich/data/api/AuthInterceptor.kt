package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** Adds the Mealie bearer token only to requests targeting the configured Mealie origin. */
class AuthInterceptor @Inject constructor(private val settingsRepository: SettingsRepository) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = settingsRepository.credentials.value
        val token = credentials.apiToken
        val request = chain.request()
        val configuredOrigin = credentials.serverUrl.toHttpUrlOrNull()
        val authorized =
            if (
                token.isBlank() ||
                    configuredOrigin == null ||
                    !shouldAttachMealieAuth(request.url, configuredOrigin)
            ) {
                request
            } else request.newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(authorized)
    }
}

internal fun sameHttpOrigin(first: HttpUrl, second: HttpUrl): Boolean =
    first.scheme == second.scheme && first.host == second.host && first.port == second.port

internal fun shouldAttachMealieAuth(requestUrl: HttpUrl, configuredUrl: HttpUrl): Boolean =
    sameHttpOrigin(requestUrl, configuredUrl)
