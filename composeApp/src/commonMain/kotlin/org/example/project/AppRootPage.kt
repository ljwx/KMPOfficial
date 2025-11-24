package org.example.project

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.example.project.navigation.LocalNavController
import org.example.project.routes.RouterSplash
import org.example.project.theme.AppTheme
import org.example.project.routes.mainHomeGraph
import org.example.project.routes.splashGraph
import org.example.project.routes.testGraph
import org.example.project.statusbar.StatusBarConfig
import org.example.project.statusbar.StatusBarStyle

@Composable
fun AppRootPage(
    modifier: Modifier = Modifier
) {
    AppTheme {
        StatusBarConfig(StatusBarStyle.DARK_CONTENT)

        // 平台特定的导航同步（如 Web 历史记录）
        // syncPlatformNavigation(navigation)
        val navController = rememberNavController()
        CompositionLocalProvider(LocalNavController provides navController) {
            NavHost(
                navController = navController,
                startDestination = RouterSplash,
                // 全局 Android 风格动画
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) {
                splashGraph()
                mainHomeGraph()
                testGraph()
            }
        }
    }
}