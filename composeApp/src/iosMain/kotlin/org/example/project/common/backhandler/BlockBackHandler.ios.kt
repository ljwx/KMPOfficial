package org.example.project.common.backhandler

import androidx.compose.runtime.Composable

@Composable
actual fun BlockBackHandler(enabled: Boolean, onBack: () -> Unit) {
    
}

/**
 * iOS 平台：空实现（不做任何处理）
 */
@Composable
actual fun HandleDoubleBackToExitAndroid(
    enabled: Boolean,
    firstBackClickTime: Long?,
    onFirstBackClick: (Long) -> Unit,
    onDoubleBackClick: () -> Unit,
    onResetTimeout: () -> Unit
) {
    // iOS 平台不做任何处理
}

