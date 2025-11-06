package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.serialization.Serializable

/**
 * AppNavigation 定义了导航层对外暴露的功能与数据。
 * UI 层只关注 *navigation.state* 如何渲染，以及调用 *openDetail()*、*navigateBack()* 等方法，不直接接触 Decompose API。
 */
interface AppNavigation {

    /** 目前导航状态，包含顶栏是否显示返回、以及哪个画面在前景。 */
    val state: State

    /** 导航到详情页。 */
    fun openDetail(title: String, message: String)

    /** 导航回上一页。 */
    fun navigateBack()

    /**
     * 将导航堆栈渲染成 Composable。
     * 这里通过传入的 *home* / *detail* lambda 实现界面组合，达到 Decompose 与 UI 解耦。
     */
    @Composable
    fun Render(
        modifier: Modifier,
        home: @Composable (Modifier) -> Unit,
        detail: @Composable (Modifier, State.ScreenState.Detail) -> Unit
    )

    /**
     * 封装 UI 需要感知的导航状态。
     */
    data class State(
        val canNavigateBack: Boolean,
        val activeScreen: ScreenState
    ) {
        /** 根据界面不同提供额外信息。 */
        sealed interface ScreenState {
            data object Home : ScreenState
            data class Detail(val title: String, val message: String) : ScreenState
        }
    }
}

/**
 * DecomposeAppNavigation 是 AppNavigation 的具体实现，真正使用 Decompose 来管理导航堆栈。
 */
internal class DecomposeAppNavigation(
    componentContext: ComponentContext
) : AppNavigation, ComponentContext by componentContext {

    /** StackNavigation 是 Decompose 提供的堆栈导航控制器。 */
    private val navigation = StackNavigation<Screen>()

    /**
     * childStack 建立并保存导航堆栈状态。
     * - serializer: Screen 序列化器，让 Decompose 能保存/恢复状态
     * - initialConfiguration: 起始界面
     * - childFactory: 将配置转换成具体的 Screen 实例
     */
    private val childStack: Value<ChildStack<Screen, Screen>> =
        childStack(
            source = navigation,
            serializer = Screen.serializer(),
            initialConfiguration = Screen.Home,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private val _state = mutableStateOf(computeState(childStack.value))
    override val state: AppNavigation.State get() = _state.value

    init {
        // 监听堆栈变化，转换成 UI 可消费的 state
        val cancellation = childStack.subscribe { stack ->
            _state.value = computeState(stack)
        }
        // 与 ComponentContext 的生命周期绑定，避免泄漏
        lifecycle.doOnDestroy { cancellation.cancel() }
    }

    override fun openDetail(title: String, message: String) {
        navigation.push(Screen.Detail(title = title, message = message))
    }

    override fun navigateBack() {
        navigation.pop()
    }

    @Composable
    override fun Render(
        modifier: Modifier,
        home: @Composable (Modifier) -> Unit,
        detail: @Composable (Modifier, AppNavigation.State.ScreenState.Detail) -> Unit
    ) {
        val stackAnimation = stackAnimation<Screen, Screen>(animator = slide())

        Children(
            stack = childStack,
            animation = stackAnimation
        ) { child ->
            when (val screen = child.instance) {
                Screen.Home -> home(modifier)
                is Screen.Detail -> detail(
                    modifier,
                    AppNavigation.State.ScreenState.Detail(screen.title, screen.message)
                )
            }
        }
    }

    /** 将 Decompose 的 ChildStack 转换成 AppNavigation.State，供 UI 使用。 */
    private fun computeState(stack: ChildStack<Screen, Screen>): AppNavigation.State {
        val active = stack.active.instance
        return when (active) {
            Screen.Home -> AppNavigation.State(
                canNavigateBack = stack.items.size > 1,
                activeScreen = AppNavigation.State.ScreenState.Home
            )

            is Screen.Detail -> AppNavigation.State(
                canNavigateBack = true,
                activeScreen = AppNavigation.State.ScreenState.Detail(
                    title = active.title,
                    message = active.message
                )
            )
        }
    }

    /** childFactory：这里我们的 Screen 已经是具体数据类，直接返回即可。 */
    private fun createChild(
        screen: Screen,
        @Suppress("UNUSED_PARAMETER") componentContext: ComponentContext
    ): Screen = screen

    /** 导航配置使用 kotlinx.serialization 序列化，方便跨平台保存。 */
    @Serializable
    private sealed interface Screen {
        @Serializable
        data object Home : Screen

        @Serializable
        data class Detail(val title: String, val message: String) : Screen
    }
}

/**
 * rememberAppNavigation() 提供可组合的工厂函数：
 * - 创建 LifecycleRegistry
 * - 与 Compose 生命周期绑定
 * - 返回 DecomposeAppNavigation 实例
 */
@Composable
fun rememberAppNavigation(): AppNavigation {
    val lifecycle = remember { LifecycleRegistry() }
    DisposableEffect(Unit) {
        lifecycle.resume()
        onDispose { lifecycle.destroy() }
    }
    return remember {
        DecomposeAppNavigation(DefaultComponentContext(lifecycle))
    }
}

