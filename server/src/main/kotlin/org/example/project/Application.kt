package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.calllogging.CallLogging
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.example.project.db.DatabaseFactory
import org.example.project.db.UserService
import org.example.project.db.dto.CreateUserRequest
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData
import org.example.project.util.configureErrorHandling
import org.slf4j.event.Level

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
        filter { call -> !call.request.path().startsWith("/health") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, Path: $path"
        }
    }
    
    // 初始化数据库连接和表结构
    // dropExistingTable: false 表示生产模式，只进行增量更新，不删除现有数据
    DatabaseFactory.init(dropExistingTable = false)
    
    val userService = UserService()

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
    }
    
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        // API 版本控制：v1 版本
        route("/v1") {
            // 应用全局限流
            rateLimit(RateLimitName("global")) {
                // 产品相关 API
                route("/products") {
                    rateLimit(RateLimitName("api")) {
                        get {
                            val products: List<ProductSummaryData> = listOf(
                                ProductSummaryData(1, "商品A", 99.99, "这是商品A的描述"),
                                ProductSummaryData(2, "商品B", 199.99, "这是商品B的描述"),
                                ProductSummaryData(3, "商品C", 299.99, "这是商品C的描述"),
                                ProductSummaryData(4, "商品D", 399.99, "这是商品D的描述"),
                            )
                            
                            val response: BaseApiResponse<List<ProductSummaryData>> = BaseApiResponse(
                                code = 200,
                                message = "成功",
                                data = products
                            )
                            delay(3000)
                            call.respond(response)
                        }
                    }
                }

                // 用户相关 API
                route("/users") {
                    rateLimit(RateLimitName("api")) {
                        // GET /v1/users: 获取所有用户
                        get {
                            val users = userService.getAllUsers()
                            val userResponses = users.map { userService.toUserResponse(it) }
                            val response = BaseApiResponse(code = 200, message = "成功", data = userResponses)
                            call.respond(response)
                        }

                        // POST /v1/users: 创建一个新用户
                        post {
                            val request = call.receive<CreateUserRequest>()
                            
                            val validationError = request.validate()
                            if (validationError != null) {
                                call.respond(HttpStatusCode.BadRequest, BaseApiResponse<Nothing>(
                                    code = 400,
                                    message = validationError,
                                    data = null
                                ))
                                return@post
                            }
                            
                            val existingUser = userService.getUserByUsername(request.username)
                            if (existingUser != null) {
                                call.respond(HttpStatusCode.Conflict, BaseApiResponse<Nothing>(
                                    code = 409,
                                    message = "用户名已存在",
                                    data = null
                                ))
                                return@post
                            }
                            
                            request.email?.let { email ->
                                val existingEmailUser = userService.getUserByEmail(email)
                                if (existingEmailUser != null) {
                                    call.respond(HttpStatusCode.Conflict, BaseApiResponse<Nothing>(
                                        code = 409,
                                        message = "邮箱已被注册",
                                        data = null
                                    ))
                                    return@post
                                }
                            }
                            
                            // TODO: 生产环境必须使用密码加密库（如 BCrypt）对密码进行哈希
                            val passwordHash = request.password
                            
                            val createdUser = userService.createUser(request, passwordHash)
                            val userResponse = userService.toUserResponse(createdUser)
                            
                            call.respond(HttpStatusCode.Created, BaseApiResponse(
                                code = 201,
                                message = "创建成功",
                                data = userResponse
                            ))
                        }
                    }
                }
            }
        }
    }
}