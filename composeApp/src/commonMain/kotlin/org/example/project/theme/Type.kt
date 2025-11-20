package org.example.project.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/**
 * 获取应用的排版样式
 * 使用 Material3 默认排版，依赖 CSS 字体栈处理中文显示
 */
@Composable
fun getAppTypography(): Typography {
    return Typography()
}
