import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 插件声明：Shared 模块作为 KMP Library，需要 KMP 能力 + Android 库发布能力。
plugins {
    alias(libs.plugins.kotlinMultiplatform) // 启用 Kotlin Multiplatform，输出多个平台的共享代码。
    alias(libs.plugins.androidLibrary) // 以 Android Library 形式编译，供 app 模块依赖。
}

// KMP 目标平台配置：声明 Shared 模块要产出的所有 Target。
kotlin {
    androidTarget {
        compilerOptions {
            // 与主应用保持一致，产出符合 Android 要求的 Java 11 字节码。
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64() // iOS 真机框架。
    iosSimulatorArm64() // iOS 模拟器框架。

    jvm() // JVM（桌面 / Server 等）目标，支持在 JVM 环境复用 Shared 代码。

    js {
        browser() // Kotlin/JS 浏览器目标，面向 Web 端使用 Shared 逻辑。
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser() // WebAssembly 目标，实验性支持 Compose WASM。
    }

    sourceSets {
        // 公共源集：放置跨平台共享逻辑，可在此继续添加通用依赖。
        commonMain.dependencies {
            // 例如添加 kotlinx.coroutines、kotlinx.serialization 等库放在这里。
            api(projects.kmplog)
        }
        // 公共测试源集：为 Shared 模块提供平台无关的单元测试能力。
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Android 构建配置：确保 Shared 模块作为 Android 库时的打包参数。
android {
    namespace = "org.example.project.shared" // 生成 R 类 / BuildConfig 的命名空间。
    compileSdk = libs.versions.android.compileSdk.get().toInt() // 编译期使用的 Android SDK 版本。
    compileOptions {
        // 与 Kotlin 目标一致，保证 Java 源/目标版本为 11。
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt() // 核心库最低支持的 Android 版本。
    }
}
