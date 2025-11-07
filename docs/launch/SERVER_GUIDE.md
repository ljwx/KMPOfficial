# Ktor 后端服务启动指南

本指南将教您如何通过两种主要方式，将项目中的 Ktor 后端服务运行起来。

## 方式一：通过 Android Studio 运行 (推荐)

这是最简单、最直观的方式，和您运行一个普通的 Kotlin 程序完全一样。

1.  **找到入口文件**：
    在左侧的项目视图中，导航到 `server` -> `src` -> `main` -> `kotlin` -> `org/example/project`，然后双击打开 **`Application.kt`** 文件。

2.  **找到 `main` 函数**：
    在 `Application.kt` 文件中，您会看到一个 `main` 函数。在函数声明的左侧，您会看到一个绿色的“播放”按钮 (▶️)。

    ```kotlin
    // ...
    fun main() { // <--- 播放按钮就在这一行左边
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }
    // ...
    ```

3.  **点击运行**：
    点击那个绿色的“播放”按钮，然后在弹出的菜单中选择 `Run 'Application.kt'`。

4.  **查看结果**：
    *   Android Studio 底部的 `Run` 窗口会被激活，开始构建并启动服务器。
    *   当您看到类似 `Application started in ...` 的日志时，就说明服务器已经成功运行了！

## 方式二：通过终端运行 (使用 Gradle)

如果您更喜欢使用命令行，或者需要在 CI/CD 环境中运行，可以使用 Gradle 任务。

1.  **打开终端**：
    您可以使用 Cursor 内置的终端 (`Terminal` 标签页)，或者系统自带的终端。

2.  **执行 Gradle 任务**：
    在项目根目录下，输入并执行以下命令：
    ```bash
    ./gradlew :server:run
    ```

3.  **查看结果**：
    终端会开始执行 Gradle 任务。当构建完成后，您会看到 Ktor 服务器启动的日志，并且终端会停留在运行状态，表示服务正在监听请求。

## 如何验证服务是否正常运行

无论您用哪种方式启动，成功后都可以通过以下步骤验证：

1.  **打开浏览器**。
2.  访问地址：**`http://0.0.0.0:8080`**
3.  您应该能看到浏览器页面上显示类似于 **`Ktor: Hello, Android!`** 或 **`Ktor: Hello, iOS!`** 的文本。

这就证明您的 KMP 项目的后端部分已经成功跑起来了！
