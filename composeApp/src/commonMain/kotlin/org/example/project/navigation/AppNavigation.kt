package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.example.project.log.KSLog
import org.example.project.util.TimeUtils
import kotlin.random.Random

val LocalAppNavigation = staticCompositionLocalOf<IAppNavigation> {
    error("No AppNavigation provided")
}

@Serializable
enum class LaunchMode {
    /** 标准模式：每次都创建新实例并入栈 */
    STANDARD,

    /** 如果栈顶已经是该页面，则复用*/
    SINGLE_TOP,

    /** 如果栈中存在该页面，则清除其上所有页面并复用*/
    SINGLE_TASK,

    /**清除整个栈，只保留该页面 */
    SINGLE_INSTANCE
}

val routerJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

@Serializable
data class ScreenRouterData(
    val router: String,
    val launchMode: LaunchMode = LaunchMode.SINGLE_TOP,
    val paramsJson: String? = null,
    val params: Map<String, String>? = null,
    val requestId: String? = null  // 用于标识结果回调的请求ID
) {

    inline fun <reified T> getParamsMap(key: String, default: T): T {
        val value = params?.get(key) ?: return default

        return when (T::class) {
            Int::class -> value.toIntOrNull() as? T ?: default
            Long::class -> value.toLongOrNull() as? T ?: default
            Float::class -> value.toFloatOrNull() as? T ?: default
            Double::class -> value.toDoubleOrNull() as? T ?: default
            Boolean::class -> value.toBooleanStrictOrNull() as? T ?: default
            String::class -> value as? T ?: default
            else -> {
                value as? T ?: default
            }
        }
    }

    inline fun <reified T> getParamsSerialize(): T? {
        if (paramsJson == null) return null
        return try {
            routerJson.decodeFromString<T>(paramsJson)
        } catch (e: Exception) {
            KSLog.eRouter("反序列化参数失败", e)
            null
        }
    }
}

@Serializable
data class ScreenRouterState(val canNavigateBack: Boolean, val activeScreen: ScreenRouterData)

interface IAppNavigation {

    fun getCurrentActiveInstance(): ScreenRouterData

    fun openScreen(
        router: String,
        params: Map<String, String>? = null,
        launchMode: LaunchMode = LaunchMode.SINGLE_TOP
    )

    fun <T> openScreen(
        router: String,
        params: T,
        serializer: kotlinx.serialization.KSerializer<T>,
        launchMode: LaunchMode = LaunchMode.SINGLE_TOP
    )

    fun <T, R> openScreenForResult(
        router: String,
        params: T,
        serializer: kotlinx.serialization.KSerializer<T>,
        onResult: (R) -> Unit,
        launchMode: LaunchMode = LaunchMode.SINGLE_TOP
    )

    fun <R> openScreenForResult(
        router: String,
        params: Map<String, String>? = null,
        onResult: (R) -> Unit,
        launchMode: LaunchMode = LaunchMode.SINGLE_TOP
    )

    fun navigateBack()

    fun <R> navigateBackWithResult(result: R)

    @Composable
    fun Render(
        modifier: Modifier,
        router: ScreenRouterData?,
        renderScreen: @Composable (Modifier, ScreenRouterData, AppNavigation) -> Unit
    )

    fun customStackAnimation(router: ScreenRouterData?): StackAnimation<ScreenRouterData, ScreenRouterData>
    
    /**
     * 获取导航堆栈，用于平台特定的集成（如 Web 历史记录同步）
     */
    fun getChildStack(): Value<ChildStack<ScreenRouterData, ScreenRouterData>>
}

class AppNavigation(componentContext: ComponentContext) : IAppNavigation,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<ScreenRouterData>()

    private val resultCallbacks = mutableMapOf<String, (Any?) -> Unit>()

    private val childStack: Value<ChildStack<ScreenRouterData, ScreenRouterData>> =
        childStack(
            source = navigation,
            serializer = ScreenRouterData.serializer(),
            initialConfiguration = ScreenRouterData(APP_SPLASH),
            handleBackButton = true,
            childFactory = { config, componentContext ->
                config
            }
        )

    override fun getCurrentActiveInstance(): ScreenRouterData {
        return childStack.value.active.instance
    }

    init {
        var previousStack: ChildStack<ScreenRouterData, ScreenRouterData>? = null

        val cancellation = childStack.subscribe { stack ->
            val currentRouter = stack.active.instance.router
            val currentStackSize = stack.items.size
            KSLog.iRouter("当前页面:$currentRouter, 栈深度:$currentStackSize")

            // 检测到页面被 pop 的情况
            if (previousStack != null) {
                val previousStackSize = previousStack!!.items.size
                if (currentStackSize < previousStackSize ||
                    (previousStack!!.active.instance.router != currentRouter && currentStackSize <= previousStackSize)
                ) {
                    // 获取被 pop 的页面信息
                    val poppedScreen = previousStack!!.active.instance
                    handlePagePopped(poppedScreen, currentRouter)
                }
            }

            previousStack = stack
        }
        lifecycle.doOnDestroy {
            cancellation.cancel()
            resultCallbacks.clear()
        }
    }

    private fun handlePagePopped(poppedScreen: ScreenRouterData, currentRouter: String?) {
        KSLog.iRouter("页面被 pop: ${poppedScreen.router} -> $currentRouter")
        // 如果被 pop 的页面有 requestId，说明是通过 openScreenForResult 打开的
        // 用户直接返回（调用 navigateBack）而没有调用 navigateBackWithResult
        // 需要清理对应的回调，避免内存泄漏
        val requestId = poppedScreen.requestId
        if (requestId != null) {
            val removedCallback = resultCallbacks.remove(requestId)
            if (removedCallback != null) {
                KSLog.wRouter("页面 ${poppedScreen.router} 被直接返回，清理未处理的结果回调, requestId:$requestId")
                // 可选：如果需要通知调用方取消操作，可以在这里触发一个取消回调
                // 但通常直接清理即可，因为用户主动返回表示取消操作
            }
        }
    }

    private fun generateRequestId(): String {
        return "req_${TimeUtils.currentTimeMillis()}_${Random.nextLong(10000, 99999)}"
    }

    override fun openScreen(router: String, params: Map<String, String>?, launchMode: LaunchMode) {
        openScreenWithLaunchMode(
            ScreenRouterData(
                router = router,
                params = params,
                launchMode = launchMode
            )
        )
    }

    override fun <T> openScreen(
        router: String,
        params: T,
        serializer: kotlinx.serialization.KSerializer<T>,
        launchMode: LaunchMode
    ) {
        val paramsJson = try {
            routerJson.encodeToString(serializer, params)
        } catch (e: Exception) {
            KSLog.eRouter("序列化参数失败,router:$router", e)
            return
        }
        openScreenWithLaunchMode(
            ScreenRouterData(
                router = router,
                paramsJson = paramsJson,
                launchMode = launchMode
            )
        )
    }

    /**
     * 根据启动模式处理导航逻辑
     */
    private fun openScreenWithLaunchMode(router: ScreenRouterData) {
        KSLog.iRouter("跳转页面:${router.router}, 启动模式:${router.launchMode}")
        val currentStack = childStack.value
        val stackItems = currentStack.items

        when (router.launchMode) {
            LaunchMode.STANDARD -> {
                navigation.push(router)
            }

            LaunchMode.SINGLE_TOP -> {
                val topScreen = stackItems.lastOrNull()
                if (topScreen?.instance?.router == router.router) {
                    KSLog.iRouter("栈顶是${router.router}，复用当前实例")
                    // 可以在这里触发刷新逻辑，如果需要的话
                } else {
                    navigation.push(router)
                }
            }

            LaunchMode.SINGLE_TASK -> {
                // SingleTask：查找栈中是否存在该页面
                val existingIndex = stackItems.indexOfFirst { it.instance.router == router.router }
                if (existingIndex >= 0) {
                    // 找到已存在的页面，清除其上所有页面
                    val existingConfig = stackItems[existingIndex].configuration
                    val clearCount = stackItems.size - existingIndex - 1
                    // 使用 navigate 自定义堆栈操作：保留到目标页面的所有页面
                    navigation.navigate { stack ->
                        val targetEntry = stack[existingIndex]
                        val keptStack = stack.take(existingIndex + 1)
                        // 如果配置不同，替换最后一个配置
                        if (existingConfig != router) {
                            keptStack.dropLast(1) + targetEntry.copy()
                        } else {
                            keptStack
                        }
                    }
                    KSLog.iRouter("之前已存在 ${router.router}，清除其上 $clearCount 个页面")
                } else {
                    navigation.push(router)
                }
            }

            LaunchMode.SINGLE_INSTANCE -> {
                // SingleInstance：清除整个栈，只保留该页面
                navigation.navigate { stack ->
                    // 只保留新页面，使用第一个 entry 的框架，但替换配置
                    listOf(stack.first().copy())
                }
                KSLog.iRouter("清除整个栈，只保留 ${router.router}")
            }
        }
    }

    override fun <T, R> openScreenForResult(
        router: String,
        params: T,
        serializer: kotlinx.serialization.KSerializer<T>,
        onResult: (R) -> Unit,
        launchMode: LaunchMode
    ) {
        val requestId = generateRequestId()
        resultCallbacks[requestId] = onResult as (Any?) -> Unit

        val paramsJson = try {
            routerJson.encodeToString(serializer, params)
        } catch (e: Exception) {
            KSLog.eRouter("序列化参数失败,router:$router", e)
            resultCallbacks.remove(requestId) // 清理回调
            return
        }
        openScreenWithLaunchMode(
            ScreenRouterData(
                router = router,
                paramsJson = paramsJson,
                requestId = requestId,
                launchMode = launchMode
            )
        )
    }

    override fun <R> openScreenForResult(
        router: String,
        params: Map<String, String>?,
        onResult: (R) -> Unit,
        launchMode: LaunchMode
    ) {
        val requestId = generateRequestId()
        resultCallbacks[requestId] = onResult as (Any?) -> Unit
        openScreenWithLaunchMode(
            ScreenRouterData(
                router = router,
                params = params,
                requestId = requestId,
                launchMode = launchMode
            )
        )
    }

    override fun navigateBack() {
        KSLog.iRouter("返回上一页")
        navigation.pop()
    }

    override fun <R> navigateBackWithResult(result: R) {
        val currentStack = childStack.value
        val currentScreen = currentStack.active.instance

        // 获取当前页面的 requestId
        val requestId = currentScreen.requestId
        if (requestId != null) {
            // 查找对应的回调
            val callback = resultCallbacks.remove(requestId)
            if (callback != null) {
                KSLog.iRouter("触发结果回调, requestId:$requestId")
                try {
                    callback(result)
                } catch (e: Exception) {
                    KSLog.eRouter("执行结果回调失败", e)
                }
            } else {
                KSLog.wRouter("未找到对应的结果回调, requestId:$requestId")
            }
        } else {
            KSLog.wRouter("当前页面没有 requestId，无法返回结果")
        }

        // 返回上一页
        navigation.pop()
    }

    override fun customStackAnimation(router: ScreenRouterData?): StackAnimation<ScreenRouterData, ScreenRouterData> {
        return stackAnimation(animator = slide())
    }

    override fun getChildStack(): Value<ChildStack<ScreenRouterData, ScreenRouterData>> {
        return childStack
    }

    @Composable
    override fun Render(
        modifier: Modifier,
        router: ScreenRouterData?,
        renderScreen: @Composable (Modifier, ScreenRouterData, AppNavigation) -> Unit
    ) {
        Children(stack = childStack, animation = customStackAnimation(router)) { child ->
            renderScreen(modifier, child.instance, this)
        }
    }

}

@Composable
fun rememberAppNavigation(): AppNavigation {
    val lifecycle = remember { LifecycleRegistry() }
    DisposableEffect(Unit) {
        lifecycle.resume()
        onDispose {
            lifecycle.destroy()
        }
    }
    return remember {
        KSLog.iRouter("创建AppNavigation")
        AppNavigation(DefaultComponentContext(lifecycle))
    }
}

inline fun <reified T> IAppNavigation.openScreen(
    router: String,
    params: T,
    launchMode: LaunchMode = LaunchMode.SINGLE_TOP
) {
    openScreen(router, params, serializer(), launchMode)
}

/**
 * 便捷扩展函数：打开页面并等待结果（使用序列化）
 */
inline fun <reified T, reified R> IAppNavigation.openScreenForResult(
    router: String,
    params: T,
    noinline onResult: (R) -> Unit,
    launchMode: LaunchMode = LaunchMode.SINGLE_TOP
) {
    openScreenForResult(router, params, serializer(), onResult, launchMode)
}

/**
 * 便捷扩展函数：打开页面并等待结果（使用 Map 参数）
 */
inline fun <reified R> IAppNavigation.openScreenForResult(
    router: String,
    params: Map<String, String>? = null,
    noinline onResult: (R) -> Unit,
    launchMode: LaunchMode = LaunchMode.SINGLE_TOP
) {
    openScreenForResult(router, params, onResult, launchMode)
}

