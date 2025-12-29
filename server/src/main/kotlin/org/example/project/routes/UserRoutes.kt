package org.example.project.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.db.user.UserService
import org.example.project.db.user.dto.CreateUserRequest
import org.example.project.network.model.BaseApiResponse

/**
 * 用户相关路由配置
 */
fun Route.userRoutes(userService: UserService) {
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

