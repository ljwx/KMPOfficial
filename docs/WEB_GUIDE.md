# Web App (Compose for Web) 运行指南

本指南将教您如何运行 KMP 项目中的 Web 应用部分。我们的项目使用了 Compose for Web，它能将您在 `composeApp/commonMain` 中编写的 Kotlin UI 代码编译成 **WebAssembly (Wasm)**，从而在浏览器中运行。

## 核心理念

与 Ktor 服务器不同，Compose for Web 应用不是一个独立的后端服务。它会启动一个**本地开发服务器**，这个服务器的作用是托管编译好的 WebAssembly 文件、HTML 和 CSS，让您的浏览器可以加载它们。

## 运行方式：使用 Gradle 任务

运行 Web 应用的最佳且唯一的方式就是通过 Gradle 任务。

1.  **打开终端**：
    您可以使用 Cursor 内置的终端 (`Terminal` 标签页)，或者系统自带的终端。

2.  **执行 Gradle 任务**：
    在项目根目录下，输入并执行以下命令。请注意，这个任务名与我们之前用的都不同：
    ```bash
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
    *   **命令解析**:
        *   `:composeApp:`: 指定我们要操作的目标模块是 `composeApp`。
        *   `wasmJsBrowserDevelopmentRun`: 这是 Compose for Web (Wasm 目标) 插件提供的标准任务，用于启动一个带热重载功能的开发服务器。

3.  **查看结果**：
    *   终端会开始下载依赖、编译 Kotlin 代码到 Wasm。**第一次运行可能会花费几分钟时间**，请耐心等待。
    *   当编译完成后，您会看到类似 **`[webpack-dev-server] Project is running at:`** 的日志。
    *   下面会给出一个 URL 地址，通常是 `http://localhost:8088` (端口号可能会变化)。
    *   终端会保持运行状态，监听您的代码变更。

## 如何验证

1.  **复制 URL**：
    从终端的日志中，复制那个 `http://localhost:XXXX` 地址。

2.  **打开浏览器**：
    将地址粘贴到浏览器中并访问。

3.  您应该能看到一个和您在 Android/iOS 上看到的非常相似的 Compose UI 界面！

## 开发与热重载

这个开发服务器最强大的功能之一是**热重载 (Hot Reload)**。

*   在服务正在运行的情况下，回到 **Android Studio**。
*   尝试修改 `composeApp/src/commonMain/kotlin/org/example/project/App.kt` 文件中的一些文本或颜色。
*   保存文件 (`Cmd+S` / `Ctrl+S`)。
*   回到浏览器，刷新页面，您会发现您的修改已经生效了，**无需重启整个服务**！

现在，您已经真正打通了 KMP 的所有平台。恭喜！
