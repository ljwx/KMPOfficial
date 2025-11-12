package org.example.project.navigation

import androidx.compose.runtime.Composable

/**
 * Android 平台不需要额外的导航同步
 */
@Composable
actual fun syncPlatformNavigation(navigation: IAppNavigation) {
    // Android 平台不需要同步浏览器历史记录
}

