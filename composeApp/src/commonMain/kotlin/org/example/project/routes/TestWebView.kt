package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.navigation.WEBVIEW_TEST
import org.example.project.page.WebViewExample

object TestWebView : ScreenRouteHandler {
    override val route: String = WEBVIEW_TEST

    @Composable
    override fun Content(router: ScreenRouterData, modifier: Modifier) {
        WebViewExample()
    }
}