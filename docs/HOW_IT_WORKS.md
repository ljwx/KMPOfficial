# KMP UI 共享工作原理解析

您已经看到，Android, iOS, 和 Web 都显示了同样的 UI。这不是魔法，而是一套设计精良的工程流程。本篇文档将为您揭示其内部的工作原理。

我们可以把整个系统想象成**“一部电影和三家不同的电影院”**。

## 第 1 部分：共享核心 (The Movie - 电影本身)

电影的核心内容，无论在哪家影院放映，都是不变的。在我们的项目中，这个“核心内容”由两个最重要的共享模块组成：

### 1. `shared/commonMain`: 电影的“剧本和设定”

这里存放着最基础、最核心的**业务逻辑和数据模型**。
*   **`Greeting.kt`**: 定义了 `Greeting` 这个类，它提供了要显示的数据（“Hello, Android/iOS/Web!”）。
*   **未来扩展**: 以后您的网络请求代码 (Ktor)、数据库代码 (SQLDelight)、ViewModel 等，都会放在这里。它们是驱动 UI 显示内容的“剧本”。

### 2. `composeApp/commonMain`: 电影的“视觉画面”

这里存放着**所有平台共用的 UI 代码**。
*   **`App.kt`**: 这里定义了 `App()` 这个 Composable 函数。它就是我们电影的**“正片”**。它负责把 `Greeting` 提供的数据（剧本）渲染成用户能看到的视觉元素（按钮、文本、图片等）。

**这两个模块如何关联？**
在 `composeApp/build.gradle.kts` 文件中，有一行至关重要的代码：
`implementation(projects.shared)`
这行代码告诉 Gradle：“`composeApp`（视觉画面）需要依赖 `shared`（剧本）才能工作。”

---

## 第 2 部分：平台启动器 (The Theaters - 电影院)

电影本身是通用的，但每家电影院（Android, iOS, Web）都有自己独特的“放映机”和“银幕”来播放它。这些“放映机”就是各个平台模块中的入口代码。

### 1. Android 电影院

*   **放映机**: `composeApp/src/androidMain/kotlin/MainActivity.kt`
*   **工作原理**:
    ```kotlin
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { // <--- 关键！
                App()
            }
        }
    }
    ```
    `setContent { ... }` 的作用是告诉 Android：“请把 `App()` 这个来自 `commonMain` 的共享 Composable 函数，作为这个 Activity 的全部内容，给我**“放映”**出来。”

### 2. iOS 电影院

*   **放映机**: `composeApp/src/iosMain/kotlin/MainViewController.kt`
*   **工作原理**:
    ```kotlin
    fun MainViewController() = ComposeUIViewController { App() } // <--- 关键！
    ```
    `ComposeUIViewController` 是一个由 Compose Multiplatform 提供的特殊“放映机”。它的作用是：接收一个共享的 Composable (`App()`) 作为参数，然后把它包装成一个 iOS 系统能够识别和显示的**原生 UIViewController**。

### 3. Web 电影院

*   **放映机**: `composeApp/src/webMain/kotlin/main.kt`
*   **工作原理**:
    ```kotlin
    @OptIn(ExperimentalComposeUiApi::class)
    fun main() {
        onWasmReady { // <--- 关键！
            CanvasBasedWindow("YourProjectName") {
                App()
            }
        }
    }
    ```
    它会在网页中创建一个 `<canvas>` 元素（一块“画布”或“银幕”），然后告诉 Compose：“请把 `App()` 这个共享 Composable，**“画”** 在这块画布上。”

---

## 第 3 部分：流程总结：从代码到像素

1.  **Gradle 读取配置**:
    *   `settings.gradle.kts` 告诉 Gradle 项目由哪些模块组成。
    *   `build.gradle.kts` 文件定义每个模块的编译目标和依赖关系。

2.  **编译共享代码**:
    *   `shared` 模块被编译成多个平台的库（`.jar` for Android, `.framework` for iOS, `.wasm` for Web）。
    *   `composeApp` 模块也被编译成多个平台的 UI 库，并带上了对 `shared` 库的依赖。

3.  **平台应用启动**:
    *   **Android**: `MainActivity` 启动，调用 `setContent`，直接渲染共享 UI。
    *   **iOS**: Swift 代码启动，加载一个 `UIViewController`，这个 Controller 内部包裹并渲染了共享 UI。
    *   **Web**: `index.html` 被加载，其 JS 代码启动 Wasm，然后在 `<canvas>` 中渲染共享 UI。

**最终，虽然每个平台的“放映机”都不同，但它们播放的都是同一部叫做 `App()` 的“电影”，这部电影的“剧本”则来自 `shared` 模块。**

这就是 KMP 能够实现“一次编码，多端运行”且 UI 高度一致的根本原因。
