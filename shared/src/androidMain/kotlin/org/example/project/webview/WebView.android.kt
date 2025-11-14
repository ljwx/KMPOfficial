package org.example.project.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

actual class PlatformWebView actual constructor(
    private val config: WebViewConfig
) {
    private var webView: WebView? = null
    
    fun createWebView(context: Context): WebView {
        val webView = WebView(context)
        this.webView = webView
        setupWebView(webView)
        return webView
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        val settings = webView.settings
        
        // 启用 JavaScript
        settings.javaScriptEnabled = config.enableJavaScript
        
        // 启用 DOM Storage
        settings.domStorageEnabled = config.enableDomStorage
        
        // 设置 User Agent
        config.userAgent?.let {
            settings.userAgentString = it
        }
        
        // 其他设置
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        
        // 设置 WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { config.onPageStarted?.invoke(it) }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { config.onPageFinished?.invoke(it) }
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                error?.description?.toString()?.let {
                    config.onError?.invoke(it)
                }
            }
        }
        
        webView.webChromeClient = WebChromeClient()
        
        // 加载初始 URL
        if (config.url.isNotEmpty()) {
            webView.loadUrl(config.url)
        }
    }
    
    actual fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }
    
    actual fun loadHtml(html: String, baseUrl: String?) {
        webView?.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
    }
    
    actual fun goBack(): Boolean {
        val canGoBack = webView?.canGoBack() == true
        if (canGoBack) {
            webView?.goBack()
        }
        return canGoBack
    }
    
    actual fun goForward(): Boolean {
        val canGoForward = webView?.canGoForward() == true
        if (canGoForward) {
            webView?.goForward()
        }
        return canGoForward
    }
    
    actual fun reload() {
        webView?.reload()
    }
    
    actual fun dispose() {
        webView?.destroy()
        webView = null
    }
}

