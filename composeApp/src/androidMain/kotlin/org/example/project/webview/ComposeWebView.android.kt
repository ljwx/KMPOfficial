package org.example.project.webview

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun ComposeWebView(
    url: String,
    modifier: Modifier,
    enableJavaScript: Boolean,
    enableDomStorage: Boolean,
    userAgent: String?,
    onPageStarted: ((String) -> Unit)?,
    onPageFinished: ((String) -> Unit)?,
    onError: ((String) -> Unit)?,
) {
    val context = LocalContext.current
    
    val webViewConfig = remember {
        WebViewConfig(
            url = url,
            enableJavaScript = enableJavaScript,
            enableDomStorage = enableDomStorage,
            userAgent = userAgent,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onError = onError
        )
    }
    
    val platformWebView = remember {
        PlatformWebView(webViewConfig)
    }
    
    AndroidView(
        factory = { ctx ->
            platformWebView.createWebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            // 当 URL 改变时更新 WebView
            if (webView.url != url) {
                platformWebView.loadUrl(url)
            }
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            platformWebView.dispose()
        }
    }
}

