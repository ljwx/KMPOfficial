package org.example.project.page.mainhome

import androidx.compose.runtime.Composable

@Composable
actual fun HandleDoubleBackToExitAndroid(
    enabled: Boolean,
    firstBackClickTime: Long?,
    onFirstBackClick: (Long) -> Unit,
    onDoubleBackClick: () -> Unit,
    onResetTimeout: () -> Unit
) {
    // JVM 平台不做任何处理
}

