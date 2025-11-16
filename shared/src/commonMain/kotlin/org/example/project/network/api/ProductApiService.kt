package org.example.project.network.api

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.headers
import org.example.project.log.KSLog
import org.example.project.network.client.HttpClientFactory
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

class ProductApiService {

    private val client = HttpClientFactory.instance()

    suspend fun getProducts(): Result<List<ProductSummaryData>> {
        val url = BaseApiService.getBaseUrl() + "products"
        KSLog.iNet("发起网络请求:$url")
        return try {
            val response: BaseApiResponse<List<ProductSummaryData>> = client.get(url) {
                headers {
                    append("Accept", "application/json")
                }
                timeout {
                    requestTimeoutMillis = 5000
                }
            }.body()
            KSLog.iNet(response.toString())
            if (response.isCodeSuccess()) {
                if (response.isSuccessTrue()) {
                    Result.success(response.data!!)
                } else {
                    Result.failure(Exception("数据是空的"))
                }
            } else {
                Result.failure(Exception(response.getMessage()))
            }
        } catch (e: Exception) {
            KSLog.eNet("请求异常", e)
            Result.failure(e)
        }
    }

}