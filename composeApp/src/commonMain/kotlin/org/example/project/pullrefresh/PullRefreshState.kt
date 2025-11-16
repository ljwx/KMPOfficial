package org.example.project.pullrefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * 下拉刷新状态，兼容 Android Material 3 的 PullRefreshState API
 * 
 * 用于跟踪和控制下拉刷新的状态，包括：
 * - 是否正在刷新
 * - 下拉进度
 * - 是否已触发刷新
 */
@Stable
class PullRefreshState internal constructor(
    initialRefreshing: Boolean = false,
    private val onRefresh: () -> Unit,
    private val refreshThreshold: Float,
    private val maxDragDistance: Float
) {
    /**
     * 是否正在刷新
     */
    var isRefreshing: Boolean by mutableStateOf(initialRefreshing)
        internal set

    /**
     * 下拉进度，范围 [0f, 1f]
     * 0f 表示未下拉，1f 表示达到刷新阈值
     */
    var progress: Float by mutableFloatStateOf(0f)
        private set

    /**
     * 是否应该触发刷新（下拉超过阈值）
     */
    var shouldRefresh: Boolean by mutableStateOf(false)
        private set

    /**
     * 当前下拉距离（像素）
     */
    internal var dragDistance: Float by mutableFloatStateOf(0f)
        private set

    /**
     * 刷新阈值（像素），超过此距离将触发刷新
     */
    private val refreshThresholdPixels = refreshThreshold

    /**
     * 最大下拉距离（像素）
     */
    private val maxDragDistancePixels = maxDragDistance
    
    /**
     * 是否正在拖拽
     */
    private var isDragging: Boolean by mutableStateOf(false)

    /**
     * 嵌套滚动连接，用于处理与滚动组件的交互
     */
    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // 如果正在刷新或正在拖拽，不拦截
            if (isRefreshing || isDragging) {
                return Offset.Zero
            }
            
            // 如果内容可以向上滚动（available.y < 0），则不拦截下拉手势
            if (available.y < 0) {
                return Offset.Zero
            }

            // 拦截下拉手势（available.y > 0）
            val drag = minOf(available.y, maxDragDistancePixels - dragDistance)
            if (drag > 0) {
                updateDrag(dragDistance + drag)
                return Offset(0f, drag)
            }
            
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // 如果正在刷新或正在拖拽，不拦截
            if (isRefreshing || isDragging) {
                return Offset.Zero
            }

            // 如果内容可以向下滚动（available.y > 0），则不拦截下拉手势
            if (available.y > 0) {
                return Offset.Zero
            }

            // 拦截下拉手势（available.y < 0，即向上滚动）
            val drag = minOf(-available.y, maxDragDistancePixels - dragDistance)
            if (drag > 0) {
                updateDrag(dragDistance + drag)
                return Offset(0f, -drag)
            }

            return Offset.Zero
        }
    }

    /**
     * 更新拖拽距离
     */
    internal fun updateDrag(distance: Float) {
        dragDistance = max(0f, min(distance, maxDragDistancePixels))
        progress = (dragDistance / refreshThresholdPixels).coerceIn(0f, 1f)
        shouldRefresh = dragDistance >= refreshThresholdPixels
    }

    /**
     * 开始拖拽
     */
    internal fun startDrag() {
        isDragging = true
    }

    /**
     * 结束拖拽
     * 
     * @param scope 协程作用域，用于执行刷新回调
     */
    internal fun endDrag(scope: CoroutineScope) {
        isDragging = false
        if (shouldRefresh && !isRefreshing) {
            isRefreshing = true
            scope.launch {
                onRefresh()
            }
        } else {
            // 重置拖拽距离
            dragDistance = 0f
            progress = 0f
            shouldRefresh = false
        }
    }

    /**
     * 完成刷新
     */
    internal fun finishRefresh() {
        isRefreshing = false
        dragDistance = 0f
        progress = 0f
        shouldRefresh = false
    }

    /**
     * 获取当前指示器的偏移量（像素）
     */
    internal fun getIndicatorOffset(): Float {
        return if (isRefreshing) {
            refreshThresholdPixels
        } else {
            dragDistance
        }
    }
}

private val RefreshThreshold = 80.dp
private val MaxDragDistance = 120.dp

/**
 * 创建并记住 PullRefreshState
 * 
 * @param refreshing 是否正在刷新
 * @param onRefresh 刷新回调函数
 * @param refreshThresholdDp 刷新阈值，下拉超过此距离将触发刷新
 * @param maxDragDistanceDp 最大下拉距离
 * @return PullRefreshState 实例
 */
@Composable
fun rememberPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThresholdDp: Dp = RefreshThreshold,
    maxDragDistanceDp: Dp = MaxDragDistance,
): PullRefreshState {
    val density = LocalDensity.current
    val refreshThreshold = remember(density, refreshThresholdDp) {
        with(density) { refreshThresholdDp.toPx() }
    }
    val maxDragDistance = remember(density, maxDragDistanceDp) {
        with(density) { maxDragDistanceDp.toPx() }
    }
    
    val state = remember(onRefresh) {
        PullRefreshState(
            initialRefreshing = refreshing,
            onRefresh = onRefresh,
            refreshThreshold = refreshThreshold,
            maxDragDistance = maxDragDistance
        )
    }

    // 同步外部 refreshing 状态
    LaunchedEffect(refreshing) {
        if (state.isRefreshing != refreshing) {
            if (refreshing) {
                state.isRefreshing = true
            } else {
                state.finishRefresh()
            }
        }
    }

    return state
}

