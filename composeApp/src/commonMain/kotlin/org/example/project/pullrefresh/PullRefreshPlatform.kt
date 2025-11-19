package org.example.project.pullrefresh

import androidx.compose.runtime.Composable

/**
 * 禁用内容的 overscroll 效果（仅 Android）
 */
@Composable
expect fun DisableOverscroll(content: @Composable () -> Unit)

