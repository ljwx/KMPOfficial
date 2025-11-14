package org.example.project.webview

import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlinx.browser.document

actual class PlatformWebView actual constructor(
    private val config: WebViewConfig
) {
    private var iframe: HTMLIFrameElement? = null
    
    fun createWebView(): HTMLElement {
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        this.iframe = iframe
        
        // 设置 iframe 属性
        iframe.style.border = "none"
        iframe.style.width = "100%"
        iframe.style.height = "100%"
        
        // 设置 sandbox 属性以启用 JavaScript（如果需要）
        if (config.enableJavaScript) {
            iframe.setAttribute("sandbox", "allow-scripts allow-same-origin allow-forms allow-popups")
        }
        
        // 监听加载事件
        iframe.addEventListener("load", { _: Event ->
            val url = iframe.contentWindow?.location?.href ?: config.url
            config.onPageFinished?.invoke(url)
        })
        
        // 监听错误事件
        iframe.addEventListener("error", { _: Event ->
            config.onError?.invoke("Failed to load page")
        })
        
        // 加载初始 URL
        if (config.url.isNotEmpty()) {
            iframe.src = config.url
            config.onPageStarted?.invoke(config.url)
        }
        
        return iframe
    }
    
    actual fun loadUrl(url: String) {
        iframe?.src = url
        config.onPageStarted?.invoke(url)
    }
    
    actual fun loadHtml(html: String, baseUrl: String?) {
        // 对于 iframe，我们需要使用 srcdoc 属性
        iframe?.setAttribute("srcdoc", html)
        config.onPageStarted?.invoke(baseUrl ?: "about:blank")
    }
    
    actual fun goBack(): Boolean {
        // iframe 本身不支持历史记录导航
        // 可以通过 contentWindow.history 尝试
        return try {
            iframe?.contentWindow?.history?.back()
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    actual fun goForward(): Boolean {
        return try {
            iframe?.contentWindow?.history?.forward()
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    actual fun reload() {
        iframe?.contentWindow?.location?.reload()
    }
    
    actual fun dispose() {
        iframe?.remove()
        iframe = null
    }
}

