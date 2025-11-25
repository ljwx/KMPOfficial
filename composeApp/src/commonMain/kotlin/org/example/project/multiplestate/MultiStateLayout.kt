package org.example.project.multiplestate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.log.KSLog

sealed interface MultiStateLayoutState {
    object Content : MultiStateLayoutState
    object Loading : MultiStateLayoutState
    object Empty : MultiStateLayoutState
    data class Error(val throwable: Throwable?) : MultiStateLayoutState
    object Offline : MultiStateLayoutState
    object Extension : MultiStateLayoutState
}

@Composable
fun MultiStateLayout(
    modifier: Modifier = Modifier,
    state: MultiStateLayoutState,
    enableAnimation: Boolean = true,
    onRetry: () -> Unit = {},
    loadingView: @Composable (BoxScope.() -> Unit) = { DefaultLoading() },
    emptyView: @Composable (BoxScope.() -> Unit) = { DefaultEmpty() },
    errorView: @Composable (BoxScope.(throwable: Throwable?, onRetry: () -> Unit) -> Unit) = { throwable, retry ->
        DefaultError(
            throwable,
            retry
        )
    },
    offlineView: @Composable (BoxScope.(onRetry: () -> Unit) -> Unit) = { retry ->
        DefaultOffline(
            retry
        )
    },
    extensionView: @Composable (BoxScope.() -> Unit) = { /* Default is empty */ },
    content: @Composable BoxScope.() -> Unit,
) {
    KSLog.iMultiState("当前状态：${state::class.simpleName}")

    if (enableAnimation) {
        AnimatedContent(
            targetState = state,
            modifier = modifier,
            transitionSpec = {
                // Refactored to a helper function for readability
                getMultiStateTransition(initialState = initialState, targetState = targetState)
            },
            label = "MultiStateLayoutAnimation"
        ) { targetState ->
            StateContainer(
                state = targetState,
                content = content,
                loadingView = loadingView,
                emptyView = emptyView,
                errorView = errorView,
                offlineView = offlineView,
                onRetry = onRetry,
                extensionView = extensionView
            )
        }
    } else {
        StateContainer(
            modifier = modifier,
            state = state,
            content = content,
            loadingView = loadingView,
            emptyView = emptyView,
            errorView = errorView,
            offlineView = offlineView,
            onRetry = onRetry,
            extensionView = extensionView
        )
    }
}

private fun getMultiStateTransition(
    initialState: MultiStateLayoutState,
    targetState: MultiStateLayoutState
): ContentTransform {
    val isEnteringContent = targetState is MultiStateLayoutState.Content
    val isExitingContent = initialState is MultiStateLayoutState.Content

    return if (isEnteringContent && !isExitingContent) {
        val enter = fadeIn() + scaleIn(initialScale = 0.8f)
        val exit = fadeOut() + scaleOut(targetScale = 1.2f)
        enter togetherWith exit
    } else if (!isEnteringContent && isExitingContent) {
        val enter = fadeIn() + scaleIn(initialScale = 1.2f)
        val exit = fadeOut() + scaleOut(targetScale = 0.8f)
        enter togetherWith exit
    } else {
        fadeIn() togetherWith fadeOut()
    }
}

@Composable
private fun StateContainer(
    modifier: Modifier = Modifier,
    state: MultiStateLayoutState,
    onRetry: () -> Unit,
    loadingView: @Composable (BoxScope.() -> Unit),
    emptyView: @Composable (BoxScope.() -> Unit),
    errorView: @Composable (BoxScope.(throwable: Throwable?, onRetry: () -> Unit) -> Unit),
    offlineView: @Composable (BoxScope.(onRetry: () -> Unit) -> Unit),
    extensionView: @Composable (BoxScope.() -> Unit),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is MultiStateLayoutState.Content -> content()
            is MultiStateLayoutState.Loading -> loadingView()
            is MultiStateLayoutState.Empty -> emptyView()
            is MultiStateLayoutState.Error -> errorView(state.throwable, onRetry)
            is MultiStateLayoutState.Offline -> offlineView(onRetry)
            is MultiStateLayoutState.Extension -> extensionView()
        }
    }
}

@Composable
fun BoxScope.DefaultLoading() {
    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
}

@Composable
fun BoxScope.DefaultEmpty() {
    Text(text = "No data available", modifier = Modifier.align(Alignment.Center))
}

@Composable
fun BoxScope.DefaultError(throwable: Throwable?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = throwable?.message ?: "An unexpected error occurred")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun BoxScope.DefaultOffline(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "You're offline. Please check your connection.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
