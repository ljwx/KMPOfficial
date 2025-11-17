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

