@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.example.project.webview

import kotlin.js.JsAny

// Kotlin/Wasm 中使用 external 声明访问 DOM API
external interface HTMLIFrameElement : JsAny {
    var src: String
    val style: CSSStyleDeclaration
    fun setAttribute(name: String, value: String)
    fun addEventListener(type: String, listener: (JsAny) -> Unit)
    fun remove()
    val contentWindow: Window?
}

external interface HTMLElement : JsAny {
    val style: CSSStyleDeclaration
    fun setAttribute(name: String, value: String)
    fun addEventListener(type: String, listener: (JsAny) -> Unit)
    fun remove()
    fun appendChild(child: HTMLElement)
    fun removeChild(child: HTMLElement)
    var id: String
}

external interface CSSStyleDeclaration : JsAny {
    var border: String
    var width: String
    var height: String
}

external interface Window : JsAny {
    val location: Location
    val history: History
}

external interface Location : JsAny {
    var href: String
    fun reload()
}

external interface History : JsAny {
    fun back()
    fun forward()
}

external val document: Document

external interface Document : JsAny {
    fun createElement(tagName: String): HTMLElement
    fun getElementById(elementId: String): HTMLElement?
}

actual class PlatformWebView actual constructor(
    private val config: WebViewConfig
) {
    private var iframe: HTMLIFrameElement? = null
    
    fun createWebView(): HTMLElement {
        val iframe = document.createElement("iframe").unsafeCast<HTMLIFrameElement>()
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
        iframe.addEventListener("load") { _ ->
            val url = iframe.contentWindow?.location?.href ?: config.url
            config.onPageFinished?.invoke(url)
        }
        
        // 监听错误事件
        iframe.addEventListener("error") { _ ->
            config.onError?.invoke("Failed to load page")
        }
        
        // 加载初始 URL
        if (config.url.isNotEmpty()) {
            iframe.src = config.url
            config.onPageStarted?.invoke(config.url)
        }
        
        return iframe.unsafeCast<HTMLElement>()
    }
    
    actual fun loadUrl(url: String) {
        iframe?.let {
            it.src = url
            config.onPageStarted?.invoke(url)
        }
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

