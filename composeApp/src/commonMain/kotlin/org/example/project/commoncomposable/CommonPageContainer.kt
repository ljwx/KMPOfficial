package org.example.project.commoncomposable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.example.project.common.BaseViewModel
import org.example.project.multiplestate.DefaultError
import org.example.project.multiplestate.MultiStateLayout
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.navigation.routes.BaseRouter

sealed interface PageContainerEvent {
    data object OnPullRefresh : PageContainerEvent
    data object OnMultiSateRetry : PageContainerEvent
}

data class PageContainerConfig(
    val enableMultiState: Boolean = true,
    val enablePullRefresh: Boolean = false
) {
    companion object {
        val Default = PageContainerConfig()
        val AlsoPull = PageContainerConfig(enableMultiState = true, enablePullRefresh = true)
    }
}

@Composable
fun CommonPageContainer(
    modifier: Modifier = Modifier,
    viewModel: BaseViewModel,
    config: PageContainerConfig = PageContainerConfig.Default,
    router: BaseRouter? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val multiState by viewModel.multiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    CommonPageScreen(
        modifier = modifier,
        multiState = multiState,
        isRefreshing = isRefreshing,
        onEvent = { event ->
            when (event) {
                PageContainerEvent.OnPullRefresh -> viewModel.pullRefresh()
                PageContainerEvent.OnMultiSateRetry -> viewModel.multiStateRetry()
            }
        },
        config = config,
        content = content
    )

}

@Composable
private fun CommonPageScreen(
    modifier: Modifier,
    multiState: MultiStateLayoutState,
    isRefreshing: Boolean,
    onEvent: (PageContainerEvent) -> Unit,
    config: PageContainerConfig = PageContainerConfig(),
    content: @Composable BoxScope.() -> Unit
) {

    if (config.enableMultiState) {
        multiState(modifier = modifier, multiState = multiState, onEvent = onEvent) {
            if (config.enablePullRefresh) {
                pullRefresh(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isRefreshing,
                    onEvent = onEvent,
                    content = content
                )
            } else {
                content()
            }
        }
    } else if (config.enablePullRefresh) {
        pullRefresh(
            modifier = modifier,
            isRefreshing = isRefreshing,
            onEvent = onEvent,
            content = content
        )
    } else {
        Box(modifier = modifier, content = content)
    }
}

@Composable
private fun multiState(
    modifier: Modifier,
    multiState: MultiStateLayoutState,
    onEvent: (PageContainerEvent) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    MultiStateLayout(
        state = multiState,
        modifier = modifier,
        errorView = { throwable, _ ->
            DefaultError(throwable) {
                onEvent(PageContainerEvent.OnMultiSateRetry)
            }
        },
        content = content
    )
}

@Composable
private fun pullRefresh(
    modifier: Modifier,
    isRefreshing: Boolean,
    onEvent: (PageContainerEvent) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.clipToBounds(),
        isRefreshing = isRefreshing,
        onRefresh = { onEvent(PageContainerEvent.OnPullRefresh) },
        state = pullToRefreshState,
        indicator = {
            // 只有在下拉或刷新时才显示
            if (isRefreshing || pullToRefreshState.distanceFraction > 0f) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha =
                                if (isRefreshing) 1f else pullToRefreshState.distanceFraction.coerceIn(
                                    0f,
                                    1f
                                )
                            // 👈 Indicator 从顶部外面(-60dp)被拉进来,随着下拉逐渐显示
                            // 当 distanceFraction = 0 时,translationY = -60dp (在顶部外面,被裁剪)
                            // 当 distanceFraction >= 1 时,translationY = 20dp (完全进入可见区域)
                            val indicatorHeight = 60.dp.toPx()
                            val pullDistance = pullToRefreshState.distanceFraction * 80.dp.toPx()
                            translationY = -indicatorHeight + pullDistance
                        }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pullToRefreshState.distanceFraction * 80.dp.toPx()
                    },
                contentAlignment = Alignment.TopCenter, content = content
            )
        }
    )
}