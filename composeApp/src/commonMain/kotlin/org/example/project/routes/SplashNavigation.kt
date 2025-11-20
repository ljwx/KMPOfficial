package org.example.project.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.example.project.page.splash.AppSplashPage

fun NavGraphBuilder.splashGraph() {
    composable<RouterSplash> {
        AppSplashPage()
    }
}