package org.example.project.pullrefresh

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 下拉刷新修饰符，兼容 Android Material 3 的 Modifier.pullRefresh API
 * 
 * 将此修饰符应用到可滚动内容上，以启用下拉刷新功能。
 * 
 * @param state PullRefreshState 实例
 * @return 应用了下拉刷新功能的 Modifier
 */
@Composable
fun Modifier.pullRefresh(state: PullRefreshState): Modifier {
    return nestedScroll(state.nestedScrollConnection)
}

/**
 * 下拉刷新指示器，兼容 Android Material 3 的 PullRefreshIndicator API
 * 
 * 显示下拉刷新的进度指示器，通常放置在内容的顶部。
 * 
 * @param refreshing 是否正在刷新
 * @param state PullRefreshState 实例
 * @param modifier 应用于指示器的 Modifier
 * @param scale 指示器的缩放比例，默认根据进度自动调整
 * @param backgroundColor 指示器的背景颜色，默认使用 MaterialTheme 的背景色
 * @param contentColor 指示器的内容颜色，默认使用 MaterialTheme 的内容色
 */
@Composable
fun PullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    scale: Boolean = true,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val indicatorOffset = with(density) {
        state.getIndicatorOffset().toDp()
    }
    
    val scaleValue = if (scale && !refreshing) {
        state.progress.coerceIn(0f, 1f)
    } else {
        1f
    }

    val alpha = if (refreshing || state.progress > 0f) {
        1f
    } else {
        0f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        label = "PullRefreshIndicatorAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (animatedAlpha > 0f) {
            CircularProgressIndicator(
                modifier = Modifier
                    .scale(scaleValue)
                    .offset(y = indicatorOffset),
                progress = if (refreshing) {
                    1f
                } else {
                    state.progress
                },
                color = contentColor.copy(alpha = animatedAlpha),
                strokeWidth = 3.dp
            )
        }
    }
}

/**
 * 下拉刷新容器，提供完整的下拉刷新功能
 * 
 * 这是一个便捷的 Composable，结合了 pullRefresh 修饰符和 PullRefreshIndicator。
 * 
 * @param refreshing 是否正在刷新
 * @param onRefresh 刷新回调函数
 * @param modifier 应用于容器的 Modifier
 * @param indicator 自定义的刷新指示器，默认使用 PullRefreshIndicator
 * @param content 内容 Composable
 */
@Composable
fun PullRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicator: @Composable BoxScope.(PullRefreshState) -> Unit = { state ->
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state
        )
    },
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullRefreshState(refreshing, onRefresh)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(state)
    ) {
        content()
        indicator(state)
    }
}

