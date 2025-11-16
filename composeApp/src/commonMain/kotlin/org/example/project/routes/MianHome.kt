package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.page.home.MainHomeContainer
import org.example.project.navigation.ROUTER_HOME
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData

object MianHome : ScreenRouteHandler {
    override val route: String = ROUTER_HOME
    
    @Composable
    override fun Content(router: ScreenRouterData, modifier: Modifier) {
        MainHomeContainer()
    }
}

