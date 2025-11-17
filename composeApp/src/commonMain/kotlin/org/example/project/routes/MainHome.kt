package org.example.project.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import org.example.project.page.home.DefaultHomeComponent
import org.example.project.page.home.HomeComponent
import org.example.project.page.home.MainHomeContainer
import org.example.project.navigation.MAIN_HOME
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouteHandler
import org.example.project.navigation.ScreenRouterData

object MainHome : ScreenRouteHandler {
    override val route: String = MAIN_HOME
    
    override fun createComponent(config: ScreenRouterData, componentContext: ComponentContext): ScreenComponent {
        return DefaultHomeComponent(componentContext, config)
    }
    
    @Composable
    override fun Content(component: ScreenComponent, router: ScreenRouterData, modifier: Modifier) {
        // 将 Component 转换为具体的类型
        val homeComponent = component as? HomeComponent
            ?: return
        
        MainHomeContainer(component = homeComponent)
    }
}

