package org.example.project.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.example.project.page.PullRefreshExamplePage

fun NavGraphBuilder.testGraph() {
    composable<RouterPullRefresh> {
        PullRefreshExamplePage()
    }
}