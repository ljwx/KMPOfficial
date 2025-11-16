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
import org.example.project.db.DatabaseFactory
import org.example.project.db.User
import org.example.project.db.UserService
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 关键步骤 1: 初始化数据库连接
    // 在应用启动时，调用 DatabaseFactory.init() 来设置数据库连接和创建表。
    DatabaseFactory.init()
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
            val response = BaseApiResponse(code = 200, message = "成功", data = users)
            call.respond(response)
        }

        // POST /users: 创建一个新用户
        post("/users") {
            // Ktor 会自动从请求的 JSON body 中反序列化出 User 对象
            val user = call.receive<User>()
            val createdUser = userService.createUser(user)
            val response = BaseApiResponse(code = 201, message = "创建成功", data = createdUser)
            call.respond(response)
        }
    }
}