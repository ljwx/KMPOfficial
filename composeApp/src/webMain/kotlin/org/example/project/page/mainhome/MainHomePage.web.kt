package org.example.project.page.mainhome

import androidx.compose.runtime.Composable

/**
 * Web 平台：空实现（不做任何处理）
 */
@Composable
actual fun HandleDoubleBackToExitAndroid(
    enabled: Boolean,
    firstBackClickTime: Long?,
    onFirstBackClick: (Long) -> Unit,
    onDoubleBackClick: () -> Unit,
    onResetTimeout: () -> Unit
) {
    // Web 平台不做任何处理
}

