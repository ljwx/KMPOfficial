package org.example.project.common.backhandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.navigation.LocalNavController
import org.example.project.navigation.isCurrent
import org.example.project.navigation.routes.RouterMainPage

@Composable
expect fun BlockBackHandler(enabled: Boolean, onBack: () -> Unit)

/**
 * 处理双击返回退出 App
 * 仅在 Android 平台有效
 */
@Composable
fun HandleDoubleBackToExit() {
    val mainNavController = LocalNavController.current
    val isInHomePage = mainNavController.isCurrent(RouterMainPage.route)

    var firstBackClickTime by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    HandleDoubleBackToExitAndroid(
        enabled = isInHomePage,
        firstBackClickTime = firstBackClickTime,
        onFirstBackClick = { currentTime ->
            firstBackClickTime = currentTime
        },
        onDoubleBackClick = {
            firstBackClickTime = null
        },
        onResetTimeout = {
            scope.launch {
                delay(2000)
                firstBackClickTime = null
            }
        }
    )
}

/**
 * Android 平台特定的双击返回退出实现
 */
@Composable
expect fun HandleDoubleBackToExitAndroid(
    enabled: Boolean,
    firstBackClickTime: Long?,
    onFirstBackClick: (Long) -> Unit,
    onDoubleBackClick: () -> Unit,
    onResetTimeout: () -> Unit
)