# KMP (Kotlin Multiplatform) 版本兼容性指南

在 KMP 开发中，版本兼容性是保证项目顺利进行的第一道关卡。错误的组合会导致从编译错误到运行时崩溃等各种奇怪的问题。

本文档旨在提供一份基于官方文档的、权威的版本匹配参考，以便在升级或配置项目时进行核对。

### KMP 版本匹配黄金法则

请记住这个核心逻辑链，它能帮助您理解所有版本关系：
**`Kotlin` 版本决定了 `Compose Multiplatform` 版本 → `AGP` 版本决定了 `Gradle` 和 `JDK` 版本。**

---

### 表 1：Kotlin & Compose Multiplatform 兼容性 (核心)

这是 KMP 中最严格的对应关系，因为 Compose 的 UI 能力依赖于一个**特定版本**的 Kotlin 编译器插件。用错版本，项目将无法编译。

| Compose Multiplatform 版本 | 兼容的 Kotlin 版本 |
| :------------------------- | :----------------- |
| **1.6.10**                 | **2.0.0**          |
| **1.6.2**                  | **1.9.23**         |
| **1.6.1**                  | **1.9.22**         |
| **1.6.0**                  | **1.9.22**         |
| 1.5.12                     | 1.9.22             |
| 1.5.11                     | 1.9.21             |
| 1.5.10                     | 1.9.20             |
| 1.5.1                      | 1.9.10             |
| 1.5.0                      | 1.9.0              |

**数据来源 (官方文档，请以此为准):**
[JetBrains Official: Compose Multiplatform to Kotlin Compatibility](https://www.jetbrains.com/help/kotlin-multiplatform-mobile/compose-compiler.html#kotlin-compatibility)

**建议：**
*   **优先选择上表中加粗的稳定组合**。
*   当您想升级 Kotlin 时，第一步就是来这里查找它对应的 Compose 版本。

---

### 表 2：Android Gradle Plugin (AGP), Gradle & JDK 兼容性 (Android 端)

这部分由 Google 的 Android 团队决定。AGP 的每个版本都需要特定最低版本的 Gradle 才能运行，而较新的 Gradle 版本又需要较高的 JDK 版本。

| AGP 版本 | 要求的 Gradle 最低版本 | 建议的 JDK 版本 |
| :------- | :--------------------- | :-------------- |
| **8.4.x**  | **8.6**                | **17**          |
| **8.3.x**  | **8.4**                | **17**          |
| **8.2.x**  | **8.2**                | **17**          |
| 8.1.x    | 8.0                    | 17              |
| 8.0.x    | 8.0                    | 17              |
| 7.4.x    | 7.5                    | 11              |

**数据来源 (官方文档，请以此为准):**
[Android Developer: Android Gradle plugin release notes](https://developer.android.com/build/releases/gradle-plugin)

**建议：**
*   对于新项目或升级，**强烈建议使用 JDK 17**，因为这是目前 Android 开发的标准。
*   AGP 版本和 Gradle 版本需要严格对应。

---

### 推荐的稳定版本组合 (可以直接使用)

综合以上信息，这里给出一个当前（2025 年 Q4）被广泛验证的、稳定的“最佳实践”组合，您可以直接参考它来配置您的项目：

| 库/工具                 | **推荐版本** | 备注                                 |
| :---------------------- | :----------- | :----------------------------------- |
| **Kotlin**              | **1.9.23**   | 一个非常稳定且广泛支持的 LTS 版本。      |
| **Compose Multiplatform** | **1.6.2**    | 与 Kotlin 1.9.23 精确匹配。        |
| **AGP**                 | **8.3.2**    | 成熟稳定，与各类库兼容性好。             |
| **Gradle**              | **8.4**      | 满足 AGP 8.3.x 的要求。              |
| **Java (JDK)**          | **17**       | 现代 Android 开发的标配。            |
| **`targetSdk`**         | `34`         | 对应 Android 14。                  |
| **`minSdk`**            | `24`         | 可覆盖绝大多数设备 (Android 7.0)。     |
| **`compileSdk`**        | `34`         | 应与 `targetSdk` 保持一致。          |

---

### 其他重要库

*   **Decompose (导航库)**:
    *   `3.0.0-alpha*` 系列版本：设计用于 Compose 1.6.x 和 Kotlin 1.9.22+。
    *   **原则**：查看其 [Releases 页面](https://github.com/arkivanov/Decompose/releases)，作者会明确说明每个版本所基于的 Kotlin 和 Compose 版本。

*   **Ktor (网络库) / SQLDelight (数据库库) / kotlinx.serialization 等**:
    *   这些库由 JetBrains 或其他社区维护，它们的版本通常会紧跟 Kotlin 的大版本。
    *   **原则**：在它们的官方文档或 Release Notes 中，都会标明 "Compiled against Kotlin X.Y.Z"。选择与您项目 Kotlin 版本最接近的版本即可。
