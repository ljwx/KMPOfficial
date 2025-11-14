package org.example.project.webview

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSError
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGRect

actual class PlatformWebView actual constructor(
    private val config: WebViewConfig
) {
    private var webView: WKWebView? = null
    
    @OptIn(ExperimentalForeignApi::class)
    fun createWebView(): WKWebView {
        val configuration = WKWebViewConfiguration().apply {
            // iOS WKWebView 默认启用 JavaScript，如果需要禁用可以通过 preferences 设置
            // preferences.javaScriptEnabled = config.enableJavaScript
            // iOS 默认启用 DOM Storage
        }
        
        // 创建一个零值的 CGRect - WKWebView 的 frame 会在 UIKitView 中自动调整
        // 这里先创建一个简单的 CGRect，实际大小会在 Compose 中设置
        val frame: CValue<CGRect> = memScoped {
            alloc<CGRect>().apply {
                origin.x = 0.0
                origin.y = 0.0
                size.width = 0.0
                size.height = 0.0
            }.readValue()
        }
        val webView = WKWebView(frame = frame, configuration = configuration)
        this.webView = webView
        
        // 设置 User Agent
        config.userAgent?.let {
            webView.customUserAgent = it
        }
        
        // 设置导航代理 - 使用简化的实现避免方法签名冲突
        // 注意：WKWebView 的回调在 Kotlin/Native 中可能有签名冲突
        // 这里先不设置 delegate，后续可以通过其他方式监听页面加载
        // webView.navigationDelegate = ...
        
        // 加载初始 URL
        if (config.url.isNotEmpty()) {
            val url = NSURL(string = config.url)
            val request = NSURLRequest(uRL = url)
            webView.loadRequest(request)
        }
        
        return webView
    }
    
    actual fun loadUrl(url: String) {
        val nsUrl = NSURL(string = url)
        val request = NSURLRequest(uRL = nsUrl)
        webView?.loadRequest(request)
    }
    
    actual fun loadHtml(html: String, baseUrl: String?) {
        val baseNsUrl = baseUrl?.let { NSURL(string = it) }
        webView?.loadHTMLString(html, baseURL = baseNsUrl)
    }
    
    actual fun goBack(): Boolean {
        val canGoBack = webView?.canGoBack == true
        if (canGoBack) {
            webView?.goBack()
        }
        return canGoBack
    }
    
    actual fun goForward(): Boolean {
        val canGoForward = webView?.canGoForward == true
        if (canGoForward) {
            webView?.goForward()
        }
        return canGoForward
    }
    
    actual fun reload() {
        webView?.reload()
    }
    
    actual fun dispose() {
        webView?.navigationDelegate = null
        webView = null
    }
}

