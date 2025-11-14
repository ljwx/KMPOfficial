# 跨平台 WebView 实现总览

本文档提供了项目中跨平台 WebView 组件的总体介绍和使用指南。

## 目录

- [概述](#概述)
- [平台支持](#平台支持)
- [快速开始](#快速开始)
- [平台特定文档](#平台特定文档)
- [API 参考](#api-参考)

---

## 概述

本项目实现了一个跨平台的 WebView 组件，支持在 Android、iOS、JVM（Desktop）和 Web 平台上显示网页内容。

### 架构设计

采用 Kotlin Multiplatform 的 `expect/actual` 机制：

```
shared/commonMain/
  └── WebView.kt              # expect 接口定义

shared/{platform}Main/
  └── WebView.{platform}.kt  # actual 实现

composeApp/commonMain/
  └── ComposeWebView.kt       # expect Composable

composeApp/{platform}Main/
  └── ComposeWebView.{platform}.kt  # actual Composable
```

---

## 平台支持

| 平台 | 实现方式 | 状态 | 文档 |
|------|---------|------|------|
| Android | Android WebView | ✅ 完全支持 | - |
| iOS | WKWebView | ✅ 基本支持 | [iOS 实现指南](./iOS_WebView_Implementation.md) |
| JVM (Desktop) | JavaFX WebView | ⚠️ 占位符实现 | - |
| Web (JS/WASM) | iframe | ✅ 基本支持 | - |

### 功能支持矩阵

| 功能 | Android | iOS | JVM | Web |
|------|---------|-----|-----|-----|
| 加载 URL | ✅ | ✅ | ⚠️ | ✅ |
| 加载 HTML | ✅ | ✅ | ⚠️ | ✅ |
| JavaScript | ✅ | ✅ | ⚠️ | ✅ |
| DOM Storage | ✅ | ✅ | ⚠️ | ✅ |
| 前进/后退 | ✅ | ✅ | ⚠️ | ⚠️ |
| 重新加载 | ✅ | ✅ | ⚠️ | ✅ |
| 页面加载回调 | ✅ | ⚠️ | ⚠️ | ⚠️ |

**图例：**
- ✅ 完全支持
- ⚠️ 部分支持或有限支持
- ❌ 不支持

---

## 快速开始

### 基本使用

```kotlin
import org.example.project.webview.ComposeWebView

@Composable
fun MyScreen() {
    ComposeWebView(
        url = "https://www.example.com",
        modifier = Modifier.fillMaxSize(),
        enableJavaScript = true,
        onPageFinished = { url ->
            println("页面加载完成: $url")
        }
    )
}
```

### 加载 HTML 内容

```kotlin
ComposeWebView(
    url = "",  // 空 URL
    modifier = Modifier.fillMaxSize(),
    onPageStarted = { url ->
        // 通过 loadHtml 方法加载 HTML
    }
)

// 在 PlatformWebView 中
platformWebView.loadHtml(
    html = "<html><body><h1>Hello World</h1></body></html>",
    baseUrl = "https://www.example.com"
)
```

### 自定义 User Agent

```kotlin
ComposeWebView(
    url = "https://www.example.com",
    userAgent = "MyApp/1.0 (Custom User Agent)",
    modifier = Modifier.fillMaxSize()
)
```

---

## 平台特定文档

### iOS 平台

详细的 iOS 实现文档，包括遇到的问题和解决方案：

📖 [iOS WebView 实现指南](./iOS_WebView_Implementation.md)

**主要内容：**
- 实现流程
- 遇到的问题（CGRect 创建、方法签名冲突等）
- 解决方案
- 网络权限配置
- 注意事项

### Android 平台

Android 平台使用标准的 `Android WebView`，实现相对简单：

- 需要在 `AndroidManifest.xml` 中添加 `INTERNET` 权限
- 使用 `AndroidView` 嵌入原生 WebView
- 支持完整的页面加载回调

### Web 平台

Web 平台使用 `iframe` 实现：

- 受浏览器同源策略限制
- 使用 `UIKitView` 嵌入 HTML 元素
- 基本功能可用，但功能有限

### JVM (Desktop) 平台

当前为占位符实现：

- 显示提示信息
- 实际 WebView 集成需要特殊处理
- 可以考虑使用 Compose Desktop 的其他方式或第三方库

---

## API 参考

### ComposeWebView

```kotlin
@Composable
fun ComposeWebView(
    url: String,                                    // 要加载的 URL
    modifier: Modifier = Modifier,                 // Modifier
    enableJavaScript: Boolean = true,              // 是否启用 JavaScript
    enableDomStorage: Boolean = true,             // 是否启用 DOM Storage
    userAgent: String? = null,                     // 自定义 User Agent
    onPageStarted: ((String) -> Unit)? = null,    // 页面开始加载回调
    onPageFinished: ((String) -> Unit)? = null,   // 页面加载完成回调
    onError: ((String) -> Unit)? = null,          // 页面加载错误回调
)
```

### PlatformWebView

```kotlin
class PlatformWebView(config: WebViewConfig) {
    fun loadUrl(url: String)                      // 加载 URL
    fun loadHtml(html: String, baseUrl: String?)  // 加载 HTML
    fun goBack(): Boolean                          // 返回上一页
    fun goForward(): Boolean                       // 前进下一页
    fun reload()                                   // 重新加载
    fun dispose()                                  // 清理资源
}
```

### WebViewConfig

```kotlin
data class WebViewConfig(
    val url: String,                               // 初始 URL
    val enableJavaScript: Boolean = true,          // 是否启用 JavaScript
    val enableDomStorage: Boolean = true,          // 是否启用 DOM Storage
    val userAgent: String? = null,                 // 自定义 User Agent
    val onPageStarted: ((String) -> Unit)? = null, // 页面开始加载回调
    val onPageFinished: ((String) -> Unit)? = null, // 页面加载完成回调
    val onError: ((String) -> Unit)? = null,      // 页面加载错误回调
)
```

---

## 权限配置

### Android

在 `AndroidManifest.xml` 中添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### iOS

在 `Info.plist` 中添加：

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

详细配置请参考 [iOS WebView 实现指南](./iOS_WebView_Implementation.md#网络权限配置)。

---

## 已知限制

### iOS 平台

1. **导航代理回调限制**：由于方法签名冲突，页面加载回调功能受限
2. **JavaScript 配置**：默认启用，如需禁用可能需要特殊处理

### Web 平台

1. **同源策略**：受浏览器同源策略限制
2. **历史记录导航**：前进/后退功能有限

### JVM 平台

1. **占位符实现**：当前仅显示提示信息，需要进一步集成

---

## 最佳实践

1. **资源清理**：确保在组件销毁时调用 `dispose()` 方法
2. **URL 更新**：使用 `update` 参数处理 URL 变化
3. **错误处理**：实现 `onError` 回调来处理加载错误
4. **网络权限**：确保在相应平台的配置文件中添加网络权限
5. **生产环境**：iOS 生产环境建议使用更严格的网络安全配置

---

## 相关资源

- [Kotlin Multiplatform 官方文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [WKWebView 文档](https://developer.apple.com/documentation/webkit/wkwebview)
- [Android WebView 文档](https://developer.android.com/reference/android/webkit/WebView)

---

**最后更新**: 2024年
**维护者**: KMP 项目团队

