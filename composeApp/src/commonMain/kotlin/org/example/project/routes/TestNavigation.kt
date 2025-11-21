package org.example.project.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.json.Json
import org.example.project.network.model.ProductSummaryData
import org.example.project.page.PullRefreshExamplePage
import org.example.project.page.product.DetailScreen

fun NavGraphBuilder.testGraph() {
    composable<RouterPullRefresh> {
        PullRefreshExamplePage()
    }

    composable<RouterProductDetail> { backStackEntry ->
        // 使用 toRoute() 获取类型安全的路由参数
        val route = backStackEntry.toRoute<RouterProductDetail>()
        // 反序列化 JSON 字符串为 ProductSummaryData 对象
        val product = Json.decodeFromString<ProductSummaryData>(route.productJson)
        
        DetailScreen(product = product)
    }
}