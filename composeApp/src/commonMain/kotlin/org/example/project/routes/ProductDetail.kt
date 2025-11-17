package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import org.example.project.navigation.PRODUCT_DETAIL
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.page.product.DefaultProductDetailComponent
import org.example.project.page.product.DetailScreen
import org.example.project.page.product.ProductDetailComponent

object ProductDetail : ScreenRouteHandler {
    override val route: String = PRODUCT_DETAIL

    override fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent {
        return DefaultProductDetailComponent(config, componentContext)
    }

    @Composable
    override fun Content(component: ScreenComponent, router: ScreenRouterData, modifier: Modifier) {
        // 将 Component 转换为具体的类型
        val detailComponent = component as? ProductDetailComponent
            ?: return
        
        DetailScreen(
            component = detailComponent,
            modifier = modifier
        )
    }
}

