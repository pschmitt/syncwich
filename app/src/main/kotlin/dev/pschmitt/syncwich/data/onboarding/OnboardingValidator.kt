package dev.pschmitt.syncwich.data.onboarding

import dev.pschmitt.syncwich.di.ValidationClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/** Why a connection attempt during onboarding didn't succeed, for a precise inline error. */
sealed interface OnboardingError {
    /** The entered server URL isn't a valid absolute HTTP(S) URL. */
    data object MalformedUrl : OnboardingError

    /** The server was reachable but rejected the token (HTTP 401/403). */
    data object Unauthorized : OnboardingError

    /** The server was unreachable (DNS/connect/timeout failure). */
    data object Unreachable : OnboardingError

    /** The server responded, but not with success and not with an auth-shaped error. */
    data class ServerError(val code: Int) : OnboardingError
}

/**
 * Confirms a server URL + API token pair actually authenticates against a reachable Mealie server
 * *before* [dev.pschmitt.syncwich.data.settings.SettingsRepository] persists them. Deliberately
 * uses its own unauthenticated, static-baseurl OkHttpClient ([ValidationClient]) rather than the
 * app's normal Retrofit/OkHttp stack, since that stack's interceptors read the currently *saved*
 * connection - which, during onboarding, is exactly what's being validated before being saved.
 */
@Singleton
class OnboardingValidator @Inject constructor(@ValidationClient private val client: OkHttpClient) {

    suspend fun validate(serverUrl: String, apiToken: String): Result<Unit> {
        val baseUrl =
            serverUrl.trim().trimEnd('/').toHttpUrlOrNull()
                ?: return Result.failure(
                    OnboardingValidationException(OnboardingError.MalformedUrl)
                )

        val request =
            Request.Builder()
                .url(baseUrl.newBuilder().addPathSegments("api/users/self").build())
                .header("Authorization", "Bearer ${apiToken.trim()}")
                .build()

        return try {
            // OkHttpClient.newCall(...).execute() blocks the calling thread on real network I/O -
            // never call it from Dispatchers.Main (viewModelScope's default), or Android throws
            // NetworkOnMainThreadException and crashes the whole app.
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Result.success(Unit)
                        response.code == 401 || response.code == 403 ->
                            Result.failure(
                                OnboardingValidationException(OnboardingError.Unauthorized)
                            )
                        else ->
                            Result.failure(
                                OnboardingValidationException(
                                    OnboardingError.ServerError(response.code)
                                )
                            )
                    }
                }
            }
        } catch (e: IOException) {
            Result.failure(OnboardingValidationException(OnboardingError.Unreachable, e))
        }
    }
}

class OnboardingValidationException(val error: OnboardingError, cause: Throwable? = null) :
    Exception(cause)
