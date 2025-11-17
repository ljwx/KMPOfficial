package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext

/**
 * 路由处理器接口
 * 负责将 Component 渲染为 Compose UI
 */
interface ScreenRouteHandler {
    val route: String
    
    /**
     * 渲染页面内容
     * @param component 页面的 Component 实例（包含状态和业务逻辑）
     * @param router 路由配置信息
     * @param modifier 修饰符
     */
    @Composable
    fun Content(component: ScreenComponent, router: ScreenRouterData, modifier: Modifier = Modifier)
    
    /**
     * 创建 Component 的工厂方法
     * 子类需要实现此方法来创建对应的 Component 实例
     */
    fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent
}

