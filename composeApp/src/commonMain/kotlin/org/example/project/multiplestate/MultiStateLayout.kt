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

/**
 * A sealed interface representing the different states for [MultiStateLayout].
 */
sealed interface MultiStateLayoutState {
    /** Represents the content state, where the main UI is displayed. */
    object Content : MultiStateLayoutState
    /** Represents the loading state, typically shown during data fetching. */
    object Loading : MultiStateLayoutState
    /** Represents the empty state, shown when there is no data to display. */
    object Empty : MultiStateLayoutState
    /**
     * Represents the error state, shown when an error occurs.
     * @param throwable The exception that occurred.
     */
    data class Error(val throwable: Throwable) : MultiStateLayoutState
    /** Represents the offline state, shown when there is no network connectivity. */
    object Offline : MultiStateLayoutState
    /** Represents a custom extension state for more specific use cases. */
    object Extension : MultiStateLayoutState
}

/**
 * A composable that displays different UI based on the given [state].
 * It provides slots for content, loading, empty, error, offline, and a custom extension state.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param state The current state to be represented by the layout.
 * @param enableAnimation A boolean to enable or disable the transition animations between states.
 * @param onRetry A lambda to be invoked when the retry button in the error or offline state is clicked.
 * @param loadingView A composable slot for the loading state. Defaults to [DefaultLoading].
 * @param emptyView A composable slot for the empty state. Defaults to [DefaultEmpty].
 * @param errorView A composable slot for the error state. Defaults to [DefaultError].
 * @param offlineView A composable slot for the offline state. Defaults to [DefaultOffline].
 * @param extensionView A composable slot for the custom extension state.
 * @param content The main content to be displayed in the [MultiStateLayoutState.Content] state.
 */
@Composable
fun MultiStateLayout(
    modifier: Modifier = Modifier,
    state: MultiStateLayoutState,
    enableAnimation: Boolean = true,
    onRetry: () -> Unit = {},
    loadingView: @Composable (BoxScope.() -> Unit) = { DefaultLoading() },
    emptyView: @Composable (BoxScope.() -> Unit) = { DefaultEmpty() },
    errorView: @Composable (BoxScope.(throwable: Throwable, onRetry: () -> Unit) -> Unit) = { throwable, retry -> DefaultError(throwable, retry) },
    offlineView: @Composable (BoxScope.(onRetry: () -> Unit) -> Unit) = { retry -> DefaultOffline(retry) },
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

/**
 * A private helper function to determine the ContentTransform for MultiStateLayout's animation.
 * It creates a layered scaling and fading effect based on the transition between states.
 *
 * @param initialState The state being transitioned from.
 * @param targetState The state being transitioned to.
 * @return The calculated [ContentTransform].
 */
private fun getMultiStateTransition(
    initialState: MultiStateLayoutState,
    targetState: MultiStateLayoutState
): ContentTransform {
    val isEnteringContent = targetState is MultiStateLayoutState.Content
    val isExitingContent = initialState is MultiStateLayoutState.Content

    return if (isEnteringContent && !isExitingContent) {
        // Transition from any state TO Content state
        // Content scales up and fades in from the "bottom" layer.
        val enter = fadeIn() + scaleIn(initialScale = 0.8f)
        // The other state scales up and fades out from the "top" layer.
        val exit = fadeOut() + scaleOut(targetScale = 1.2f)
        enter togetherWith exit
    } else if (!isEnteringContent && isExitingContent) {
        // Transition from Content state TO any other state
        // The other state scales down and fades in to the "top" layer.
        val enter = fadeIn() + scaleIn(initialScale = 1.2f)
        // Content scales down and fades out to the "bottom" layer.
        val exit = fadeOut() + scaleOut(targetScale = 0.8f)
        enter togetherWith exit
    } else {
        // Default fade transition for changes between non-content states (e.g., Loading -> Error).
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
    errorView: @Composable (BoxScope.(throwable: Throwable, onRetry: () -> Unit) -> Unit),
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
private fun BoxScope.DefaultLoading() {
    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
}

@Composable
private fun BoxScope.DefaultEmpty() {
    Text(text = "No data available", modifier = Modifier.align(Alignment.Center))
}

@Composable
private fun BoxScope.DefaultError(throwable: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = throwable.message ?: "An unexpected error occurred")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun BoxScope.DefaultOffline(onRetry: () -> Unit) {
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
