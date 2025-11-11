import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 插件声明：决定当前 Gradle Module 可用的功能集合。
plugins {
    // Kotlin Multiplatform：允许同一个工程生成 Android / iOS / JVM / JS / WASM 等平台产物。
    alias(libs.plugins.kotlinMultiplatform)
    // Android Application：提供打包 Android APK / AAB 的任务与配置入口。
    alias(libs.plugins.androidApplication)
    // Compose Multiplatform：引入 Compose UI 在多端的核心 Gradle 配置。
    alias(libs.plugins.composeMultiplatform)
    // Compose Compiler：启用 Compose 专用 Kotlin 编译器插件，保障重组等语义正常。
    alias(libs.plugins.composeCompiler)
    // Compose Hot Reload：开发期快速预览 / 热更新支持。
    alias(libs.plugins.composeHotReload)
    // Kotlinx Serialization：生成跨平台序列化代码（Decompose 配置保存需要）。
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Android 目标：输出 Android 平台所需的字节码与资源。
    androidTarget {
        compilerOptions {
            // 与 Android Gradle 插件保持一致，产出 Java 11 目标字节码。
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // iOS 真机与模拟器目标：生成可供 Apple 平台调用的 KMP Framework。
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // 与模板保持一致，统一输出 Framework 名称。
            baseName = "ComposeApp"
            // 静态库便于在 Xcode 中直接链接，无需额外 runtime。
            isStatic = true
            export(projects.shared)// 将 shared 模块的 API 暴露给 iOS
            export(projects.kmplog)// 将 kmplog 模块的 API 暴露给 iOS
            export(libs.kotlinx.datetime)// 显式导出 kmplog 的传递依赖（KMP export 不会自动传递）
        }
    }

    // JVM (桌面) 目标：用于 Compose Desktop 应用。
    jvm()

    // JS 目标：开启 Compose Web（Kotlin/JS）浏览器构建。
    js {
        browser() // 生成面向浏览器的 bundle，并启用 webpack dev server。
        binaries.executable() // 输出可直接运行的 JS 入口文件。
    }

    // WASM 目标：实验性地启用 Compose WebAssembly 支持。
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser() // 与 JS 一样，面向浏览器环境。
        binaries.executable() // 输出 wasm 主入口，便于直接运行。
    }

    // 源集依赖：分别声明各平台 / 公共代码所需库。
    sourceSets {
        // Android 专属依赖：预览与 Activity 集成。
        androidMain.dependencies {
            implementation(compose.preview) // Compose 预览支持。
            implementation(libs.androidx.activity.compose) // 与 ComponentActivity 交互。
        }
        // 公共业务层依赖：所有平台共享的 Compose 与业务模块。
        commonMain.dependencies {
            implementation(compose.runtime) // Compose 核心运行时。
            implementation(compose.foundation) // 基础 UI 组件（Row/Column 等）。
            implementation(compose.material3) // Material 3 UI 组件库。
            implementation(compose.materialIconsExtended) // 官方 Material 图标集合。
            implementation(compose.ui) // UI 核心 API（绘制、布局）。
            implementation(compose.components.resources) // 访问多平台资源（图片、字符串）。
            implementation(compose.components.uiToolingPreview) // KMP 预览工具支持。
            implementation(libs.androidx.lifecycle.viewmodelCompose) // ViewModel in Compose。
            implementation(libs.androidx.lifecycle.runtimeCompose) // Lifecycle 感知能力。
            implementation("com.arkivanov.decompose:decompose:3.3.0") // 多平台导航核心。
            implementation("com.arkivanov.decompose:extensions-compose:3.3.0") // Compose 集成辅助。
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3") // Decompose 3.x 使用 kotlinx.serialization 保存配置。
            api(projects.shared) // 引入共享业务逻辑模块，导出给 iOS Framework 使用。
            api(projects.kmplog) // 导出跨平台日志模块，确保 Clock.System 打包进 iOS Framework。
        }
        // 公共测试依赖：提供 Kotlin/Multiplatform 单元测试能力。
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // JVM (桌面) 端专属依赖。
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs) // 自动选择当前桌面的 Compose runtime。
            implementation(libs.kotlinx.coroutinesSwing) // Swing 事件循环上的协程支持。
        }
    }
}

// Android Gradle 插件专属配置：决定 Android 产物打包细节。
android {
    namespace = "org.example.project" // 生成 R 类与 Manifest 使用的命名空间。
    compileSdk = libs.versions.android.compileSdk.get().toInt() // 编译时使用的 Android SDK 版本。

    defaultConfig {
        applicationId = "org.example.project" // 最终 Android 包名。
        minSdk = libs.versions.android.minSdk.get().toInt() // 最低支持的 Android 版本。
        targetSdk = libs.versions.android.targetSdk.get().toInt() // 目标适配 SDK 版本。
        versionCode = 1 // 内部版本号（用于升级顺序）。
        versionName = "1.0" // 对外展示的版本名称。
    }
    packaging {
        resources {
            // 排除常见的重复 license 文件，避免打包冲突。
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // 模板默认关闭混淆，方便调试；正式发版可酌情开启。
            isMinifyEnabled = false
        }
    }
    compileOptions {
        // 与 Kotlin 编译目标保持一致，确保 Java / Kotlin 都使用 Java 11 语法。
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// 额外依赖：仅在 debug 构建下启用 Compose 预览工具。
dependencies {
    debugImplementation(compose.uiTooling)
}

// Compose Desktop 特有配置：定义桌面应用入口与打包格式。
compose.desktop {
    application {
        mainClass = "org.example.project.MainKt" // JVM 桌面应用的入口函数所在类。

        nativeDistributions {
            // 同时产出 macOS DMG、Windows MSI、Linux DEB 三种安装包格式。
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project" // 安装包 App Name。
            packageVersion = "1.0.0" // 桌面安装包版本号。
        }
    }
}
