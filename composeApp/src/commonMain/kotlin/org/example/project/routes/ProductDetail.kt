package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.navigation.PRODUCT_DETAIL
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.product.DetailScreen

object ProductDetail : ScreenRouteHandler {
    override val route: String = PRODUCT_DETAIL

    @Composable
    override fun Content(router: ScreenRouterData, modifier: Modifier) {
        // DetailScreen 内部会自己获取参数，RouteHandler 只负责路由分发
        DetailScreen(modifier = modifier)
    }
}

