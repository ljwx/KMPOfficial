package org.example.project.page.product

import com.arkivanov.decompose.ComponentContext
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouterData
import org.example.project.network.model.ProductSummaryData

/**
 * 商品详情页 Component
 */
interface ProductDetailComponent : ScreenComponent {
    /**
     * 商品信息
     */
    val product: ProductSummaryData?
}

/**
 * 商品详情页 Component 实现
 */
class DefaultProductDetailComponent(
    override val config: ScreenRouterData,
    componentContext: ComponentContext
) : ProductDetailComponent, ComponentContext by componentContext {
    
    override val product: ProductSummaryData? = config.getParamsSerialize<ProductSummaryData>()
}

