package org.example.project.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.example.project.navigation.routes.RouterMainPage
import org.example.project.page.mainhome.MainHomePage

fun NavGraphBuilder.mainHomeGraph() {
//    navigation(startDestination = RouterMainTabHome.route, route = RouterMainPage.route) {
//        composable(route = RouterMainTabHome.route) { backStackEntry ->
//            MainTabHomeScreen()
//        }
//        composable(route = RouterMainTabCreate.route) { backStackEntry ->
//            MainHomePage()
//        }
//        composable(route = RouterMainTabMine.route) { backStackEntry ->
//            MainHomePage()
//        }
//    }
    composable<RouterMainPage> {
        MainHomePage()
    }

}