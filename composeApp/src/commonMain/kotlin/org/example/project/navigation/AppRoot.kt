package org.example.project.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.example.project.log.KSLog
import org.example.project.multiplestate.ErrorScreen

@Composable
expect fun syncPlatformNavigation(navigation: IAppNavigation)

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    remember { initializeRoutes() }
    val navigation = rememberAppNavigation()
    
    // 平台特定的导航同步（如 Web 历史记录）
    syncPlatformNavigation(navigation)
    
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        navigation.Render(modifier = modifier.fillMaxSize(), null) { modifier, router, appNavigation ->
            val handler = RouterRegistry.getHandler(router.router)
            if (handler != null) {
                handler.Content(router = router, modifier = modifier)
            } else {
                ErrorScreen(
                    message = "未找到路由处理器: ${router.router}",
                    modifier = modifier
                )
            }
        }
    }
}