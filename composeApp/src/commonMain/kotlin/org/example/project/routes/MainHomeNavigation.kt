package org.example.project.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.example.project.page.home.MainHomeContainer

/**
 * 主页路由图
 * 
 * 注意：不再需要 navController 参数，因为子组件通过 LocalNavController.current 访问
 */
fun NavGraphBuilder.mainHomeGraph() {
    composable<RouterMainHome> { backStackEntry ->
        MainHomeContainer()
    }
}