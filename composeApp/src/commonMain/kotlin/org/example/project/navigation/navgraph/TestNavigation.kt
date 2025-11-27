package org.example.project.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.json.Json
import org.example.project.navigation.routes.RouterProductDetail
import org.example.project.navigation.routes.RouterPullRefresh
import org.example.project.network.model.ProductSummaryData
import org.example.project.page.PullRefreshExamplePage
import org.example.project.page.product.DetailScreen

fun NavGraphBuilder.testGraph() {
    composable<RouterPullRefresh> {
        PullRefreshExamplePage()
    }

    composable<RouterProductDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RouterProductDetail>()
        val product = Json.decodeFromString<ProductSummaryData>(route.productJson)
        DetailScreen(product = product)
    }
}