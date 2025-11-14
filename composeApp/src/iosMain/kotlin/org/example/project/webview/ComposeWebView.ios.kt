package org.example.project.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
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
    
    UIKitView(
        factory = {
            @OptIn(ExperimentalForeignApi::class)
            platformWebView.createWebView()
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            // 当 URL 改变时更新 WebView
            val currentUrl = (webView as? platform.WebKit.WKWebView)?.URL?.absoluteString
            if (currentUrl != url) {
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

