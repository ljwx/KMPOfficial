package org.example.project.network.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseApiResponse<T>(
    val code: Int,
    private val message: String?,
    val data: T? = null
) {
    fun isCodeSuccess(): Boolean {
        return code in 200..299
    }

    fun isSuccessTrue(): Boolean {
        return isCodeSuccess() && data != null
    }

    fun getMessage(): String {
        return message ?: ""
    }

}

@Serializable
data class ProductSummaryData(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String
)


