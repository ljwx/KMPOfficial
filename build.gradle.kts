// 项目级插件版本声明：集中管理插件，避免在子模块重复解析。
plugins {
    // Android Application 插件：供 app 模块按需启用，顶层声明后 apply false。
    alias(libs.plugins.androidApplication) apply false
    // Android Library 插件：供 shared / 其他库模块启用。
    alias(libs.plugins.androidLibrary) apply false
    // Compose Hot Reload：仅在 Compose 模块需要时启用。
    alias(libs.plugins.composeHotReload) apply false
    // Compose Multiplatform：UI 多平台支持插件。
    alias(libs.plugins.composeMultiplatform) apply false
    // Compose Compiler：Compose 的 Kotlin 编译器扩展。
    alias(libs.plugins.composeCompiler) apply false
    // Kotlin JVM：供纯 JVM 模块（如 server）使用。
    alias(libs.plugins.kotlinJvm) apply false
    // Kotlin Multiplatform：供需要多平台能力的模块启用。
    alias(libs.plugins.kotlinMultiplatform) apply false
    // Ktor 插件：供 server 模块使用的 Web 框架插件。
    alias(libs.plugins.ktor) apply false
}