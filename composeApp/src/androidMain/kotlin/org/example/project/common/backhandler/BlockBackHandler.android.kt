package org.example.project.common.backhandler

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun BlockBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack = onBack)
}

@Composable
actual fun HandleDoubleBackToExitAndroid(
    enabled: Boolean,
    firstBackClickTime: Long?,
    onFirstBackClick: (Long) -> Unit,
    onDoubleBackClick: () -> Unit,
    onResetTimeout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler(enabled = enabled) {
        val currentTime = System.currentTimeMillis()
        
        firstBackClickTime?.let { lastTime ->
            val timeDiff = currentTime - lastTime
            if (timeDiff <= 2000) {
                activity?.finishAffinity() // Android 退出逻辑
                onDoubleBackClick()
            } else {
                onFirstBackClick(currentTime)
                Toast.makeText(context, "再按一次退出App", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            onFirstBackClick(currentTime)
            Toast.makeText(context, "再按一次退出App", Toast.LENGTH_SHORT).show()
            // 2 秒后自动重置
            onResetTimeout()
        }
    }
}

