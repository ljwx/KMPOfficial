package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ScreenRouteHandler {
    val route: String
    
    @Composable
    fun Content(router: ScreenRouterData, modifier: Modifier = Modifier)
}

