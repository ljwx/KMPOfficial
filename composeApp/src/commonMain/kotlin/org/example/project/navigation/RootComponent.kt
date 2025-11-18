package org.example.project.navigation

import com.arkivanov.decompose.ComponentContext
import org.example.project.log.KSLog

/**
 * RootComponent - 应用根组件
 * 
 * 职责：
 * 1. 管理 AppNavigation 的生命周期
 * 2. 通过 ComponentContext 提供状态保存和恢复能力
 * 3. 确保导航栈在配置变更、进程重启等场景下能够正确恢复
 * 
 * 生命周期管理：
 * - RootComponent 的生命周期绑定到平台的原生生命周期（Activity/ViewController/Window）
 * - 当平台生命周期变化时（如屏幕旋转、进程重启），StateKeeper 会自动保存和恢复状态
 * - AppNavigation 通过 RootComponent 的 ComponentContext 获得状态保存能力
 * 
 * 状态保存机制：
 * - Decompose 的 childStack 使用 serializer 自动序列化导航栈配置
 * - ComponentContext 的 StateKeeper 可以保存额外的组件状态
 * - 状态会在平台特定的存储中持久化（Android Bundle、iOS UserDefaults 等）
 * 
 * @param componentContext 组件上下文，提供生命周期和状态保存能力
 */
class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    /**
     * AppNavigation 实例
     * 
     * 通过 RootComponent 的 ComponentContext 创建，确保：
     * 1. 导航栈状态可以通过 StateKeeper 保存和恢复
     * 2. 生命周期与平台原生生命周期同步
     * 3. 在配置变更时不会丢失导航状态
     */
    val navigation: AppNavigation = AppNavigation(componentContext)

    init {
        KSLog.iRouter("RootComponent 初始化完成")
    }
}

