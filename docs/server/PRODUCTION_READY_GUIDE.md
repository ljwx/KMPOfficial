# 商用后端服务搭建完整指南

本文档提供从零开始搭建一个可商用、生产就绪的 Ktor 后端服务的完整指南。涵盖数据库迁移、日志、限流、错误处理、安全配置等所有关键环节。

---

## 目录

1. [项目初始化](#1-项目初始化)
2. [数据库配置与迁移](#2-数据库配置与迁移)
3. [日志系统](#3-日志系统)
4. [统一错误处理](#4-统一错误处理)
5. [API 限流](#5-api-限流)
6. [安全配置](#6-安全配置)
7. [环境变量配置](#7-环境变量配置)
8. [健康检查与监控](#8-健康检查与监控)
9. [API 版本控制](#9-api-版本控制)
10. [数据验证](#10-数据验证)
11. [连接池配置](#11-连接池配置)
12. [部署准备](#12-部署准备)
13. [测试策略](#13-测试策略)
14. [最佳实践总结](#14-最佳实践总结)

---

## 1. 项目初始化

### 1.1 创建 Gradle 项目结构

```kotlin
// server/build.gradle.kts
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
```

### 1.2 添加核心依赖

```kotlin
dependencies {
    // Ktor 核心
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    
    // JSON 序列化
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // 日志
    implementation(libs.logback)
    implementation(libs.ktor.serverCallLogging)
    
    // 错误处理
    implementation(libs.ktor.serverStatusPages)
    
    // 限流
    implementation(libs.ktor.serverRateLimiting)
    
    // 数据库
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javatime)
    implementation(libs.mysql.connector)
    implementation(libs.hikaricp)
    
    // 测试
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
```

---

## 2. 数据库配置与迁移

### 2.1 数据库连接配置

**关键原则：**
- ✅ 使用连接池（HikariCP）管理数据库连接
- ✅ 通过环境变量配置敏感信息（密码、URL）
- ✅ 生产环境必须使用 `dropExistingTable = false`
- ✅ 实现表结构兼容性检查
- ✅ 支持增量更新（添加缺失列）

**实现示例：**

```kotlin
// server/src/main/kotlin/org/example/project/db/DatabaseFactory.kt
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    /**
     * 检查表结构是否兼容
     */
    private fun isTableStructureCompatible(metaData: DatabaseMetaData, tableName: String): Boolean {
        val requiredColumns = mapOf(
            "id" to "INT",
            "username" to "VARCHAR",
            "password" to "VARCHAR",
            "status" to "VARCHAR",
            "role" to "VARCHAR",
            "created_at" to "DATETIME",
            "updated_at" to "DATETIME"
        )
        
        val columns = metaData.getColumns(null, null, tableName, null)
        val existingColumns = mutableMapOf<String, String>()
        
        while (columns.next()) {
            val columnName = columns.getString("COLUMN_NAME").lowercase()
            val dataType = columns.getString("TYPE_NAME").uppercase()
            existingColumns[columnName] = dataType
        }
        
        // 检查所有必需列是否存在
        for ((columnName, expectedType) in requiredColumns) {
            val actualType = existingColumns[columnName]
            if (actualType == null) {
                logger.warn("缺少必需列: $columnName")
                return false
            }
            
            val typeMatches = when {
                expectedType == "VARCHAR" -> actualType.contains("VARCHAR") || actualType.contains("CHAR")
                expectedType == "INT" -> actualType.contains("INT")
                expectedType == "DATETIME" -> actualType.contains("DATETIME") || actualType.contains("TIMESTAMP")
                else -> actualType.contains(expectedType)
            }
            
            if (!typeMatches) {
                logger.warn("列 $columnName 类型不匹配: 期望 $expectedType，实际 $actualType")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 初始化数据库连接和表结构
     * 
     * @param dropExistingTable 是否强制删除已存在的表（仅用于开发环境）
     *                          生产环境必须设置为 false，避免数据丢失
     */
    fun init(dropExistingTable: Boolean = false) {
        val driverClassNameValue = "com.mysql.cj.jdbc.Driver"
        val jdbcURL = System.getenv("JDBC_URL") 
            ?: "jdbc:mysql://localhost:3306/dbname?useSSL=false&serverTimezone=UTC"
        val user = System.getenv("DB_USER") ?: "root"
        val password = System.getenv("DB_PASSWORD") 
            ?: throw IllegalStateException("生产环境必须设置 DB_PASSWORD 环境变量")

        // 配置 HikariCP 连接池
        val config = HikariConfig().apply {
            jdbcUrl = jdbcURL
            driverClassName = driverClassNameValue
            username = user
            this.password = password
            
            // 连接池配置（可通过环境变量覆盖）
            maximumPoolSize = System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: 10
            minimumIdle = System.getenv("DB_MIN_IDLE")?.toIntOrNull() ?: 5
            connectionTimeout = System.getenv("DB_CONNECTION_TIMEOUT")?.toLongOrNull() ?: 30000L
            idleTimeout = System.getenv("DB_IDLE_TIMEOUT")?.toLongOrNull() ?: 600000L
            maxLifetime = System.getenv("DB_MAX_LIFETIME")?.toLongOrNull() ?: 1800000L
            poolName = "HikariCP-Pool"
            connectionTestQuery = "SELECT 1"
            leakDetectionThreshold = System.getenv("DB_LEAK_DETECTION_THRESHOLD")?.toLongOrNull() ?: 60000L
        }
        
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        
        // 初始化表结构（使用事务确保原子性）
        transaction(database) {
            val directConnection = dataSource.connection
            try {
                val metaData = directConnection.metaData
                val tables = metaData.getTables(null, null, "users", null)
                val tableExists = tables.next()
                
                if (tableExists) {
                    if (dropExistingTable) {
                        // 开发环境：强制删除重建
                        logger.info("强制删除模式：删除旧表 users")
                        directConnection.createStatement().executeUpdate("DROP TABLE users")
                        SchemaUtils.create(Users)
                        logger.info("已创建新表 users")
                    } else {
                        // 生产环境：只进行增量更新
                        val isCompatible = isTableStructureCompatible(metaData, "users")
                        
                        if (isCompatible) {
                            logger.info("表结构兼容，使用增量更新模式")
                            SchemaUtils.createMissingTablesAndColumns(Users)
                            logger.info("已完成表结构更新")
                        } else {
                            // 表结构不兼容，抛出异常要求使用迁移工具
                            val errorMessage = """
                                |表结构不兼容，但为了数据安全，不会自动删除表。
                                |
                                |请使用以下方式之一进行迁移：
                                |1. 使用数据库迁移工具（推荐）：Flyway 或 Liquibase
                                |2. 手动迁移：备份数据 -> 删除表 -> 创建新表 -> 恢复数据
                                |3. 开发环境：设置 dropExistingTable = true（仅限开发环境）
                            """.trimMargin()
                            
                            logger.error("表结构不兼容: $errorMessage")
                            throw IllegalStateException(
                                "表结构不兼容且无法自动修复。$errorMessage"
                            )
                        }
                    }
                } else {
                    // 表不存在，直接创建
                    logger.info("表 users 不存在，创建新表")
                    SchemaUtils.create(Users)
                    logger.info("已创建新表 users")
                }
            } finally {
                directConnection.close()
            }
        }
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
```

### 2.2 数据库迁移最佳实践

**推荐使用 Flyway 或 Liquibase 进行数据库迁移：**

1. **Flyway 集成示例：**

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.flywaydb:flyway-core:9.22.0")
    implementation("org.flywaydb:flyway-mysql:9.22.0")
}

// DatabaseFactory.kt
import org.flywaydb.core.Flyway

fun init(dropExistingTable: Boolean = false) {
    // ... 连接池配置 ...
    
    // 运行 Flyway 迁移
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
    
    flyway.migrate()
    
    // ... 表结构检查 ...
}
```

2. **迁移文件结构：**

```
server/src/main/resources/db/migration/
├── V1__Create_users_table.sql
├── V2__Add_email_column.sql
└── V3__Add_index_on_username.sql
```

---

## 3. 日志系统

### 3.1 配置 Logback

**创建 `server/src/main/resources/logback.xml`：**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
    
    <!-- 生产环境可以设置为 WARN 减少日志量 -->
    <logger name="org.example.project" level="${LOG_LEVEL:-INFO}" />
</configuration>
```

### 3.2 配置请求日志

```kotlin
// Application.kt
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        // 过滤健康检查端点
        filter { call -> !call.request.path().startsWith("/health") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val remoteAddress = call.request.origin.remoteAddress
            val responseTime = call.responseTime()
            
            "Status: $status, Method: $httpMethod, Path: $path, " +
            "Remote: $remoteAddress, User-Agent: $userAgent, ResponseTime: ${responseTime}ms"
        }
    }
}
```

### 3.3 日志最佳实践

- ✅ 使用结构化日志（JSON 格式）便于日志分析
- ✅ 不要在日志中记录敏感信息（密码、token）
- ✅ 使用适当的日志级别（DEBUG/INFO/WARN/ERROR）
- ✅ 配置日志轮转，避免磁盘空间耗尽
- ✅ 生产环境使用异步日志写入

---

## 4. 统一错误处理

### 4.1 实现统一错误处理器

```kotlin
// server/src/main/kotlin/org/example/project/util/ErrorHandler.kt
package org.example.project.util

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.example.project.network.model.BaseApiResponse
import org.slf4j.LoggerFactory

/**
 * 统一错误处理配置
 * 用于处理所有未捕获的异常，返回统一的错误响应格式
 */
fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandler")
    
    install(StatusPages) {
        // 处理所有异常
        exception<Throwable> { call, cause ->
            logger.error("未处理的异常", cause)
            
            // 根据异常类型返回不同的状态码和消息
            val (statusCode, message) = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest to "请求参数错误: ${cause.message}"
                is IllegalStateException -> HttpStatusCode.BadRequest to "请求状态错误: ${cause.message}"
                is NoSuchElementException -> HttpStatusCode.NotFound to "资源不存在: ${cause.message}"
                is SecurityException -> HttpStatusCode.Forbidden to "访问被拒绝: ${cause.message}"
                is UnsupportedOperationException -> HttpStatusCode.NotImplemented to "功能未实现: ${cause.message}"
                else -> {
                    // 生产环境不暴露详细错误信息
                    val isProduction = System.getenv("ENVIRONMENT") == "production"
                    if (isProduction) {
                        HttpStatusCode.InternalServerError to "服务器内部错误，请稍后重试"
                    } else {
                        HttpStatusCode.InternalServerError to "服务器内部错误: ${cause.message}"
                    }
                }
            }
            
            call.respond(
                statusCode,
                BaseApiResponse<Nothing>(
                    code = statusCode.value,
                    message = message,
                    data = null
                )
            )
        }
        
        // 处理 HTTP 状态码错误
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                BaseApiResponse<Nothing>(
                    code = status.value,
                    message = "请求的资源不存在",
                    data = null
                )
            )
        }
        
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(
                status,
                BaseApiResponse<Nothing>(
                    code = status.value,
                    message = "不支持的请求方法",
                    data = null
                )
            )
        }
    }
}
```

### 4.2 在应用中使用

```kotlin
fun Application.module() {
    // 必须在最前面配置，以便捕获所有异常
    configureErrorHandling()
    
    // ... 其他配置 ...
}
```

---

## 5. API 限流

### 5.1 配置限流插件

```kotlin
// Application.kt
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.module() {
    install(RateLimit) {
        // 全局限流配置：每分钟最多 100 个请求
        register(RateLimitName("global")) {
            rateLimiter(
                limit = System.getenv("RATE_LIMIT_REQUESTS")?.toIntOrNull() ?: 100,
                refillPeriod = System.getenv("RATE_LIMIT_WINDOW_SECONDS")?.toLongOrNull()?.let {
                    it.seconds
                } ?: 1.minutes
            )
        }
        
        // API 限流配置：每分钟最多 60 个请求
        register(RateLimitName("api")) {
            rateLimiter(
                limit = System.getenv("API_RATE_LIMIT_REQUESTS")?.toIntOrNull() ?: 60,
                refillPeriod = System.getenv("API_RATE_LIMIT_WINDOW_SECONDS")?.toLongOrNull()?.let {
                    it.seconds
                } ?: 1.minutes
            )
        }
        
        // 登录接口限流：每分钟最多 5 次
        register(RateLimitName("login")) {
            rateLimiter(
                limit = System.getenv("LOGIN_RATE_LIMIT_REQUESTS")?.toIntOrNull() ?: 5,
                refillPeriod = 1.minutes
            )
        }
    }
    
    routing {
        route("/v1") {
            // 应用全局限流
            rateLimit(RateLimitName("global")) {
                route("/products") {
                    rateLimit(RateLimitName("api")) {
                        get { /* ... */ }
                    }
                }
                
                route("/auth") {
                    route("/login") {
                        rateLimit(RateLimitName("login")) {
                            post { /* ... */ }
                        }
                    }
                }
            }
        }
    }
}
```

### 5.2 限流最佳实践

- ✅ 不同接口使用不同的限流策略
- ✅ 登录、注册等敏感接口使用更严格的限流
- ✅ 通过环境变量配置限流参数，便于调整
- ✅ 返回 429 Too Many Requests 状态码
- ✅ 考虑使用 Redis 实现分布式限流

---

## 6. 安全配置

### 6.1 密码加密

**使用 BCrypt 加密密码：**

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
}

// UserService.kt
import org.mindrot.jbcrypt.BCrypt

class UserService {
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
}
```

### 6.2 CORS 配置

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.ktor.serverCors)
}

// Application.kt
import io.ktor.server.plugins.cors.routing.*

fun Application.module() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        
        // 生产环境应该指定具体域名
        val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")?.split(",") 
            ?: listOf("http://localhost:3000")
        allowedOrigins.forEach { origin ->
            allowHost(origin, schemes = listOf("http", "https"))
        }
        
        allowCredentials = true
        maxAgeInDays = 1
    }
}
```

### 6.3 HTTPS 配置

**生产环境必须使用 HTTPS：**

```kotlin
// Application.kt
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

fun main() {
    val environment = applicationEngineEnvironment {
        if (System.getenv("ENVIRONMENT") == "production") {
            // 生产环境使用 HTTPS
            connector {
                port = 8080
                host = "0.0.0.0"
            }
            
            sslConnector(
                keyStore = loadKeyStore(),
                keyStorePassword = System.getenv("SSL_KEYSTORE_PASSWORD") ?: "",
                privateKeyPassword = System.getenv("SSL_KEY_PASSWORD") ?: "",
                keyAlias = "ktor"
            ) {
                port = 8443
                host = "0.0.0.0"
            }
        } else {
            // 开发环境使用 HTTP
            connector {
                port = 8080
                host = "0.0.0.0"
            }
        }
        
        module(Application::module)
    }
    
    embeddedServer(Netty, environment).start(wait = true)
}
```

### 6.4 安全头配置

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.ktor.serverDefaultHeaders)
}

// Application.kt
import io.ktor.server.plugins.defaultheaders.*

fun Application.module() {
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
    }
}
```

---

## 7. 环境变量配置

### 7.1 必需的环境变量

创建 `.env.example` 文件：

```bash
# 环境标识
ENVIRONMENT=production

# 数据库配置
JDBC_URL=jdbc:mysql://localhost:3306/dbname?useSSL=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=your_secure_password_here
DB_MAX_POOL_SIZE=20
DB_MIN_IDLE=10
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION_THRESHOLD=60000

# 限流配置
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW_SECONDS=60
API_RATE_LIMIT_REQUESTS=60
API_RATE_LIMIT_WINDOW_SECONDS=60
LOGIN_RATE_LIMIT_REQUESTS=5

# CORS 配置
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# SSL 配置（生产环境）
SSL_KEYSTORE_PASSWORD=your_keystore_password
SSL_KEY_PASSWORD=your_key_password

# 日志级别
LOG_LEVEL=INFO

# JWT 配置（如果使用）
JWT_SECRET=your_jwt_secret_key_here
JWT_ISSUER=your-app-name
JWT_AUDIENCE=your-app-audience
JWT_REALM=your-app-realm
```

### 7.2 环境变量管理

- ✅ 使用 `.env` 文件（开发环境）
- ✅ 使用容器环境变量（生产环境）
- ✅ 使用密钥管理服务（AWS Secrets Manager、HashiCorp Vault）
- ✅ 不要在代码中硬编码敏感信息
- ✅ 使用 `.gitignore` 忽略 `.env` 文件

---

## 8. 健康检查与监控

### 8.1 健康检查端点

```kotlin
// Application.kt
routing {
    get("/health") {
        val dbHealthy = try {
            DatabaseFactory.dbQuery { 
                org.jetbrains.exposed.sql.select(1).first()
            }
            true
        } catch (e: Exception) {
            false
        }
        
        val healthStatus = if (dbHealthy) {
            HttpStatusCode.OK
        } else {
            HttpStatusCode.ServiceUnavailable
        }
        
        call.respond(
            healthStatus,
            mapOf(
                "status" to if (dbHealthy) "UP" else "DOWN",
                "database" to if (dbHealthy) "UP" else "DOWN",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    get("/health/ready") {
        // 就绪检查：服务是否准备好接收流量
        call.respond(HttpStatusCode.OK, mapOf("status" to "READY"))
    }
    
    get("/health/live") {
        // 存活检查：服务是否还在运行
        call.respond(HttpStatusCode.OK, mapOf("status" to "ALIVE"))
    }
}
```

### 8.2 指标监控

**使用 Micrometer 集成 Prometheus：**

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.0")
}

// Application.kt
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

routing {
    get("/metrics") {
        call.respondText(prometheusRegistry.scrape(), ContentType.Text.Plain)
    }
}
```

---

## 9. API 版本控制

### 9.1 版本化路由

```kotlin
routing {
    // API 版本控制：v1 版本
    route("/v1") {
        route("/users") {
            get { /* v1 实现 */ }
        }
    }
    
    // API 版本控制：v2 版本
    route("/v2") {
        route("/users") {
            get { /* v2 实现 */ }
        }
    }
}
```

### 9.2 版本控制最佳实践

- ✅ 使用 URL 路径版本控制（`/v1/`, `/v2/`）
- ✅ 保持向后兼容性
- ✅ 废弃旧版本时提供足够的迁移时间
- ✅ 在响应头中返回 API 版本信息

---

## 10. 数据验证

### 10.1 请求验证

```kotlin
// dto/CreateUserRequest.kt
data class CreateUserRequest(
    val username: String,
    val email: String?,
    val password: String,
    val nickname: String?
) {
    fun validate(): String? {
        return when {
            username.isBlank() -> "用户名不能为空"
            username.length < 3 -> "用户名长度至少 3 个字符"
            username.length > 50 -> "用户名长度不能超过 50 个字符"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "用户名只能包含字母、数字和下划线"
            email != null && !isValidEmail(email) -> "邮箱格式不正确"
            password.isBlank() -> "密码不能为空"
            password.length < 8 -> "密码长度至少 8 个字符"
            else -> null
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

// Application.kt
post("/v1/users") {
    val request = call.receive<CreateUserRequest>()
    
    val validationError = request.validate()
    if (validationError != null) {
        call.respond(
            HttpStatusCode.BadRequest,
            BaseApiResponse<Nothing>(
                code = 400,
                message = validationError,
                data = null
            )
        )
        return@post
    }
    
    // ... 处理逻辑 ...
}
```

---

## 11. 连接池配置

### 11.1 HikariCP 最佳配置

```kotlin
val config = HikariConfig().apply {
    jdbcUrl = jdbcURL
    driverClassName = driverClassNameValue
    username = user
    password = password
    
    // 连接池大小
    maximumPoolSize = 20  // 根据数据库服务器性能调整
    minimumIdle = 10     // 保持的最小空闲连接数
    
    // 连接超时
    connectionTimeout = 30000L  // 30秒
    
    // 空闲连接超时
    idleTimeout = 600000L  // 10分钟
    
    // 连接最大生命周期
    maxLifetime = 1800000L  // 30分钟（MySQL 默认 wait_timeout 是 8 小时）
    
    // 连接泄漏检测
    leakDetectionThreshold = 60000L  // 60秒
    
    // 连接测试查询
    connectionTestQuery = "SELECT 1"
    
    // 连接池名称（便于监控）
    poolName = "HikariCP-Pool"
    
    // 其他优化配置
    isAutoCommit = false  // 禁用自动提交，使用事务
    transactionIsolation = "TRANSACTION_READ_COMMITTED"
}
```

### 11.2 连接池监控

```kotlin
// 定期输出连接池状态
val hikariPool = dataSource.hikariPoolMXBean
logger.info("连接池状态 - 活跃连接: ${hikariPool.activeConnections}, " +
           "空闲连接: ${hikariPool.idleConnections}, " +
           "等待线程: ${hikariPool.threadsAwaitingConnection}")
```

---

## 12. 部署准备

### 12.1 Dockerfile

```dockerfile
FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:build --no-daemon

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/server/build/libs/server-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 设置时区
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 12.2 docker-compose.yml

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - ENVIRONMENT=production
      - JDBC_URL=jdbc:mysql://db:3306/dbname?useSSL=false&serverTimezone=UTC
      - DB_USER=root
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on:
      - db
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
      - MYSQL_DATABASE=dbname
    volumes:
      - db_data:/var/lib/mysql
    restart: unless-stopped

volumes:
  db_data:
```

### 12.3 部署检查清单

- [ ] 所有环境变量已配置
- [ ] 数据库连接正常
- [ ] 日志目录有写权限
- [ ] HTTPS 证书已配置（生产环境）
- [ ] 健康检查端点正常
- [ ] 限流配置合理
- [ ] 备份策略已制定
- [ ] 监控告警已配置
- [ ] 文档已更新

---

## 13. 测试策略

### 13.1 单元测试

```kotlin
// UserServiceTest.kt
class UserServiceTest {
    @Test
    fun `test create user`() = runTest {
        val userService = UserService()
        val request = CreateUserRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        )
        
        val user = userService.createUser(request, "hashed_password")
        assertNotNull(user.id)
        assertEquals("testuser", user.username)
    }
}
```

### 13.2 集成测试

```kotlin
// ApplicationTest.kt
class ApplicationTest {
    @Test
    fun `test GET users endpoint`() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/v1/users").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
```

---

## 14. 最佳实践总结

### 14.1 代码组织

```
server/src/main/kotlin/org/example/project/
├── Application.kt              # 应用入口和配置
├── db/                        # 数据库相关
│   ├── DatabaseFactory.kt    # 数据库连接和初始化
│   ├── User.kt               # 数据模型
│   ├── UserService.kt        # 业务逻辑
│   └── dto/                  # 数据传输对象
├── network/                   # 网络相关
│   └── model/                # API 响应模型
├── routes/                    # 路由定义
│   ├── UserRoutes.kt
│   └── ProductRoutes.kt
└── util/                      # 工具类
    ├── ErrorHandler.kt        # 错误处理
    └── DateTimeUtils.kt      # 时间工具
```

### 14.2 关键原则

1. **安全性**
   - ✅ 密码必须加密存储
   - ✅ 使用 HTTPS（生产环境）
   - ✅ 实施 API 限流
   - ✅ 输入验证和 SQL 注入防护
   - ✅ 敏感信息通过环境变量配置

2. **可靠性**
   - ✅ 统一错误处理
   - ✅ 数据库连接池管理
   - ✅ 健康检查端点
   - ✅ 日志记录和监控

3. **可维护性**
   - ✅ 代码分层清晰
   - ✅ 统一的 API 响应格式
   - ✅ 完善的文档
   - ✅ 版本控制

4. **性能**
   - ✅ 数据库连接池优化
   - ✅ 异步处理（协程）
   - ✅ 合理的限流策略
   - ✅ 索引优化

5. **可扩展性**
   - ✅ API 版本控制
   - ✅ 数据库迁移支持
   - ✅ 微服务架构准备
   - ✅ 配置外部化

---

## 附录：快速启动检查清单

### 开发环境

1. ✅ 安装 JDK 17+
2. ✅ 安装 MySQL 8.0+
3. ✅ 配置 `.env` 文件
4. ✅ 运行 `./gradlew :server:run`
5. ✅ 测试健康检查：`curl http://localhost:8080/health`

### 生产环境

1. ✅ 配置所有必需的环境变量
2. ✅ 设置 `ENVIRONMENT=production`
3. ✅ 配置 HTTPS 证书
4. ✅ 设置 `dropExistingTable = false`
5. ✅ 配置数据库备份策略
6. ✅ 配置日志轮转
7. ✅ 配置监控和告警
8. ✅ 进行压力测试
9. ✅ 制定回滚计划

---

## 参考资源

- [Ktor 官方文档](https://ktor.io/docs/)
- [Exposed 文档](https://github.com/JetBrains/Exposed)
- [HikariCP 配置指南](https://github.com/brettwooldridge/HikariCP)
- [Flyway 文档](https://flywaydb.org/documentation/)
- [OWASP 安全指南](https://owasp.org/)

---

**文档版本：** 1.0.0  
**最后更新：** 2024年

