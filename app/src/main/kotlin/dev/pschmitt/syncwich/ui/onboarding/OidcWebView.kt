package dev.pschmitt.syncwich.ui.onboarding

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Hosts Mealie's server-side OIDC flow and stops at the web login callback. The callback's code is
 * never rendered or handed to another app; only the URL and the matching WebView cookie jar leave
 * this composable for the server-side exchange.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OidcWebView(
    serverUrl: String,
    startUrl: String,
    onCallback: (callbackUrl: String, cookies: String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val callbackHandler by rememberUpdatedState(onCallback)
    val errorHandler by rememberUpdatedState(onError)
    val webView =
        remember(serverUrl, startUrl) {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient =
                    object : WebViewClient() {
                        private var callbackHandled = false

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = handleUrl(view, request.url)

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                            handleUrl(view, Uri.parse(url))

                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            handleUrl(view, Uri.parse(url))
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                errorHandler("Couldn't load the OIDC sign-in page.")
                            }
                            super.onReceivedError(view, request, error)
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: android.webkit.WebResourceResponse,
                        ) {
                            if (request.isForMainFrame) {
                                errorHandler(
                                    "The Mealie server returned HTTP ${errorResponse.statusCode} " +
                                        "during OIDC sign-in."
                                )
                            }
                            super.onReceivedHttpError(view, request, errorResponse)
                        }

                        private fun handleUrl(view: WebView, url: Uri): Boolean {
                            if (!isOidcCallbackUrl(serverUrl, url.toString())) return false
                            if (callbackHandled) return true
                            callbackHandled = true
                            val cookies = CookieManager.getInstance().getCookie(url.toString()).orEmpty()
                            view.stopLoading()
                            callbackHandler(url.toString(), cookies)
                            return true
                        }
                    }
                loadUrl(startUrl)
            }
        }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    AndroidView(factory = { webView }, modifier = modifier.fillMaxSize())
}

internal fun isOidcCallbackUrl(serverUrl: String, callbackUrl: String): Boolean {
    val base = serverUrl.trim().trimEnd('/').toHttpUrlOrNull() ?: return false
    val callback = callbackUrl.toHttpUrlOrNull() ?: return false
    if (
        base.scheme !in setOf("http", "https") ||
            base.scheme != callback.scheme ||
            base.host != callback.host ||
            base.port != callback.port
    ) {
        return false
    }
    val loginPath = base.encodedPath.trimEnd('/') + "/login"
    return callback.encodedPath == loginPath &&
        (callback.queryParameter("code") != null || callback.queryParameter("error") != null)
}
