package org.example.project.statusbar

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Android 平台的状态栏配置实现
 */
@Composable
actual fun StatusBarConfig(style: StatusBarStyle) {
    val view = LocalView.current
    
    SideEffect {
        val activity = view.context as? android.app.Activity ?: return@SideEffect
        val window = activity.window
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowCompat.getInsetsController(window, view)
        // isAppearanceLightStatusBars = true 表示状态栏显示深色图标（适合浅色背景）
        // isAppearanceLightStatusBars = false 表示状态栏显示浅色图标（适合深色背景）
        controller.isAppearanceLightStatusBars = when (style) {
            StatusBarStyle.DARK_CONTENT -> true  // 深色图标，适合浅色背景
            StatusBarStyle.LIGHT_CONTENT -> false // 浅色图标，适合深色背景
        }
        
        // Android 6.0+ 支持设置状态栏颜色为透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}

