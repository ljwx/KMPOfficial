@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.example.project.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.js.JsAny
import kotlin.random.Random

// 使用 shared 模块中定义的 DOM API，这里只需要扩展类型以支持 style 属性

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
    
    // 生成唯一的容器 ID
    val containerId = remember { "webview-container-${Random.nextLong()}" }
    
    // 使用 Box 作为容器，通过 DisposableEffect 管理 DOM 元素
    Box(modifier = modifier.fillMaxSize()) {
        DisposableEffect(Unit) {
            // 创建容器元素（使用 shared 模块中的 document）
            val container = document.createElement("div")
            container.id = containerId
            container.style.width = "100%"
            container.style.height = "100%"
            
            // 创建 WebView 元素
            val webViewElement = platformWebView.createWebView()
            
            // 将 WebView 附加到容器
            container.appendChild(webViewElement)
            
            // 注意：在 Compose for Web 中，我们需要将容器附加到某个父元素
            // 这里我们暂时无法直接获取 Box 的 DOM 节点
            // 实际使用时可能需要使用其他方式，比如通过全局容器
            
            onDispose {
                // 清理资源
                try {
                    container.removeChild(webViewElement)
                } catch (e: Throwable) {
                    // 忽略错误，元素可能已经被移除
                }
                platformWebView.dispose()
            }
        }
        
        // 当 URL 改变时更新 WebView
        LaunchedEffect(url) {
            platformWebView.loadUrl(url)
        }
    }
}


