package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import org.example.project.navigation.DefaultScreenComponent
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.navigation.WEBVIEW_TEST
import org.example.project.page.WebViewExample

object TestWebView : ScreenRouteHandler {
    override val route: String = WEBVIEW_TEST

    override fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent {
        // WebView 页面不需要特殊的状态管理，使用默认 Component
        return DefaultScreenComponent(config, componentContext)
    }

    @Composable
    override fun Content(component: ScreenComponent, router: ScreenRouterData, modifier: Modifier) {
        WebViewExample()
    }
}