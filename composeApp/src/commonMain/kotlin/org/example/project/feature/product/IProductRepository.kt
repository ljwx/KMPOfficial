package org.example.project.feature.product

import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

interface IProductRepository {

    suspend fun getProductList(): Result<BaseApiResponse<List<ProductSummaryData>>>

}