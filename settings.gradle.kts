rootProject.name = "KotlinProject" // 设置整个工程的根名称，影响生成的 Gradle/IDE 工程名。
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS") // 启用 Gradle Typesafe Project Accessors，方便在代码中以属性方式引用子模块。

// 插件管理仓库：确定 Gradle 在解析插件时使用的远程源。
pluginManagement {
    repositories {
        google {
            mavenContent {
                // 限定仅解析 Android / Google 官方插件，加快同步速度。
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral() // Kotlin/Compose 等插件常驻仓库。
        gradlePluginPortal() // 其他第三方插件默认仓库。
    }
}

// 依赖解析仓库：供所有模块解析库依赖时使用的仓库列表。
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Foojay Toolchains 插件：自动解析 JDK 工具链，确保构建机具备所需 JDK 版本。
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 声明子模块：Compose 客户端、后端 Server、共享逻辑模块。
include(":composeApp")
include(":server")
include(":shared")