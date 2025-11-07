## Jetpack Compose 与 Compose Multiplatform 对比

本文档旨在阐明 Jetpack Compose (Android) 与 Compose Multiplatform (KMP) 的关系、异同，并重点说明平台差异带来的问题与应对策略。

---

### 1. 核心关系：同源但目标不同

- **Jetpack Compose (JPC)**：由 Google 官方主导，专为 **Android** 平台设计的现代化 UI 工具包。
- **Compose Multiplatform (CMP)**：由 JetBrains 基于 JPC 扩展而来，目标是让同一套 Compose 代码能运行在 **iOS、Desktop (Windows/macOS/Linux)、Web** 等多端。

你可以把 CMP 理解为 JPC 的“多平台移植版”，它们共享绝大部分核心 API。

---

### 2. 相同点 (90%+)

绝大部分 Compose 开发知识都是通用的，包括：

- **核心 API**：`@Composable`、`remember`、`mutableStateOf`、`LaunchedEffect` 等。
- **布局组件**：`Row`、`Column`、`Box`、`LazyColumn` 等。
- **Material Design**：`Scaffold`、`TopAppBar`、`Button`、`Text` 等 M3/M2 组件。
- **编程思想**：UI = f(State)、单向数据流、重组机制。
- **修饰符 (Modifier)**：几乎所有 `Modifier` 的用法都一致。

---

### 3. 不同点及处理策略

差异主要源于各平台独有的能力与限制。

| 类别 | Jetpack Compose (Android) | Compose Multiplatform (KMP) |
|---|---|---|
| **导航** | 使用 `androidx.navigation:navigation-compose` | 需第三方库 (Decompose / Voyager) |
| **资源** | `painterResource(id = R.drawable.icon)` | `painterResource(res = Res.drawable.icon)` |
| **平台上下文** | 可用 `LocalContext.current` 获取 `Context` | 无统一上下文，需 `expect/actual` |
| **平台 API** | 直接调用 Android SDK | 通过 `expect/actual` 访问原生 API |
| **项目结构** | 标准 Android Gradle | KMP Gradle (`commonMain` / `androidMain` / `iosMain` 等) |

---

### 4. 平台上下文差异详解

`LocalContext.current` 是 Android 开发中最常用的 API 之一，可以获取 `Context`，进而访问文件、数据库、网络状态、系统服务等。但在 KMP `commonMain` 中，**不存在这样一个通用的“上下文”对象**，因为每个平台提供的能力各不相同。

#### 4.1 带来的问题

当你需要在共享代码 (`commonMain`) 中执行以下操作时，会遇到平台差异：

- **文件读写**：iOS、Android、Desktop 的文件路径与 API 完全不同。
- **网络状态监听**：各平台 API 不一。
- **设备信息获取**：如设备 ID、屏幕尺寸等。
- **调用原生 UI 组件**：如显示一个原生 Toast 或 UIAlert。
- **数据库访问**：需要平台专属的 DB Driver（如 Android SQLite vs iOS CoreData）。

#### 4.2 处理策略：`expect`/`actual`

`expect`/`actual` 是 Kotlin Multiplatform 的核心机制，用于处理平台差异：

1. **在 `commonMain` 中声明 `expect`**：定义一个函数、类或属性的“期望”接口，但不提供实现。
   ```kotlin
   // commonMain/utils/FileSystem.kt
   expect fun saveFile(fileName: String, content: ByteArray): Boolean
   ```

2. **在各平台源集中提供 `actual` 实现**：
   - **`androidMain`**：
     ```kotlin
     // androidMain/utils/FileSystem.kt
     actual fun saveFile(fileName: String, content: ByteArray): Boolean {
         val context: Context = getApplicationContext() // 获取 Android Context
         context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
             it.write(content)
         }
         return true
     }
     ```
   - **`iosMain`**：
     ```kotlin
     // iosMain/utils/FileSystem.kt
     import platform.Foundation.NSDocumentDirectory
     // ...
     actual fun saveFile(fileName: String, content: ByteArray): Boolean {
         val path = NSDocumentDirectory?.path
         // ... 使用 iOS API 写入文件
         return true
     }
     ```

3. **在 `commonMain` 中调用 `expect` 函数**：
   ```kotlin
   // 在你的 Composable 或 ViewModel (commonMain)
   fun onSaveButtonClick() {
       saveFile("my_data.txt", "Hello KMP".encodeToByteArray())
   }
   ```

编译时，KMP 会自动根据目标平台链接到对应的 `actual` 实现。

---

### 5. 总结

- **放心学 Jetpack Compose**：90% 的知识可直接用于 KMP。
- **关注平台差异点**：导航、资源、平台 API 调用是主要区别。
- **善用 `expect`/`actual`**：这是处理平台上下文差异、调用原生能力的最标准方案。
- **考虑第三方库**：很多库（如 Ktor for HTTP, SQLDelight for DB）已经封装好了 `expect/actual`，可以直接在 `commonMain` 中使用。

通过这种方式，你可以在最大化代码复用的同时，保留访问各平台原生功能的能力。
