package org.example.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.example.project.AppRootPage


@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    ComposeViewport(viewportContainerId = "composeApp") {
        AppRootPage()
    }
}