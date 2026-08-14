package dev.pschmitt.syncwich.data.api

import java.nio.file.Files
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConditionalGetInterceptorTest {

    @Test
    fun `revalidates cached GETs and serves the cached body after a 304`() {
        val server = MockWebServer()
        val cacheDirectory = Files.createTempDirectory("syncwich-http-cache").toFile()
        val cache = Cache(cacheDirectory, 1024 * 1024)
        try {
            server.enqueue(
                MockResponse()
                    .setHeader("Cache-Control", "max-age=3600")
                    .setHeader("ETag", "\"recipe-v1\"")
                    .setBody("cached recipe")
            )
            server.enqueue(
                MockResponse()
                    .setHeader("ETag", "recipe-v2")
                    .setBody("updated recipe")
            )
            server.start()
            val client =
                OkHttpClient.Builder()
                    .cache(cache)
                    .addInterceptor(ConditionalGetInterceptor())
                    .build()
            val request = Request.Builder().url(server.url("api/recipes")).build()

            client.newCall(request).execute().use { response ->
                assertEquals("cached recipe", response.body.string())
            }
            client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
                assertEquals("updated recipe", response.body.string())
            }

            assertEquals(2, server.requestCount)
            assertNotNull(server.takeRequest().getHeader("Cache-Control"))
            assertEquals("\"recipe-v1\"", server.takeRequest().getHeader("If-None-Match"))
        } finally {
            cache.close()
            server.shutdown()
            cacheDirectory.deleteRecursively()
        }
    }
}
