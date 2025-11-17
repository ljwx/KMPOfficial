package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.navigation.APP_SPLASH
import org.example.project.navigation.LaunchMode
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.page.splash.AppSplashPage

object AppSplash : ScreenRouteHandler {
    override val route: String = APP_SPLASH
    
    @Composable
    override fun Content(router: ScreenRouterData, modifier: Modifier) {
        AppSplashPage()
    }
}