plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "org.example.project"
version = "1.0.0"
application {
    mainClass.set("org.example.project.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    // ContentNegotiation 和 JSON 序列化支持
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // --- 数据库依赖 ---
    // Exposed 核心库，提供了 DSL 功能
    implementation(libs.exposed.core)
    // Exposed DAO API，提供了对象关系映射 (ORM) 功能
    implementation(libs.exposed.dao)
    // Exposed JDBC 驱动，用于连接数据库
    implementation(libs.exposed.jdbc)
    // H2 数据库驱动，我们使用的内存数据库
    implementation(libs.h2)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}