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
    // 时区处理策略：
    // - 数据库统一存储 UTC 时间
    // - 应用层使用 DateTimeUtils.nowUTC() 获取 UTC 时间
    // - API 层可根据用户时区进行转换（如果需要）
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
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
    
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        get("/products") {
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
            call.respond(response)
        }

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
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, BaseApiResponse<Nothing>(
                    code = 500,
                    message = "服务器内部错误: ${e.message}",
                    data = null
                ))
            }
        }
    }
}