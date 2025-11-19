package org.example.project.pullrefresh

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun DisableOverscroll(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        content()
    }
}

