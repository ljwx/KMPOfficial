# KMP iOS 构建问题排查手册

本文档总结了一次真实且复杂的 KMP 项目构建失败的排错全过程，旨在提炼出一套可复用的问题诊断思路和解决方案。

## 问题一：运行时链接错误 (`IrLinkageError`)

*   **问题 (Problem):** 应用在 iOS 模拟器上启动后立即崩溃。
*   **现象 (Symptoms):**
    ```
    Uncaught Kotlin exception: kotlin.native.internal.IrLinkageError: 
    No class found for symbol 'kotlinx.datetime/Clock.System|null[0]'
    ```
    或
    ```
    Uncaught Kotlin exception: kotlin.native.internal.IrLinkageError: 
    Function 'selectable' can not be called...
    ```
*   **原因分析 (Cause Analysis):**
    `IrLinkageError` 是 Kotlin/Native 特有的链接错误。它意味着在最终生成 iOS 的可执行文件时，编译器找不到某个函数或类的具体实现。这通常有两种可能：
    1.  **依赖范围不正确**：一个模块（如 `composeApp`）依赖了另一个模块（如 `kmplog`）。如果 `kmplog` 的公开 API 中使用了 `kotlinx-datetime`，但它在 `build.gradle.kts` 中却将 `kotlinx-datetime` 声明为 `implementation`，那么这个依赖的实现细节就不会暴露给 `composeApp`，导致链接失败。
    2.  **根本性的版本不兼容**：这是我们这次遇到的真正元凶。当 `Kotlin` 版本、`Compose Multiplatform` 版本、以及其他 `androidx` 库版本之间存在 ABI (应用二进制接口) 不兼容时，Gradle 在打包 Framework 的过程中可能就会“丢失”某些库的实现，尽管你的代码和依赖声明看起来都是正确的。这最终以 `IrLinkageError` 的形式在运行时爆发。

*   **解决方案 (Solution):**
    1.  **检查并修正依赖范围**：确保所有被下游模块直接或间接使用的 API 所涉及的库，都使用 `api()` 而不是 `implementation()` 来声明。
    2.  **核对版本兼容性**：这是最关键的一步。前往 **JetBrains/compose-jb** 的官方版本兼容性页面（[VERSIONING.md](https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#kotlin-compatibility)），找到与你正在使用的 **Kotlin 版本**完全匹配的 **Compose Multiplatform 版本**。

*   **拓展思路 (Divergent Thinking):**
    *   当遇到链接错误时，首先怀疑版本兼容性，并查阅官方文档。
    *   可以尝试将 KMP 框架的链接方式从动态 (`isStatic = false`) 改为静态 (`isStatic = true`)。这会改变链接机制，有时可以作为一种诊断手段，绕过一些动态链接的缓存或配置问题。
    *   如果某个库持续导致问题，可以尝试在最终的 `iosApp` 中通过 SPM 或 CocoaPods 直接依赖它，但这是一种“降维打击”，应作为最后的手段。

## 问题二：Gradle 环境与缓存损坏

*   **问题 (Problem):** Gradle 同步或执行任务失败，报出缓存文件无法读取或找不到 Java 环境的错误。
*   **现象 (Symptoms):**
    ```
    Could not read workspace metadata from .../metadata.bin
    ```
    或
    ```
    The operation couldn’t be completed. Unable to locate a Java Runtime.
    ```
*   **原因分析 (Cause Analysis):**
    1.  **缓存损坏**：Gradle 的缓存 (`~/.gradle/caches/`) 是一个复杂的系统。有时因为异常中断、磁盘问题或版本切换，缓存会进入一个不一致的损坏状态，导致 Gradle 无法正常读写。
    2.  **Java 环境混淆**：这是最隐蔽的问题。尽管你的系统环境变量（`JAVA_HOME`）指向一个完整的 JDK，但启动 Gradle 的工具（如 IntelliJ IDEA / Android Studio）可能会错误地使用它自己捆绑的、不完整的 JRE (JetBrains Runtime)。这个 JRE 缺少 `jlink` 等 Android 构建所需的关键工具。

*   **解决方案 (Solution):**
    1.  **强制指定 Java Home**：在项目根目录的 `gradle.properties` 文件中，添加 `org.gradle.java.home` 属性，并将其路径明确指向你电脑上安装的**完整 JDK** 的 Home 目录。
    2.  **彻底清理缓存**：当怀疑缓存损坏时，不要只依赖 `./gradlew clean`。最可靠的方法是手动删除整个 Gradle 缓存目录：`rm -rf ~/.gradle/caches/`。同时，清理 Xcode 的构建缓存 `rm -rf ~/Library/Developer/Xcode/DerivedData/` 也是一个好习惯。
    3.  **禁用配置缓存**：如果问题持续存在，可以在 `gradle.properties` 中设置 `org.gradle.configuration-cache=false` 来禁用配置缓存。这会牺牲一些构建速度，但能确保构建逻辑总是从头开始，避免被锁在损坏的缓存状态里。

*   **拓展思路 (Divergent Thinking):**
    *   始终确保 IDE 设置中 `Gradle JDK` 的路径与你期望使用的 JDK 一致。
    *   学习使用 `./gradlew --stop` 命令来停止所有正在运行的 Gradle 守护进程，这有助于解决文件被锁定的问题。
    *   当一个 Gradle 版本持续出现问题时，果断在 `gradle-wrapper.properties` 中更换到另一个稳定版本，是解决环境问题的有效手段。

## 问题三：构建脚本 API 不兼容

*   **问题 (Problem):** 在调整了 AGP、Kotlin 或其他插件的版本后，Gradle 同步失败，报 `Unresolved reference`。
*   **现象 (Symptoms):**
    ```
    Unresolved reference: ExperimentalWasmDsl
    Unresolved reference: withHostTestBuilder
    Unsupported plugin option: ...:generateFunctionKeyMetaAnnotations=true
    ```
*   **原因分析 (Cause Analysis):**
    这些都是由于 API 变更导致的。随着各大插件（尤其是 AGP 和 Compose）的快速迭代，一些旧的、实验性的 API 会被废弃、重命名或移除。当你的构建脚本 (`.gradle.kts`) 还在使用这些旧 API 时，新版本的插件自然会报错。

*   **解决方案 (Solution):**
    1.  **移除废弃 API**：根据错误提示，找到对应的代码行并将其删除或注释掉。例如，`ExperimentalWasmDsl` 在新版 Kotlin 中已不再需要。
    2.  **查阅迁移指南**：对于更复杂的 API 变更（比如 Android 测试相关的 API），需要查阅对应插件版本的官方迁移指南，找到新的替代写法。
    3.  **识别并禁用问题插件**：在我们的案例中，`generateFunctionKeyMetaAnnotations` 这个错误是由 `composeHotReload` 这个实验性插件在后台注入的。通过暂时禁用它，我们成功地绕过了这个问题。

*   **拓展思路 (Divergent Thinking):**
    *   在进行版本升级时，保持谨慎，一次只升级一个主要组件，这样能更快地定位到是哪个插件的变更导致了问题。
    *   学会阅读插件的官方文档和更新日志（Changelog），这是理解 API 变更最权威的来源。

---

### 升级操作总纲（为你自己操作时准备）

当你决定把项目恢复并重新升级时，我建议你遵循以下“安全升级路径”：

1.  **第一步：确定“黄金组合”**
    *   访问 [Compose Multiplatform 版本兼容性页面](https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#kotlin-compatibility)。
    *   选择你想要使用的 **Kotlin** 版本（比如最新的稳定版）。
    *   根据该 Kotlin 版本，找到官方推荐的 **Compose Multiplatform 版本**。
    *   访问 [Android Gradle 插件版本说明](https://developer.android.com/build/releases/gradle-plugin)，找到与你选择的 Compose 版本兼容的 **AGP** 版本，以及它所要求的 **Gradle** 版本。

2.  **第二步：执行修改**
    *   在 `gradle/libs.versions.toml` 中，一次性更新所有核心版本号：`kotlin`、`composeMultiplatform`、`agp` 以及相关的 `androidx` 库。
    *   在 `gradle/wrapper/gradle-wrapper.properties` 中，更新 `distributionUrl` 以匹配你选择的 Gradle 版本。
    *   在 `gradle.properties` 中，确保 `org.gradle.java.home` 指向一个与新 Gradle 版本兼容的、完整的 JDK。

3.  **第三步：彻底清理**
    *   关闭你的 IDE。
    *   在终端运行 `rm -rf ~/.gradle/caches/ .gradle/` 来清除所有本地和项目的 Gradle 缓存。
    *   运行 `rm -rf ~/Library/Developer/Xcode/DerivedData/` 清理 Xcode 缓存。

4.  **第四步：全新同步与构建**
    *   重新打开 IDE，它会提示你这是一个新的 Gradle 项目，点击 "Sync" 或 "Import"。
    *   耐心等待 Gradle 下载全新的发行版和所有依赖。
    *   解决可能出现的、由于 API 变更导致的构建脚本语法错误（参考 **问题三**）。
    *   同步成功后，直接去 Xcode 中 Clean 并 Build。

