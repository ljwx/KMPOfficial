package org.example.project.webview

/**
 * WebView 配置类
 */
data class WebViewConfig(
    val url: String,
    val enableJavaScript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val userAgent: String? = null,
    val onPageStarted: ((String) -> Unit)? = null,
    val onPageFinished: ((String) -> Unit)? = null,
    val onError: ((String) -> Unit)? = null,
)

/**
 * 跨平台 WebView 接口
 */
expect class PlatformWebView(config: WebViewConfig) {
    /**
     * 加载 URL
     */
    fun loadUrl(url: String)
    
    /**
     * 加载 HTML 内容
     */
    fun loadHtml(html: String, baseUrl: String? = null)
    
    /**
     * 返回上一页
     */
    fun goBack(): Boolean
    
    /**
     * 前进下一页
     */
    fun goForward(): Boolean
    
    /**
     * 重新加载
     */
    fun reload()
    
    /**
     * 清理资源
     */
    fun dispose()
}

