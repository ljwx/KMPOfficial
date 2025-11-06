# iOS 开发快速上手指南 (for Android Devs) - 进阶版

欢迎来到 iOS 开发！本指南将帮助您使用 Xcode 将我们的 KMP 项目在 iPhone 模拟器上跑起来，并深入理解关键的开发工作流。

## 第 1 部分：基础运行

### 核心理念转换

*   **Android Studio**: 负责 KMP 项目的**绝大部分工作**，包括共享逻辑、共享 UI 的编码和 Gradle 构建。
*   **Xcode**: 在 KMP 工作流中，主要扮演**“最终组装和启动器”**的角色。它负责编译少量的原生 Swift/Objective-C 代码，将 Gradle 编译好的 Kotlin `.framework` 链接进来，最后打包、签名并安装到模拟器或真机上。

**一句话总结：绝大部分时间在 Android Studio，只在运行/调试 iOS 平台时才切换到 Xcode。**

### 运行到模拟器：三步走

1.  **打开正确目录**: 必须用 Xcode 打开项目中的 **`iosApp`** 目录。
2.  **选择目标**: 在 Xcode 顶部工具栏选择一个 **iPhone 模拟器**或连接的**真机**。
3.  **运行**: 点击左上角的**“播放”按钮 (▶️)**。

---

## 第 2 部分：核心工作流详解

### 我应该在哪里写代码？

**答案：99% 的时间都在 Android Studio 里。**

*   **共享业务逻辑**: 在 `shared` 模块中编写 (`commonMain`, `androidMain`, `iosMain` 等)。
*   **共享 UI**: 在 `composeApp` 模块的 `commonMain` 中编写 Composable 函数。
*   **平台特定 API 实现**: 在 `shared` 模块的 `iosMain` 中为 `expect` 声明编写 `actual` 实现。

**什么时候才需要在 Xcode 里写代码？**
只有当您需要编写纯粹的、无法通过 KMP `expect/actual` 机制实现的 **Swift / Objective-C** 代码时。例如，实现一个复杂的后台推送通知扩展。对于绝大多数应用功能，您都无需在 Xcode 中编写代码。

### 如何调试 UI 和代码？

**1. 调试 Kotlin 代码 (断点)**
这部分体验非常无缝！您可以在 **Android Studio** 的 `shared` 或 `composeApp` 模块的 Kotlin 代码中设置断点。然后，在 Xcode 中以调试模式 (正常的“播放”按钮) 运行应用。当代码执行到断点处时，**Xcode 会自动暂停**，您可以像调试原生代码一样查看变量和调用栈。

**2. 检查 UI 布局**
*   **相当于安卓的？**: `Layout Inspector`
*   **Xcode 做法**: 应用在模拟器或真机上运行时，点击 Xcode 调试栏中的 **"Debug View Hierarchy"** 按钮 (一个看起来像三个叠加矩形的图标)。
*   **区别**: Xcode 的视图检查器非常强大，可以 3D 旋转和查看所有视图层级。但请注意，对于 Compose UI，它只能看到承载所有 Composable 的那个最顶层的 `ComposeView`。它**无法**像 Android Studio 的 Layout Inspector 那样展示每一个 Composable 函数的细节和参数。对于 Compose UI 的调试，还需依赖日志和代码审查。

### Xcode 生存指南：关键文件和概念

1.  **`iosApp.xcodeproj` vs `iosApp.xcworkspace`**
    *   **`.xcodeproj`**: 您的主应用项目文件，类似 Android 的 `app` 模块。
    *   **`.xcworkspace`**: 您的“工作区”。它包含了主项目 (`.xcodeproj`) **以及**所有的外部依赖 (比如通过 CocoaPods 集成的库)。
    *   **法则**: **永远打开 `.xcworkspace` 文件**，而不是 `.xcodeproj`。否则，项目会因为找不到外部依赖而编译失败。

2.  **`Info.plist` (属性列表)**
    *   **相当于安卓的？**: `AndroidManifest.xml`。
    *   **作用**: 配置应用名称、版本号、图标，以及最重要的——**申请系统权限**。当您需要使用网络、相机、定位等功能时，必须在这里添加相应的描述条目，否则 App 会在调用时闪退。

3.  **`Assets.xcassets` (资产目录)**
    *   **相当于安卓的？**: `res/drawable`, `res/mipmap`, `res/values/colors.xml` 的集合体。
    *   **作用**: 管理应用的静态资源，如图标 (`AppIcon`)、图片、颜色等。这是一个可视化的编辑器，您可以直接把图片文件拖进去。

---

## 第 3 部分：下一步

### 如何在真实 iPhone 上运行？

这比 Android 要复杂一些，因为它涉及到苹果的签名和认证体系。

1.  **Apple Developer Account**: 您需要一个苹果开发者账号 (个人账号每年 99 美元)。
2.  **在 Xcode 中登录**: `Xcode -> Settings... -> Accounts`，添加您的开发者账号。
3.  **设置签名**: 在项目设置的 `Signing & Capabilities` 标签页中，选择您的开发者团队。Xcode 会尝试自动为您管理证书和描述文件。

这是一个很大的话题，但以上是您开始探索的第一步。

### 常见问题排错

*   **问题**: 编译时报错，提示 `Framework not found "shared"` 或 `Module 'shared' not found`。
*   **原因**: Xcode 找不到由 Gradle 编译生成的 Kotlin framework。
*   **解决方案**: 这是最常见的问题。返回 **Android Studio**，执行一次 **Gradle Sync**，然后再执行一次 **Build -> Make Project**。然后再回 Xcode，使用快捷键 `Cmd+Shift+K` 清理一下构建缓存 (`Clean Build Folder`)，最后重新运行。

希望这份更详细的指南能成为您在 iOS 开发探索之路上更得力的助手！
