package org.example.project.feature.product

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.headers
import org.example.project.log.KSLog
import org.example.project.network.api.BaseApiService
import org.example.project.network.client.HttpClientFactory
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

class ProductRepository : IProductRepository {

    private val client = HttpClientFactory.instance()

    override suspend fun getProductList(): Result<BaseApiResponse<List<ProductSummaryData>>> {
        val url = BaseApiService.getBaseUrl() + "v1/products"
        KSLog.iNet("Repository: 发起网络请求: $url")
        return try {
            KSLog.iNet("Repository: 开始执行网络请求...")
            val response: BaseApiResponse<List<ProductSummaryData>> = client.get(url) {
                headers {
                    append("Accept", "application/json")
                }
                timeout {
                    requestTimeoutMillis = 5000
                }
            }.body()
            KSLog.iNet("Repository: 网络请求完成，code=${response.code}, data size=${response.data?.size ?: 0}")
            Result.success(response)
        } catch (e: Exception) {
            KSLog.eNet("Repository: 请求异常", e)
            Result.failure(e)
        }
    }
}