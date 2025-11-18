package org.example.project.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.example.project.log.KSLog
import org.example.project.multiplestate.ErrorScreen
import org.example.project.statusbar.StatusBarConfig
import org.example.project.statusbar.StatusBarStyle

/**
 * 平台特定的导航同步函数
 * 用于在 Web 平台同步浏览器历史记录等
 */
@Composable
expect fun syncPlatformNavigation(navigation: IAppNavigation)

/**
 * AppRoot - 应用根组件
 * 
 * 这是应用的入口点，负责：
 * 1. 初始化路由注册表
 * 2. 使用 RootComponent 管理导航生命周期
 * 3. 渲染导航栈中的页面
 * 
 * 使用方式：
 * - 在生产环境中，应该传入从平台生命周期管理器创建的 RootComponent
 * - 在预览/测试环境中，可以使用默认的 RootComponent
 * 
 * @param rootComponent RootComponent 实例，如果为 null 则使用默认实现（仅用于预览）
 * @param modifier 修饰符
 */
@Composable
fun AppRoot(
    rootComponent: RootComponent? = null,
    modifier: Modifier = Modifier
) {
    // 初始化路由注册表
    remember { initializeRoutes() }
    
    // 使用传入的 RootComponent 或创建默认的（仅用于预览）
    val component = rootComponent ?: remember {
        KSLog.wRouter("AppRoot: 使用默认 RootComponent（仅用于预览）")
        DefaultPlatformLifecycleOwner().createRootComponent()
    }
    
    val navigation = component.navigation
    
    StatusBarConfig(StatusBarStyle.LIGHT_CONTENT)
    
    // 平台特定的导航同步（如 Web 历史记录）
    syncPlatformNavigation(navigation)
    
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        navigation.Render(modifier = modifier.fillMaxSize(), null) { modifier, router, appNavigation ->
            // 从 childStack 中获取当前活动的 Component 实例
            val childStack = navigation.getChildStack()
            val screenComponent = childStack.value.active.instance
            
            val handler = RouterRegistry.getHandler(router.router)
            if (handler != null) {
                handler.Content(component = screenComponent, router = router, modifier = modifier)
            } else {
                ErrorScreen(
                    message = "未找到路由处理器: ${router.router}",
                    modifier = modifier
                )
            }
        }
    }
}