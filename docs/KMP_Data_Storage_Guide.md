### **KMP 持久化存储方案 `multiplatform-settings` 使用指南**

本文档旨在说明如何在 Kotlin Multiplatform (KMP) 项目中正确集成和使用 `multiplatform-settings` 库，以实现跨平台的键值对数据持久化存储。

#### **1. 核心目标：为什么要用这个库？**

在移动或 Web 应用中，我们经常需要存储一些简单的数据，如用户偏好（是否开启夜间模式）、用户凭证（Token）、或一些轻量级的缓存。

- **在 Android 中**，我们通常使用 `DataStore` 或 `SharedPreferences`。
- **在 iOS 中**，我们使用 `NSUserDefaults`。
- **在桌面和 Web 中**，也有各自的存储机制。

`multiplatform-settings` 库的目标就是提供一个统一的 API，让您可以在 `commonMain`（共享代码）中调用一次 `settings.putString("key", "value")`，而库的底层会自动在不同平台上调用对应的原生存储技术。

**核心优势：业务逻辑只需写一次，同时享受原生平台的最佳性能。**

#### **2. 原理剖析：为什么需要平台相关的代码？**

这是 KMP 的核心设计理念：**共享通用逻辑，拥抱平台特性**。

我们编写的平台相关代码（`expect`/`actual`），本质上是在搭建一座“桥梁”，连接共享代码和原生平台。

1.  **`expect class SettingsFactory` (在 `commonMain`)**
    *   **它是什么**：一个“设计蓝图”或“接口声明”。
    *   **它的作用**：它在共享模块中定义了一个契约，即“任何想要使用我的平台，都必须提供一个能创建出 `Settings` 实例的工厂”。它不关心如何创建，只关心这个能力本身。

2.  **`actual class SettingsFactory` (在 `androidMain`, `iosMain` 等)**
    *   **它是什么**：对“设计蓝图”的具体实现。
    *   **它的作用**：
        *   在 **Android** 上，它使用 `Context` 来创建 `DataStoreSettings`，这是 Android 推荐的现代存储方案。
        *   在 **iOS** 上，它使用 `NSUserDefaults` 来创建 `NSUserDefaultsSettings`。
        *   在 **JVM** 或 **JS** 上，它也使用了各自平台的原生存储 API。

通过这种方式，我们的业务代码（如 `SettingsManager`）可以完全留在 `commonMain` 中，对平台的具体实现一无所知，从而实现了最大程度的代码复用和解耦。

#### **3. 关键概念：`api` vs `implementation`**

这是 Gradle 依赖配置中的一个关键点，也是导致许多 KMP 编译问题的根源。

- `implementation`：依赖是模块的“内部实现细节”。该依赖的 API 不会暴露给其他依赖本模块的模块。
- `api`：依赖是模块“公共 API”的一部分。该依赖的 API 会被传递下去，暴露给其他模块。

**在我们的场景中：**

`commonMain` 中的 `expect class SettingsFactory` 返回了一个 `Settings` 对象。这个 `Settings` 类型来自于 `multiplatform-settings` 库。因为这个类型出现在了公共的 `expect` 类中，所以它成为了 `shared` 模块公共 API 的一部分。

因此，我们**必须**在 `shared/build.gradle.kts` 中使用 `api(libs.multiplatformSettings.core)`。

这样做是告诉 Gradle：“`multiplatform-settings` 这个库的 API 是我 `shared` 模块公开契约的一部分，请把它暴露给所有依赖我的平台模块（如 `androidMain`, `iosMain`），以便它们能够看到并实现我定义的接口。”

如果使用 `implementation`，`androidMain` 就无法识别 `commonMain` 定义的 `Settings` 类型，从而导致 `Return type mismatch` 编译错误。

#### **4. 标准用法：如何在项目中使用？**

**第一步：初始化 (一次性)**

在使用 `SettingsManager` 之前，必须在各个平台的应用入口处进行初始化。这个操作通常在应用启动时执行一次即可。

*   **Android (`MainActivity.kt` 或 `Application.kt` 的 `onCreate` 方法中):**
    ```kotlin
    import org.example.project.shared.settings.SettingsFactory
    import org.example.project.shared.settings.SettingsManager

    // ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用 applicationContext 初始化 SettingsManager
        SettingsManager.init(SettingsFactory(applicationContext))
        
        // ...
    }
    ```

*   **iOS (`ContentView.swift` 或 `iOSApp.swift` 的 `init` 方法中):**
    ```swift
    import SwiftUI
    import shared // 确保导入了 shared 模块

    struct ContentView: View {
        init() {
            // 在 iOS 中，工厂不需要参数
            SettingsManager.shared.doInit(factory: SettingsFactory())
        }
        
        // ...
    }
    ```

**第二步：在共享代码中读写数据**

初始化完成后，您就可以在项目的任何地方（ViewModel, Repository 等）通过 `SettingsManager` 单例来安全地读写数据了。

```kotlin
// 位于 composeApp/src/commonMain/kotlin/org.example.project/.. 任意位置

import org.example.project.shared.settings.SettingsManager

fun demoSettingsUsage() {
    // --- 保存数据 ---
    SettingsManager.saveUserToken("your_secret_auth_token_here")
    SettingsManager.saveDarkMode(true)
    
    println("数据已保存！")

    // --- 读取数据 ---
    val currentToken = SettingsManager.getUserToken()
    val isDarkModeEnabled = SettingsManager.isDarkMode()

    println("读取到的 Token: $currentToken") // 输出: 读取到的 Token: your_secret_auth_token_here
    println("夜间模式是否开启: $isDarkModeEnabled") // 输出: 夜间模式是否开启: true
    
    // 读取一个不存在的 key, 会返回指定的默认值
    val nonExistentValue = SettingsManager.isDarkMode() // isDarkMode() 方法内部默认返回 false
    println("不存在的布尔值: $nonExistentValue") // 输出: 不存在的布尔值: false
}
```
