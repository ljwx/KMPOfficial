package org.example.project.pullrefresh

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 下拉刷新修饰符
 */
@Composable
fun Modifier.pullRefresh(state: PullRefreshState): Modifier {
    return nestedScroll(state.nestedScrollConnection)
}

/**
 * 下拉刷新指示器
 */
@Composable
fun PullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    scale: Boolean = true,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        val scaleValue = if (scale && !refreshing && state.progress < 1f) {
            0.5f + (state.progress * 0.5f)
        } else {
            1f
        }
        
        val alphaValue = if (state.progress > 0f || refreshing) 1f else 0f

        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scaleValue)
                    .alpha(alphaValue),
                color = contentColor,
                strokeWidth = 3.dp
            )
        } else if (state.progress > 0f) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scaleValue)
                    .alpha(alphaValue),
                progress = { state.progress },
                color = contentColor,
                strokeWidth = 3.dp
            )
        }
    }
}

/**
 * 下拉刷新容器
 */
@Composable
fun PullRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enableContentOffset: Boolean = true,
    indicator: @Composable (PullRefreshState) -> Unit = { state ->
        PullRefreshIndicator(refreshing = refreshing, state = state)
    },
    content: @Composable () -> Unit
) {
    val state = rememberPullRefreshState(refreshing, onRefresh)
    val density = LocalDensity.current
    
    // Indicator 偏移：从标题栏下（-56dp）拉出来
    // 直接读取 state.dragDistance 以触发重组
    val indicatorTargetOffset = with(density) { 
        state.dragDistance.toDp()
    }
    
    // 计算动画时长：根据下拉距离动态调整
    val animationDuration = if (state.isRefreshing && indicatorTargetOffset > 0.dp) {
        val currentDistance = indicatorTargetOffset
        val targetDistance = with(density) { state.getRefreshThresholdPixels().toDp() }
        val distanceToTravel = (currentDistance - targetDistance).value
        if (distanceToTravel > 0) {
            // 基础时长 180ms，每超过阈值 1dp 增加 1ms，最大不超过 300ms
            val baseDuration = 180
            val extraDuration = (distanceToTravel * 1f).toInt().coerceAtMost(120)
            baseDuration + extraDuration
        } else {
            180
        }
    } else {
        0
    }
    
    val indicatorOffset by animateDpAsState(
        targetValue = indicatorTargetOffset,
        animationSpec = tween(
            durationMillis = when {
                !state.isRefreshing && indicatorTargetOffset == 0.dp && state.progress == 0f -> 400
                state.isRefreshing && indicatorTargetOffset > 0.dp -> animationDuration
                else -> 0
            },
            easing = if (state.isRefreshing && indicatorTargetOffset > 0.dp) {
                // 回弹到刷新位置时使用更明显的弹簧效果
                androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0.2f, 0.8f)
            } else {
                androidx.compose.animation.core.FastOutSlowInEasing
            }
        ),
        label = "indicatorOffset"
    )
    
    // 内容偏移：与 indicator 同步移动
    val contentTargetOffset = if (enableContentOffset && (state.progress > 0f || state.isRefreshing)) {
        with(density) { state.dragDistance.toDp() }
    } else {
        0.dp
    }
    
    val contentOffset by animateDpAsState(
        targetValue = contentTargetOffset,
        animationSpec = tween(
            durationMillis = when {
                // 刷新结束时（归零）：400ms 平滑上移
                contentTargetOffset == 0.dp && state.progress == 0f -> 400
                // 触发刷新时：使用动态计算的动画时长，与 indicatorOffset 同步
                state.isRefreshing && contentTargetOffset > 0.dp -> animationDuration
                // 其他情况立即跟随
                else -> 0
            },
            easing = if (state.isRefreshing && indicatorTargetOffset > 0.dp) {
                // 回弹到刷新位置时使用更明显的弹簧效果
                androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0.2f, 0.8f)
            } else {
                androidx.compose.animation.core.FastOutSlowInEasing
            }
        ),
        label = "contentOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(state)
    ) {
        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = contentOffset)
        ) {
            DisableOverscroll {
                content()
            }
        }
        
        // Indicator 浮在上层，初始位置 -56dp（被标题栏遮住）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = indicatorOffset - 56.dp)
        ) {
            indicator(state)
        }
    }
}
