# Desktop App (JVM) 运行指南

本指南将教您如何将 KMP 项目中的 Desktop 应用运行起来。

## 核心理念

Desktop 应用本质上是一个标准的桌面 Java/Kotlin 应用程序。Gradle 会将您的共享代码 (`shared` 和 `composeApp` 模块) 打包成一个可执行的 `.jar` 文件，然后通过 Java 虚拟机 (JVM) 在您的电脑上运行，弹出一个桌面窗口。

## 方式一：通过 Android Studio 运行 (推荐)

这是最简单、最直观的方式，和您运行 Ktor 服务器时完全一样。

1.  **找到入口文件**：
    在左侧的项目视图中，导航到 `composeApp` -> `src` -> `jvmMain` -> `kotlin` -> `org/example/project`，然后双击打开 **`main.kt`** 文件。

2.  **找到 `main` 函数**：
    在 `main.kt` 文件中，您会看到一个 `main` 函数。在函数声明的左侧，您会看到一个绿色的“播放”按钮 (▶️)。

    ```kotlin
    fun main() = application { // <--- 播放按钮就在这一行左边
        Window(
            onCloseRequest = ::exitApplication,
            title = "YourProjectName",
        ) {
            App()
        }
    }
    ```

3.  **点击运行**：
    点击那个绿色的“播放”按钮，然后在弹出的菜单中选择 `Run 'MainKt'`。

4.  **查看结果**：
    *   Android Studio 底部的 `Run` 窗口会被激活，开始构建并启动应用。
    *   片刻之后，您的电脑桌面上应该会**弹出一个新的窗口**，窗口中显示着和您在 Android/iOS/Web 上看到的完全一样的 Compose UI！

## 方式二：通过终端运行 (使用 Gradle)

如果您更喜欢使用命令行，也可以通过 Gradle 任务来运行。

1.  **打开终端**：
    您可以使用 Cursor 内置的终端 (`Terminal` 标签页)，或者系统自带的终端。

2.  **执行 Gradle 任务**：
    在项目根目录下，输入并执行以下命令：
    ```bash
    ./gradlew :composeApp:run
    ```
    
    **注意**：正确的任务名称是 `run`，不是 `jvmRun`。Compose Desktop 插件会自动识别 JVM 目标并运行桌面应用。

3.  **查看结果**：
    终端会开始执行 Gradle 任务。当构建完成后，您会看到一个独立的桌面窗口弹出。

现在，我们可以自豪地宣布，您的 KMP 项目已经真正地在 **Android, iOS, Web, Server, 和 Desktop** 所有五个平台上成功运行了！这是一个了不起的成就！
