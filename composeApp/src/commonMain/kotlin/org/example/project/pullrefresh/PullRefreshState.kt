package org.example.project.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * 下拉刷新状态管理
 */
@Stable
class PullRefreshState internal constructor(
    initialRefreshing: Boolean = false,
    private val onRefresh: () -> Unit,
    private val refreshThreshold: Float,
    private val maxDragDistance: Float
) {
    var isRefreshing: Boolean by mutableStateOf(initialRefreshing)
        internal set

    var progress: Float by mutableFloatStateOf(0f)
        internal set

    internal var dragDistance: Float by mutableFloatStateOf(0f)
        private set

    private val refreshThresholdPixels = refreshThreshold
    private val maxDragDistancePixels = maxDragDistance
    private val animatable = Animatable(0f)
    private var isReleasing = false // 标记是否正在释放，避免重复处理
    
    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isReleasing) return Offset.Zero
            
            // 向上滚动时处理复位逻辑
            if (available.y < 0 && dragDistance > 0) {
                // 如果正在刷新，不允许复位，只消耗下拉距离
                if (isRefreshing) {
                    val newDragDistance = (dragDistance + available.y).coerceAtLeast(0f)
                    val consumed = dragDistance - newDragDistance
                    dragDistance = newDragDistance
                    updateProgress()
                    return Offset(0f, consumed)
                } else {
                    // 如果未在刷新，允许复位：清空下拉距离，让 indicator 回到初始位置
                    val consumed = dragDistance
                    dragDistance = 0f
                    updateProgress()
                    return Offset(0f, consumed)
                }
            }
            
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // 如果正在释放或刷新，立即阻止所有滚动事件，避免松手后继续响应
            if (isRefreshing || isReleasing) {
                // 消耗所有可用的向下滚动距离，阻止内容继续下滑
                if (available.y > 0) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            // 关键判断：只有当 consumed.y == 0 时，说明列表已经在顶部（列表头已经在内容区域内）
            // 此时 available.y > 0 表示向下拖动，可以处理下拉刷新
            // 如果 consumed.y > 0，说明列表还在向上滚动，不在顶部，不应该处理下拉刷新
            if (available.y > 0 && consumed.y == 0f) {
                // 列表在顶部，处理下拉刷新
                val dragConsumed = onPull(available.y)
                return Offset(0f, dragConsumed)
            }
            
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (dragDistance > 0 && !isReleasing && !isRefreshing) {
                isReleasing = true
                // 不要在这里提前设置 isRefreshing，让 onRelease() 来处理
                onRelease()
                isReleasing = false
            }
            // 消耗所有速度，阻止惯性滚动
            return Velocity.Zero
        }
    }

    /**
     * 处理下拉手势，应用递增阻尼
     * 
     * 返回值：消耗的滚动距离，用于阻止内容继续滚动
     * 内容移动通过 contentOffset（等于 dragDistance）实现
     */
    private fun onPull(pullDelta: Float): Float {
        if (isRefreshing) return 0f
        
        // 如果已经达到最大下拉距离，完全消耗滚动距离，阻止内容继续滚动
        if (dragDistance >= maxDragDistancePixels) {
            return pullDelta
        }
        
        // 递增阻尼：越拉越难
        // 调小阻尼（让下拉更容易）：基础系数从 0.5 增加到 0.7，指数从 1.8 降到 1.5
        // 基础系数越大，初始阻尼越小（下拉越容易）；指数越小，阻尼变化越平缓
        val currentProgress = dragDistance / maxDragDistancePixels
        val dampingFactor = 0.7f * (1f - currentProgress).pow(1.5f)
        
        // 计算调整后的增量
        val adjustedDelta = pullDelta * dampingFactor
        val newDragDistance = (dragDistance + adjustedDelta).coerceIn(0f, maxDragDistancePixels)
        dragDistance = newDragDistance
        updateProgress()
        
        // 始终返回完整的 pullDelta 以完全消耗滚动距离
        // 内容移动通过 contentOffset（等于 dragDistance）实现，不依赖滚动
        // 这样可以确保在 iOS 和 Android 上都能正常工作
        return pullDelta
    }

    /**
     * 松手处理
     */
    private suspend fun onRelease() {
        if (isRefreshing) return
        
        val shouldTriggerRefresh = dragDistance >= refreshThresholdPixels
        
        if (shouldTriggerRefresh) {
            // 先设置 isRefreshing = true，立即阻止后续滚动事件
            isRefreshing = true
            
            // 确保 dragDistance 立即设置为目标值，避免动画过程中的不一致
            val targetDistance = refreshThresholdPixels
            
            // 如果当前距离超过刷新阈值，平滑回到刷新位置
            if (dragDistance > targetDistance) {
                // 根据下拉距离动态计算动画时长：下拉越多，动画越长
                // 基础时长 180ms，每超过阈值 1px 增加 1ms，最大不超过 300ms
                val distanceToTravel = dragDistance - targetDistance
                val baseDuration = 180
                val extraDuration = (distanceToTravel * 1f).toInt().coerceAtMost(120)
                val animationDuration = baseDuration + extraDuration
                
                animatable.snapTo(dragDistance)
                animatable.animateTo(
                    targetValue = targetDistance,
                    // 使用先快后慢的缓动函数，模拟弹簧回弹效果
                    // 自定义贝塞尔曲线：开始更快（0.2），结束更慢（0.8），创造明显的弹簧效果
                    animationSpec = tween(
                        durationMillis = animationDuration,
                        easing = CubicBezierEasing(0.2f, 0f, 0.2f, 0.8f)
                    )
                ) {
                    dragDistance = value
                    updateProgress()
                }
                // 确保最终值准确
                dragDistance = targetDistance
                updateProgress()
            } else {
                // 如果已经在刷新位置附近，直接设置
                dragDistance = targetDistance
                updateProgress()
            }
            
            onRefresh()
        } else {
            // 回弹
            animatable.snapTo(dragDistance)
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) {
                dragDistance = value
                updateProgress()
            }
            dragDistance = 0f
            updateProgress()
        }
    }

    private fun updateProgress() {
        progress = (dragDistance / refreshThresholdPixels).coerceIn(0f, 1f)
    }

    /**
     * 完成刷新
     */
    internal suspend fun finishRefresh() {
        if (!isRefreshing) return
        
        isRefreshing = false
        
        // 400ms 柔和动画回到初始位置
        animatable.snapTo(dragDistance)
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
        ) {
            dragDistance = value
            updateProgress()
        }
        dragDistance = 0f
        updateProgress()
    }

    internal fun getIndicatorOffset(): Float = dragDistance
    internal fun getContentOffset(): Float = dragDistance
    internal fun getRefreshThresholdPixels(): Float = refreshThresholdPixels
}

private val RefreshThreshold = 80.dp  // 从 60.dp 增加到 80.dp，需要下拉更多距离才触发刷新
private val MaxDragDistance = 140.dp  // 从 120.dp 增加到 140.dp，允许下拉更远

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
    
    val state = remember {
        PullRefreshState(
            initialRefreshing = refreshing,
            onRefresh = onRefresh,
            refreshThreshold = refreshThreshold,
            maxDragDistance = maxDragDistance
        )
    }

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
