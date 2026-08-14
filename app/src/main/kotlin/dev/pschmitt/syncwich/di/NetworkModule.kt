package dev.pschmitt.syncwich.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pschmitt.syncwich.BuildConfig
import dev.pschmitt.syncwich.data.api.AuthInterceptor
import dev.pschmitt.syncwich.data.api.CookbooksApi
import dev.pschmitt.syncwich.data.api.DynamicBaseUrlInterceptor
import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.RecipesApi
import dev.pschmitt.syncwich.data.api.UsersApi
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Unauthenticated, static-baseurl client used only by onboarding to validate a server URL/API token
 * pair *before* they're persisted - see `OnboardingValidator`. The default [OkHttpClient] is wired
 * to [DynamicBaseUrlInterceptor]/[AuthInterceptor], both of which read the currently *saved*
 * connection, so it can't be reused to test unsaved credentials.
 */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ValidationClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                // Body-level logging in debug is invaluable for diagnosing a self-hosted Mealie
                // quirk; release only logs the request line + headers (redacted below) so a bug
                // report doesn't leak recipe/household data. Never logs the bearer token itself:
                // HttpLoggingInterceptor redacts any header explicitly marked sensitive.
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            }
        return OkHttpClient.Builder()
            // Rewrites scheme/host/path to the configured instance - added first so auth/logging
            // see the real request.
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @ValidationClient
    fun provideValidationOkHttpClient(): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Placeholder - DynamicBaseUrlInterceptor rewrites every request to the configured
            // Mealie instance at request time, so this host is never actually contacted.
            .baseUrl("http://mealie.invalid/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideRecipesApi(retrofit: Retrofit): RecipesApi = retrofit.create(RecipesApi::class.java)

    @Provides
    @Singleton
    fun provideOrganizersApi(retrofit: Retrofit): OrganizersApi =
        retrofit.create(OrganizersApi::class.java)

    @Provides
    @Singleton
    fun provideCookbooksApi(retrofit: Retrofit): CookbooksApi =
        retrofit.create(CookbooksApi::class.java)
}
