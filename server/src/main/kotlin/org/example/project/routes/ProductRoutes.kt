package org.example.project.routes

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

/**
 * 产品相关路由配置
 */
fun Route.productRoutes() {
    route("/products") {
        rateLimit(RateLimitName("api")) {
            // GET /v1/products: 获取产品列表
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
}

