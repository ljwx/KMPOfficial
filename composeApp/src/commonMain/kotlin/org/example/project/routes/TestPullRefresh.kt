package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import org.example.project.navigation.DefaultScreenComponent
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData
import org.example.project.navigation.PULL_REFRESH_TEST
import org.example.project.page.PullRefreshExamplePage

object TestPullRefresh : ScreenRouteHandler {
    override val route: String = PULL_REFRESH_TEST

    override fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent {
        return DefaultScreenComponent(config, componentContext)
    }

    @Composable
    override fun Content(component: ScreenComponent, router: ScreenRouterData, modifier: Modifier) {
        PullRefreshExamplePage()
    }
}

