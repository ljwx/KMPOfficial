package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.http.HttpStatusCode
import org.example.project.db.DatabaseFactory
import org.example.project.db.UserService
import org.example.project.db.dto.CreateUserRequest
import org.example.project.db.dto.UserResponse
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 关键步骤 1: 初始化数据库连接
    // 在应用启动时，调用 DatabaseFactory.init() 来设置数据库连接和创建表。
    // 注意：如果旧表结构不兼容，可以传入 dropExistingTable = true 来删除旧表重建
    // 生产环境请设置为 false，并手动迁移数据
    DatabaseFactory.init(dropExistingTable = true)  // 开发环境：删除旧表重建
    // 关键步骤 2: 实例化服务
    // 创建 UserService 的实例，用于后续 API 调用中的数据库操作。
    val userService = UserService()

    // 配置 ContentNegotiation 和 JSON 序列化
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
    
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        get("/products") {
            // 示例：返回产品列表的标准响应格式
            val products: List<ProductSummaryData> = listOf(
                ProductSummaryData(1, "商品A", 99.99, "这是商品A的描述"),
                ProductSummaryData(2, "商品B", 199.99, "这是商品B的描述"),
                ProductSummaryData(3, "商品C", 299.99, "这是商品C的描述"),
                ProductSummaryData(4, "商品D", 399.99, "这是商品D的描述"),
            )
            
            // 使用 BaseApiResponse 包装响应，显式指定类型
            val response: BaseApiResponse<List<ProductSummaryData>> = BaseApiResponse(
                code = 200,
                message = "成功",
                data = products
            )
            call.respond(response)
        }

        // 关键步骤 3: 定义 API 路由
        // 为用户数据创建 HTTP 端点。
        // GET /users: 获取所有用户
        get("/users") {
            val users = userService.getAllUsers()
            val userResponses = users.map { userService.toUserResponse(it) }
            val response = BaseApiResponse(code = 200, message = "成功", data = userResponses)
            call.respond(response)
        }

        // POST /users: 创建一个新用户
        post("/users") {
            try {
                // 接收创建用户请求
                val request = call.receive<CreateUserRequest>()
                
                // 数据校验
                val validationError = request.validate()
                if (validationError != null) {
                    val response = BaseApiResponse<Nothing>(
                        code = 400,
                        message = validationError,
                        data = null
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }
                
                // 检查用户名是否已存在
                val existingUser = userService.getUserByUsername(request.username)
                if (existingUser != null) {
                    val response = BaseApiResponse<Nothing>(
                        code = 409,
                        message = "用户名已存在",
                        data = null
                    )
                    call.respond(HttpStatusCode.Conflict, response)
                    return@post
                }
                
                // 检查邮箱是否已存在（如果提供了邮箱）
                request.email?.let { email ->
                    val existingEmailUser = userService.getUserByEmail(email)
                    if (existingEmailUser != null) {
                        val response = BaseApiResponse<Nothing>(
                            code = 409,
                            message = "邮箱已被注册",
                            data = null
                        )
                        call.respond(HttpStatusCode.Conflict, response)
                        return@post
                    }
                }
                
                // TODO: 在实际项目中，应该使用密码加密库（如 BCrypt）对密码进行哈希
                // 这里为了演示，暂时直接使用原始密码（生产环境必须加密！）
                val passwordHash = request.password // 应该改为：BCrypt.hashpw(request.password, BCrypt.gensalt())
                
                // 创建用户
                val createdUser = userService.createUser(request, passwordHash)
                val userResponse = userService.toUserResponse(createdUser)
                
                val response = BaseApiResponse(
                    code = 201,
                    message = "创建成功",
                    data = userResponse
                )
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                val response = BaseApiResponse<Nothing>(
                    code = 500,
                    message = "服务器内部错误: ${e.message}",
                    data = null
                )
                call.respond(HttpStatusCode.InternalServerError, response)
            }
        }
    }
}