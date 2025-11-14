package org.example.project.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 跨平台 WebView Composable 组件
 * 
 * @param url 要加载的 URL
 * @param modifier Modifier
 * @param enableJavaScript 是否启用 JavaScript，默认为 true
 * @param enableDomStorage 是否启用 DOM Storage，默认为 true
 * @param userAgent 自定义 User Agent
 * @param onPageStarted 页面开始加载回调
 * @param onPageFinished 页面加载完成回调
 * @param onError 页面加载错误回调
 */
@Composable
expect fun ComposeWebView(
    url: String,
    modifier: Modifier = Modifier,
    enableJavaScript: Boolean = true,
    enableDomStorage: Boolean = true,
    userAgent: String? = null,
    onPageStarted: ((String) -> Unit)? = null,
    onPageFinished: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
)

