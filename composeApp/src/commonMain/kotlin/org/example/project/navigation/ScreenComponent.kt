package org.example.project.navigation

import com.arkivanov.decompose.ComponentContext

/**
 * 屏幕 Component 基接口
 * 所有页面的 Component 都应该实现这个接口
 * Component 的生命周期独立于 Compose 组合，状态不会因为组合被移除而丢失
 */
interface ScreenComponent {
    /**
     * Component 的配置信息
     */
    val config: ScreenRouterData
}

/**
 * 默认的 Component 实现
 * 当找不到对应的 RouteHandler 时使用
 */
class DefaultScreenComponent(
    override val config: ScreenRouterData,
    componentContext: ComponentContext
) : ScreenComponent, ComponentContext by componentContext

/**
 * Component 工厂接口
 * 用于根据路由配置创建对应的 Component 实例
 */
interface ComponentFactory {
    /**
     * 创建 Component 实例
     * @param config 路由配置
     * @param componentContext ComponentContext，提供生命周期和状态保存能力
     * @return Component 实例
     */
    fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent
}

