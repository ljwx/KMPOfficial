package org.example.project.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.example.project.commoncomposable.CommonTopBarBack
import org.example.project.webview.ComposeWebView

/**
 * WebView 使用示例
 */
@Composable
fun WebViewExample() {
    var url by remember { mutableStateOf("https://www.baidu.com") }
    var currentUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { CommonTopBarBack(title = "${currentUrl ?: url}", null) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ComposeWebView(
                url = url,
                modifier = Modifier.fillMaxSize(),
                onPageStarted = { startedUrl ->
                    currentUrl = startedUrl
                },
                onPageFinished = { finishedUrl ->
                    currentUrl = finishedUrl
                },
                onError = { error ->
                    currentUrl = "错误: $error"
                }
            )
        }
    }
}

