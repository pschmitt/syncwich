package dev.pschmitt.syncwich.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Makes cacheable GET responses revalidate with their origin instead of being used indefinitely.
 * OkHttp then adds `If-None-Match`/`If-Modified-Since` from the cached response and merges a 304
 * into the cached body. Servers without validators simply fall back to an ordinary 200 response.
 */
class ConditionalGetInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" || request.header("Cache-Control") != null) {
            return chain.proceed(request)
        }
        // max-age=0 keeps the cached response eligible for a conditional request. OkHttp treats a
        // literal no-cache request as FORCE_NETWORK and therefore cannot attach stored validators.
        return chain.proceed(request.newBuilder().header("Cache-Control", "max-age=0").build())
    }
}
