package org.example.project.webview

import org.w3c.dom.Document

actual class PlatformWebView actual constructor(
    private val config: WebViewConfig
) {
    private var webView: Any? = null
    private var webEngine: Any? = null
    
    init {
        // 延迟初始化，使用反射加载 JavaFX 类
        try {
            initializeWebView()
        } catch (e: Exception) {
            // JavaFX 不可用时，webView 和 webEngine 保持为 null
            // 调用方法时会检查并抛出有意义的错误
        }
    }
    
    private fun initializeWebView() {
        // 使用反射加载 JavaFX WebView 类
        val webViewClass = Class.forName("javafx.scene.web.WebView")
        val webViewInstance = webViewClass.getDeclaredConstructor().newInstance()
        this.webView = webViewInstance
        
        // 获取 WebEngine
        val engineMethod = webViewClass.getMethod("getEngine")
        val engineInstance = engineMethod.invoke(webViewInstance)
        this.webEngine = engineInstance
        
        val engineClass = engineInstance.javaClass
        
        // 启用 JavaScript
        val javaScriptEnabledSetter = engineClass.getMethod("setJavaScriptEnabled", Boolean::class.java)
        javaScriptEnabledSetter.invoke(engineInstance, config.enableJavaScript)
        
        // 设置 User Agent
        config.userAgent?.let {
            val userAgentSetter = engineClass.getMethod("setUserAgent", String::class.java)
            userAgentSetter.invoke(engineInstance, it)
        }
        
        // 设置页面加载监听
        val locationPropertyMethod = engineClass.getMethod("locationProperty")
        val locationProperty = locationPropertyMethod.invoke(engineInstance)
        val locationPropertyClass = locationProperty.javaClass
        
        // 创建 ChangeListener
        val changeListenerClass = Class.forName("javafx.beans.value.ChangeListener")
        val listener = java.lang.reflect.Proxy.newProxyInstance(
            changeListenerClass.classLoader,
            arrayOf(changeListenerClass)
        ) { _, method, args ->
            if (method.name == "changed") {
                val oldValue = args?.get(1) as? String?
                val newValue = args?.get(2) as? String?
                if (oldValue != newValue && newValue != null) {
                    config.onPageStarted?.invoke(newValue)
                }
            }
            null
        }
        
        val addListenerMethod = locationPropertyClass.getMethod(
            "addListener",
            changeListenerClass
        )
        addListenerMethod.invoke(locationProperty, listener)
        
        // 监听文档变化（页面加载完成）
        val documentPropertyMethod = engineClass.getMethod("documentProperty")
        val documentProperty = documentPropertyMethod.invoke(engineInstance)
        val documentPropertyClass = documentProperty.javaClass
        
        val docListener = java.lang.reflect.Proxy.newProxyInstance(
            changeListenerClass.classLoader,
            arrayOf(changeListenerClass)
        ) { _, method, args ->
            if (method.name == "changed") {
                val newDoc = args?.get(2)
                if (newDoc != null) {
                    val locationGetter = engineClass.getMethod("getLocation")
                    val location = locationGetter.invoke(engineInstance) as? String ?: ""
                    config.onPageFinished?.invoke(location)
                }
            }
            null
        }
        
        val addDocListenerMethod = documentPropertyClass.getMethod(
            "addListener",
            changeListenerClass
        )
        addDocListenerMethod.invoke(documentProperty, docListener)
        
        // 加载初始 URL
        if (config.url.isNotEmpty()) {
            val loadMethod = engineClass.getMethod("load", String::class.java)
            loadMethod.invoke(engineInstance, config.url)
        }
    }
    
    private fun checkJavaFXAvailable() {
        if (webEngine == null) {
            throw IllegalStateException(
                "JavaFX is not available. Please ensure JavaFX modules are in the module path at runtime."
            )
        }
    }
    
    actual fun loadUrl(url: String) {
        checkJavaFXAvailable()
        val engineClass = webEngine!!.javaClass
        val loadMethod = engineClass.getMethod("load", String::class.java)
        loadMethod.invoke(webEngine, url)
    }
    
    actual fun loadHtml(html: String, baseUrl: String?) {
        checkJavaFXAvailable()
        val engineClass = webEngine!!.javaClass
        val loadContentMethod = engineClass.getMethod("loadContent", String::class.java, String::class.java)
        loadContentMethod.invoke(webEngine, html, "text/html")
    }
    
    actual fun goBack(): Boolean {
        checkJavaFXAvailable()
        val engineClass = webEngine!!.javaClass
        val getHistoryMethod = engineClass.getMethod("getHistory")
        val history = getHistoryMethod.invoke(webEngine)
        val historyClass = history.javaClass
        
        val getCurrentIndexMethod = historyClass.getMethod("getCurrentIndex")
        val currentIndex = getCurrentIndexMethod.invoke(history) as Int
        
        val canGoBack = currentIndex > 0
        if (canGoBack) {
            val goMethod = historyClass.getMethod("go", Int::class.java)
            goMethod.invoke(history, -1)
        }
        return canGoBack
    }
    
    actual fun goForward(): Boolean {
        checkJavaFXAvailable()
        val engineClass = webEngine!!.javaClass
        val getHistoryMethod = engineClass.getMethod("getHistory")
        val history = getHistoryMethod.invoke(webEngine)
        val historyClass = history.javaClass
        
        val getCurrentIndexMethod = historyClass.getMethod("getCurrentIndex")
        val currentIndex = getCurrentIndexMethod.invoke(history) as Int
        
        val getEntriesMethod = historyClass.getMethod("getEntries")
        val entries = getEntriesMethod.invoke(history) as? java.util.List<*> ?: emptyList<Any>()
        val maxIndex = entries.size - 1
        
        val canGoForward = currentIndex < maxIndex
        if (canGoForward) {
            val goMethod = historyClass.getMethod("go", Int::class.java)
            goMethod.invoke(history, 1)
        }
        return canGoForward
    }
    
    actual fun reload() {
        checkJavaFXAvailable()
        val engineClass = webEngine!!.javaClass
        val reloadMethod = engineClass.getMethod("reload")
        reloadMethod.invoke(webEngine)
    }
    
    actual fun dispose() {
        if (webEngine != null) {
            try {
                val engineClass = webEngine!!.javaClass
                val loadMethod = engineClass.getMethod("load", String::class.java)
                loadMethod.invoke(webEngine, "about:blank")
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }
        webView = null
        webEngine = null
    }
    
    /**
     * 获取 JavaFX WebView 实例（用于 Compose Desktop 集成）
     * 注意：此方法返回 Any 类型，需要在使用时进行类型转换
     */
    fun getWebView(): Any? = webView
}
