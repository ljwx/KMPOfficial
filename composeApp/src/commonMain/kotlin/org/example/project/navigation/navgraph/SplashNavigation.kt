package org.example.project.navigation.navgraph

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.example.project.navigation.routes.RouterSplash
import org.example.project.page.splash.AppSplashPage

fun NavGraphBuilder.splashGraph() {
    composable<RouterSplash>(
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }) {
        AppSplashPage()
    }
}