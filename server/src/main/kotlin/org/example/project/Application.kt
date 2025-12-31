package org.example.project

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.example.project.config.ConfigRateLimit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.example.project.db.DatabaseFactory
import org.example.project.db.apprun.AppRunService
import org.example.project.db.user.UserService
import org.example.project.routes.appRunInfo
import org.example.project.routes.productRoutes
import org.example.project.routes.userRoutes
import org.example.project.util.configureErrorHandling
import org.slf4j.event.Level

// 服务器配置常量
const val SERVER_PORT = 8080

fun main() {
    // 时区处理策略：
    // - 数据库统一存储 UTC 时间
    // - 应用层使用 DateTimeUtils.nowUTC() 获取 UTC 时间
    // - API 层可根据用户时区进行转换（如果需要）
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 配置统一错误处理（必须在最前面，以便捕获所有异常）
    configureErrorHandling()
    
    // 配置 CallLogging 请求日志插件
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.uri.startsWith("/health") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.local.method.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.uri
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, Path: $path"
        }
    }
    
    // 初始化数据库连接和表结构
    // dropExistingTable: false 表示生产模式，只进行增量更新，不删除现有数据
    DatabaseFactory.init(dropExistingTable = false)
    
    val userService = UserService()
    val appRunService = AppRunService()

    // 配置 ContentNegotiation 和 JSON 序列化
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
    
    // 配置 RateLimiting 限流插件
    install(RateLimit) {
        // 全局限流配置：每分钟最多 100 个请求
        register(RateLimitName(ConfigRateLimit.GLOBAL)) {
            rateLimiter(
                limit = System.getenv("RATE_LIMIT_REQUESTS")?.toIntOrNull() ?: 100,
                refillPeriod = System.getenv("RATE_LIMIT_WINDOW_SECONDS")?.toLongOrNull()?.let {
                    it.seconds
                } ?: 1.minutes
            )
        }
        // API 限流配置：每分钟最多 60 个请求
        register(RateLimitName(ConfigRateLimit.API)) {
            rateLimiter(
                limit = System.getenv("API_RATE_LIMIT_REQUESTS")?.toIntOrNull() ?: 60,
                refillPeriod = System.getenv("API_RATE_LIMIT_WINDOW_SECONDS")?.toLongOrNull()?.let {
                    it.seconds
                } ?: 1.minutes
            )
        }
    }
    
    routing {
        // 根路径
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        // API 版本控制：v1 版本
        route("/v1") {
            // 应用全局限流
            rateLimit(RateLimitName(ConfigRateLimit.GLOBAL)) {
                // 注册各个功能模块的路由
                productRoutes()
                userRoutes(userService)
                appRunInfo(appRunService)
            }
        }
    }
}