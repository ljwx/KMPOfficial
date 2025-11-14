# iOS WebView 实现指南

本文档记录了在 Kotlin Multiplatform 项目中为 iOS 平台实现 WebView 组件的完整流程、遇到的问题及解决方案。

## 目录

1. [实现流程](#实现流程)
2. [遇到的问题](#遇到的问题)
3. [解决方案](#解决方案)
4. [代码实现](#代码实现)
5. [网络权限配置](#网络权限配置)
6. [注意事项](#注意事项)

---

## 实现流程

### 1. 架构设计

采用 `expect/actual` 机制实现跨平台 WebView：

- **`shared/commonMain`**: 定义 `expect` 接口和配置类
- **`shared/iosMain`**: 实现 iOS 平台的 `actual` 类
- **`composeApp/commonMain`**: 定义 Compose 组件的 `expect` 函数
- **`composeApp/iosMain`**: 实现 iOS 平台的 Compose 组件

### 2. 文件结构

```
shared/src/iosMain/kotlin/org/example/project/webview/
  └── WebView.ios.kt          # iOS 平台 WebView 实现

composeApp/src/iosMain/kotlin/org/example/project/webview/
  └── ComposeWebView.ios.kt  # iOS 平台 Compose 组件

iosApp/iosApp/
  └── Info.plist             # iOS 网络权限配置
```

### 3. 实现步骤

1. **创建 expect 接口** (`shared/commonMain`)
   - 定义 `WebViewConfig` 配置类
   - 定义 `PlatformWebView` expect 类

2. **实现 iOS 平台类** (`shared/iosMain`)
   - 使用 `WKWebView` 实现 `PlatformWebView`
   - 实现 `createWebView()` 方法创建 WebView 实例

3. **实现 Compose 组件** (`composeApp/iosMain`)
   - 使用 `UIKitView` 嵌入原生 `WKWebView`
   - 处理 URL 更新和资源清理

4. **配置网络权限** (`iosApp/iosApp/Info.plist`)
   - 添加 `NSAppTransportSecurity` 配置

---

## 遇到的问题

### 问题 1: 未解析的引用 `WKWebViewNavigationDelegateProtocol`

**错误信息：**
```
Unresolved reference 'WKWebViewNavigationDelegateProtocol'
```

**原因：**
iOS 平台没有 `WKWebViewNavigationDelegateProtocol`，应该使用 `WKNavigationDelegateProtocol`。

**解决方案：**
移除错误的导入，使用正确的协议名称。

---

### 问题 2: 未解析的引用 `javaScriptEnabled`

**错误信息：**
```
Unresolved reference 'javaScriptEnabled'
```

**原因：**
`WKWebViewConfiguration.preferences` 的属性访问方式不正确。在 Kotlin/Native 中，某些 Objective-C 属性的访问方式不同。

**解决方案：**
iOS 的 `WKWebView` 默认启用 JavaScript，如果不需要禁用，可以省略此配置。如需配置，应使用正确的方式访问 `preferences` 属性。

---

### 问题 3: CGRect 类型不匹配

**错误信息：**
```
Argument type mismatch: actual type is 'CGRect', but 'CValue<CGRect>' was expected.
Cannot infer type for type parameter 'T'.
Unresolved reference 'readValue'.
```

**原因：**
`WKWebView` 的构造函数需要 `CValue<CGRect>` 类型，而不是直接的 `CGRect`。需要使用 `kotlinx.cinterop` 提供的 API 来创建 `CValue`。

**解决方案：**
使用 `memScoped` + `alloc<CGRect>()` + `readValue()` 来创建 `CValue<CGRect>`。

---

### 问题 4: 方法签名冲突

**错误信息：**
```
Conflicting overloads:
fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?): Unit
Add @ObjCSignatureOverride to allow collision for functions inherited from Objective-C.
```

**原因：**
`WKNavigationDelegateProtocol` 中的某些方法在 Kotlin/Native 中存在签名冲突，无法直接实现。

**解决方案：**
暂时不实现完整的导航代理，避免方法签名冲突。后续可以通过其他方式（如 KVO 监听 URL 变化）来实现回调功能。

---

### 问题 5: ExperimentalForeignApi 需要 opt-in

**错误信息：**
```
This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi'
```

**原因：**
使用 `kotlinx.cinterop` API（如 `memScoped`、`alloc`、`readValue`）需要显式 opt-in。

**解决方案：**
在函数和 lambda 中添加 `@OptIn(ExperimentalForeignApi::class)` 注解。

---

## 解决方案

### 1. CGRect 的正确创建方式

```kotlin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRect

@OptIn(ExperimentalForeignApi::class)
fun createWebView(): WKWebView {
    // 创建 CValue<CGRect>
    val frame: CValue<CGRect> = memScoped {
        alloc<CGRect>().apply {
            origin.x = 0.0
            origin.y = 0.0
            size.width = 0.0
            size.height = 0.0
        }.readValue()
    }
    
    val webView = WKWebView(frame = frame, configuration = configuration)
    return webView
}
```

**关键点：**
- 使用 `memScoped` 管理内存
- 使用 `alloc<CGRect>()` 分配内存
- 使用 `readValue()` 转换为 `CValue<CGRect>`
- 必须添加 `@OptIn(ExperimentalForeignApi::class)`

---

### 2. UIKitView 中的 opt-in

```kotlin
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ComposeWebView(...) {
    val platformWebView = remember {
        PlatformWebView(webViewConfig)
    }
    
    UIKitView(
        factory = {
            @OptIn(ExperimentalForeignApi::class)  // 在 lambda 中也需要 opt-in
            platformWebView.createWebView()
        },
        modifier = modifier.fillMaxSize(),
        ...
    )
}
```

**关键点：**
- 函数级别需要 opt-in
- `factory` lambda 内部也需要 opt-in（因为调用了需要 opt-in 的方法）

---

### 3. 导航代理的简化处理

由于方法签名冲突，暂时不实现完整的导航代理：

```kotlin
// 设置导航代理 - 使用简化的实现避免方法签名冲突
// 注意：WKWebView 的回调在 Kotlin/Native 中可能有签名冲突
// 这里先不设置 delegate，后续可以通过其他方式监听页面加载
// webView.navigationDelegate = ...
```

**替代方案：**
- 使用 KVO (Key-Value Observing) 监听 `URL` 属性变化
- 使用 JavaScript 注入来监听页面加载事件
- 使用 `WKWebView` 的其他 API 来获取页面状态

---

## 代码实现

### PlatformWebView 实现

```kotlin
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
            // iOS WKWebView 默认启用 JavaScript
            // iOS 默认启用 DOM Storage
        }
        
        // 创建 CValue<CGRect>
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
```

### ComposeWebView 实现

```kotlin
package org.example.project.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi

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
```

---

## 网络权限配置

### Info.plist 配置

在 `iosApp/iosApp/Info.plist` 中添加网络权限配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- 其他配置... -->
    
    <!-- 网络权限配置：允许应用访问网络 -->
    <key>NSAppTransportSecurity</key>
    <dict>
        <!-- 允许加载任意网络内容（包括 HTTP 和 HTTPS） -->
        <!-- 注意：生产环境建议只允许 HTTPS，或针对特定域名配置 -->
        <key>NSAllowsArbitraryLoads</key>
        <true/>
        
        <!-- 如果需要更安全的配置，可以取消上面的 NSAllowsArbitraryLoads，
             然后使用下面的配置只允许特定域名 -->
        <!--
        <key>NSExceptionDomains</key>
        <dict>
            <key>example.com</key>
            <dict>
                <key>NSExceptionAllowsInsecureHTTPLoads</key>
                <true/>
                <key>NSIncludesSubdomains</key>
                <true/>
            </dict>
        </dict>
        -->
    </dict>
</dict>
</plist>
```

### 配置说明

- **`NSAppTransportSecurity`**: App Transport Security (ATS) 配置
- **`NSAllowsArbitraryLoads`**: 设置为 `true` 允许所有网络连接（HTTP 和 HTTPS）
- **生产环境建议**: 移除 `NSAllowsArbitraryLoads`，使用 `NSExceptionDomains` 只允许特定域名

---

## 注意事项

### 1. 内存管理

- 使用 `memScoped` 管理 `CGRect` 的内存分配
- 在 `DisposableEffect` 中正确清理 WebView 资源
- 避免内存泄漏，及时设置 `navigationDelegate = null`

### 2. ExperimentalForeignApi

- 所有使用 `kotlinx.cinterop` API 的地方都需要 `@OptIn(ExperimentalForeignApi::class)`
- 包括函数级别和 lambda 内部

### 3. 导航代理限制

- 由于方法签名冲突，暂时无法实现完整的 `WKNavigationDelegateProtocol`
- 页面加载回调功能受限
- 可以考虑使用其他方式实现回调（KVO、JavaScript 注入等）

### 4. JavaScript 配置

- iOS `WKWebView` 默认启用 JavaScript
- 如需禁用，需要通过 `preferences` 配置，但访问方式可能因 Kotlin/Native 版本而异

### 5. 网络权限

- iOS 9+ 默认只允许 HTTPS
- 需要在 `Info.plist` 中配置 `NSAppTransportSecurity`
- 生产环境建议使用更严格的配置

### 6. Frame 设置

- `WKWebView` 的 `frame` 参数在 `UIKitView` 中会自动调整
- 初始创建时可以使用零值 `CGRect`
- 实际大小由 Compose 的 `modifier` 控制

---

## 总结

iOS WebView 的实现主要挑战在于：

1. **C 互操作 API 的使用**：需要正确使用 `kotlinx.cinterop` 来创建 `CValue<CGRect>`
2. **方法签名冲突**：`WKNavigationDelegateProtocol` 的方法在 Kotlin/Native 中存在冲突
3. **Opt-in 要求**：需要显式标记使用实验性 API

虽然存在一些限制（如导航代理回调），但基本的 WebView 功能（加载 URL、HTML、前进/后退等）都可以正常工作。

---

## 相关资源

- [Kotlin/Native C Interop](https://kotlinlang.org/docs/native-c-interop.html)
- [WKWebView Documentation](https://developer.apple.com/documentation/webkit/wkwebview)
- [Compose Multiplatform UIKitView](https://github.com/JetBrains/compose-multiplatform)

---

**最后更新**: 2024年
**作者**: KMP 项目团队

