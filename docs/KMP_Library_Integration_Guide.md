
# 将 KMP 库集成到多平台应用的通用指南

本文档旨在提供一个清晰、可复用的流程，指导你如何将一个 Kotlin Multiplatform (KMP) 库（无论是你自己创建的还是第三方的）集成到一个跨平台应用中（例如本项目中的 `composeApp`）。

通过遵循这些步骤，你可以系统地完成配置，避免因平台差异（尤其是 iOS）导致的常见构建和链接错误。

---

## 核心概念

在 KMP 中，模块间的依赖关系是通过 Gradle 的 `sourceSets`（源集）来管理的。核心思想是：

1.  **`commonMain` 依赖**: 在公共源集 `commonMain` 中声明的依赖，将自动对所有目标平台（Android, iOS, JVM, WasmJs 等）生效。
2.  **`api` vs `implementation`**:
    *   `implementation`：依赖仅在模块内部可见。这是默认推荐的方式，可以加快编译速度。
    *   `api`：依赖会“传递”，即消费此模块的外部模块也能访问到这个依赖的 API。当你需要将库的接口暴露给最终的应用时，必须使用 `api`。
3.  **iOS Framework 导出 (`export`)**: 对于 iOS 平台，KMP 会将所有需要的代码编译成一个单一的 `.framework` 文件供 Xcode 使用。你必须在主应用模块（如 `composeApp`）的 `build.gradle.kts` 中明确使用 `export()` 来指定哪些库及其传递性依赖需要被包含并暴露给 Swift/Objective-C。**这是最关键且最容易出错的一步。**

---

## 集成步骤

假设你要集成一个名为 `my-new-library` 的新 KMP 库。

### 第一步：配置库模块 (`my-new-library/build.gradle.kts`)

这是库自身的构建脚本，它定义了库支持哪些平台以及它自己的依赖。

1.  **应用插件**: 确保应用了 `kotlin-multiplatform` 插件。如果需要作为 Android 库，还需 `com.android.library` 插件。

    ```kotlin
    plugins {
        alias(libs.plugins.kotlinMultiplatform)
        alias(libs.plugins.androidLibrary) // 如果支持 Android
    }
    ```

2.  **定义目标平台 (`targets`)**: 在 `kotlin { ... }` 代码块中，声明所有需要支持的平台。

    ```kotlin
    kotlin {
        // Android, iOS, JVM, JS, WasmJs...
        androidTarget()
        jvm()
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            // ... iOS 特有配置
        }
    }
    ```

3.  **声明依赖**: 在 `sourceSets.commonMain.dependencies` 中添加库自身的依赖。
    *   **关键点**: 如果你的库依赖了另一个库（例如 `my-new-library` 依赖 `kotlinx-datetime`），并且你希望使用 `my-new-library` 的应用也能直接访问 `kotlinx-datetime` 的 API，你必须使用 `api` 关键字。

    ```kotlin
    // 在 my-new-library/build.gradle.kts 中
    sourceSets {
        commonMain.dependencies {
            // 使用 api 会将 kotlinx-datetime 的 API 传递出去
            api(libs.kotlinx.datetime)
        }
    }
    ```

4.  **为 iOS 配置导出 (如果需要)**: 如果库有传递性依赖，为了让依赖它的模块（如 `shared` 或 `composeApp`）能更方便地配置，建议在库模块的 `framework` 配置中也 `export` 它自己的依赖。

    ```kotlin
    // 在 my-new-library/build.gradle.kts 的 iosTarget 配置中
    iosTarget.binaries.framework {
        baseName = "MyNewLibrary"
        // 明确导出自己的传递性依赖
        export(libs.kotlinx.datetime)
    }
    ```

### 第二步：在主应用模块中引入库 (`composeApp/build.gradle.kts`)

这是最终的应用模块，它消费 `my-new-library`。

1.  **注册模块**: 首先，确保你的新库模块已经被包含在项目中。在根目录的 `settings.gradle.kts` 文件里添加它。

    ```kotlin
    // settings.gradle.kts
    include(":my-new-library")
    ```

2.  **添加依赖**: 在主应用模块（如 `composeApp`）的 `build.gradle.kts` 中，将其添加到 `commonMain` 的依赖中。

    ```kotlin
    // composeApp/build.gradle.kts
    kotlin {
        sourceSets {
            commonMain.dependencies {
                // 使用 projects.myNewLibrary 引用本地模块
                api(projects.myNewLibrary)
            }
        }
    }
    ```
    *   **注意**: 这里使用 `api` 是因为 `composeApp` 是最终的应用，它需要将 `my-new-library` 的所有 API 都打包进最终的产物（尤其是 iOS Framework）中。

3.  **为 iOS 导出库和其所有传递性依赖**: 这是保证 iOS 正常链接和运行的核心步骤。
    *   在 `composeApp` 的 `iosTarget.binaries.framework` 配置中，你**必须** `export` 你刚刚添加的库。
    *   **最关键的一点**: 你还**必须** `export` 这个库所依赖的**所有**传递性依赖。Gradle 不会自动帮你完成这一步。

    ```kotlin
    // composeApp/build.gradle.kts
    kotlin {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true

                // 1. 导出你自己的库
                export(projects.myNewLibrary)

                // 2. 导出 my-new-library 的所有传递性依赖
                //    即使 my-new-library 内部已经 export 过，这里也要再次声明。
                //    这是 KMP 当前构建机制的要求。
                export(libs.kotlinx.datetime)
            }
        }
    }
    ```
    *   **示例**: 在本项目中，`composeApp` 不仅 `export(projects.kmplog)`，还必须 `export(libs.kotlinx.datetime)`，因为 `kmplog` 依赖了 `datetime`。如果缺少了对 `datetime` 的 `export`，在 Swift/Objective-C 代码中调用 `kmplog` 相关的 API 时就会出现链接错误（Linker Error）。

---

## 可选步骤：通过 `shared` 模块聚合

项目中通常会有一个 `shared` 模块，它的作用是作为一个“聚合器”或“中间层”，将多个底层的库（如 `kmplog`）组合起来，统一提供给 `composeApp`。

-   **优点**: 简化 `composeApp` 的配置，`composeApp` 只需依赖 `shared` 即可。
-   **流程**:
    1.  在 `shared/build.gradle.kts` 的 `commonMain` 中依赖你的新库: `api(projects.myNewLibrary)`。
    2.  在 `composeApp/build.gradle.kts` 中依赖 `shared`: `api(projects.shared)`。
    3.  在 `composeApp` 的 iOS `export` 配置中，导出 `shared`: `export(projects.shared)`。

-   **重要提醒**: 即使使用了 `shared` 模块，关于**传递性依赖的 `export` 问题依然存在**。你仍然需要在最终的 `composeApp` 的 `build.gradle.kts` 中，手动 `export` 所有底层库的传递性依赖（如 `kotlinx-datetime`）。`shared` 模块无法替你隐藏这个细节。

---

## 快速检查清单

当集成一个新 KMP 库时，请对照以下清单进行检查：

1.  **[ ] 库模块 (`my-new-library`)**:
    *   `build.gradle.kts` 中是否为传递性依赖使用了 `api`？
    *   `build.gradle.kts` 的 `iosTarget` 中是否 `export` 了它的传递性依赖？

2.  **[ ] 根项目**:
    *   `settings.gradle.kts` 中是否 `include(":my-new-library")`？

3.  **[ ] 主应用模块 (`composeApp`)**:
    *   `build.gradle.kts` 的 `commonMain` 依赖中是否添加了 `api(projects.myNewLibrary)`？
    *   `build.gradle.kts` 的 `iosTarget.binaries.framework` 配置中：
        *   是否 `export(projects.myNewLibrary)`？
        *   是否 `export` 了 `my-new-library` 的**所有**传递性依赖？（例如 `export(libs.kotlinx.datetime)`）

遵循以上流程和清单，你将能更顺利地在 KMP 项目中复用和集成各类库。
