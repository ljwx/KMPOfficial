# KMP 项目结构说明

欢迎使用 Kotlin Multiplatform！这个文件将帮助您理解项目中的每个核心目录和文件的作用。

## 顶层目录和文件

这是您在项目根目录下看到的主要结构：

```
.
├── composeApp/         # 【核心】所有UI相关的代码都在这里
├── iosApp/             # iOS 应用的入口和 Xcode 项目配置
├── server/             # [可选] Ktor 后端服务的代码
├── shared/             # 【核心】所有共享的业务逻辑代码都在这里
│
├── gradle/             # Gradle 版本管理和依赖版本目录
│   └── libs.versions.toml # 所有依赖项的版本都在这里统一管理
├── build.gradle.kts    # 项目根构建脚本 (通常无需修改)
└── settings.gradle.kts # 声明项目包含哪些模块
```

---

## 模块详解

### `shared` 模块 (共享逻辑层)

这是 KMP 的心脏。所有**非 UI**的、**可跨平台共享**的代码都应该放在这里。

*   **`shared/src/commonMain/kotlin`**: **最核心的目录**。所有平台的通用代码都在这里，例如：
    *   数据模型 (Data Classes)
    *   业务逻辑 (ViewModels, Presenters, UseCases)
    *   网络请求 (Ktor Client)
    *   数据库操作 (SQLDelight)
    *   `expect` 声明 (定义一个需要平台实现的通用接口)

*   **`shared/src/androidMain/kotlin`**: 只在 **Android** 平台使用的代码。
    *   通常用于提供 `actual` 实现 (例如，获取 Android 的数据库驱动)。

*   **`shared/src/iosMain/kotlin`**: 只在 **iOS** 平台使用的代码。
    *   提供 iOS 的 `actual` 实现 (例如，调用 iOS 原生 API)。

*   **`shared/src/jvmMain/kotlin`**: 被所有 **JVM 平台共享**的代码，包括 **Android** 和 **Desktop**。

*   **`shared/src/jsMain/kotlin`**: 只在 **JavaScript** 平台 (即 **Web**) 使用的代码。

*   **`build.gradle.kts`**: `shared` 模块的构建脚本。在这里配置 KMP 的目标平台和各个平台的依赖。

### `composeApp` 模块 (共享 UI 层)

这是 **Compose Multiplatform** 的核心。所有**UI 相关**的代码都放在这里。

*   **`composeApp/src/commonMain/kotlin`**: **UI 的核心**。所有平台的通用 Composable 函数 (UI 界面) 都在这里。你的 App 的绝大部分界面代码都会写在这里。

*   **`composeApp/src/androidMain/kotlin`**: Android 应用的**入口**。这里会有一个 `MainActivity.kt`，它的作用就是加载 `commonMain` 中定义的共享 UI。

*   **`composeApp/src/jvmMain/kotlin`**: Desktop 应用的**入口** (`main.kt`)。它的作用是创建一个窗口，然后在里面加载共享 UI。

*   **`composeApp/src/iosMain/kotlin`**: iOS 应用的**入口**。它负责启动 iOS 应用并加载共享 UI。

*   **`composeApp/src/webMain/kotlin`**: Web 应用的**入口**。

### `iosApp` 模块 (iOS 配置层)

这个目录本质上是一个 **Xcode 项目**。

*   **作用**: 它不包含太多 Kotlin 代码。它的主要作用是配置 iOS 应用的信息（如应用图标、权限、启动画面等），并作为“壳”来承载和启动 `composeApp` 模块中为 iOS 编译好的 UI。
*   **注意**: 如果要编译和运行 iOS 应用，您需要用 Xcode 打开这个 `iosApp` 目录。

### `server` 模块 (后端服务层) - 详细说明

这是一个独立的 **Ktor 后端服务**项目。Ktor 是 JetBrains 官方开发的现代化 Kotlin 网络框架。

*   **入口文件**: `server/src/main/kotlin/org/example/project/Application.kt` 是服务的启动入口。
*   **核心优势**: 它可以直接引用 `shared` 模块中定义的共享代码。这意味着您的数据模型 (Data Classes) 和核心业务逻辑可以在前端 (Android/iOS/Web) 和后端之间完美复用，实现端到端的类型安全，极大减少了重复代码和潜在的 bug。

---

## 源码 (`src`) vs 构建产物 (`build`, `bin`)

您会注意到项目中有很多 `build` 目录 (在 `server` 模块中则是一个 `bin` 目录)。这是一个非常重要的概念：

*   **`src` (Source - 源码)**: 这是您作为开发者**唯一需要关心和编写代码**的地方。它包含了您项目的所有设计蓝图。

*   **`build` / `bin` (Build Output - 构建产物)**: 这些目录是由 Gradle (构建工具) **自动生成**的。它包含了编译后的代码、处理后的资源和其他中间文件。

**黄金法则**: **永远不要手动修改 `build` 或 `bin` 目录里的任何东西。** 请将它们视为随时可以被删除和重新生成的临时文件夹。您的所有工作都应该在 `src` 目录中进行。这和 Android 项目中 `app/src` 与 `app/build` 的关系是完全一样的。
