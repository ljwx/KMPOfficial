这是一份完全整合的、企业级 KMP 开发标准文档。

它将 Google 官方 Navigation 与 Koin 4.0 依赖注入 完美融合，并适配了 Android / iOS / Web (Wasm) 三端。

## KMP 商业项目核心架构指南 (2025版)

适用场景： 中大型商业项目 (10W+ 日活) 核心技术栈： Compose Multiplatform + Jetpack Navigation (官方) + Koin 4.0 + Clean Architecture 支持平台： Android, iOS, Web (Wasm)

### 1. 依赖管理配置 (libs.versions.toml)

这是项目的基础。必须严格使用支持 Wasm 和 KMP 的版本。

```kotlin
[versions]
kotlin = "2.0.0"
# 核心：Koin 4.0 是支持 Wasm 的分水岭
koin = "4.0.0"
# 核心：官方导航
navigation = "2.8.0-alpha08"
# 官方 ViewModel
lifecycle = "2.8.0" 

[libraries]
# === Navigation ===
androidx-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }

# === ViewModel ===
androidx-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }

# === Koin (必须用 core 和 compose) ===
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

### 2. 基础设施层 (Infrastructure Setup)

### 2.1 依赖注入模块定义 (Common)
在 commonMain 中定义你的 Module。使用 viewModelOf 可以自动匹配构造函数参数。

```kotlin
// commonMain/.../di/AppModule.kt
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.company.app.features.user.UserViewModel
import com.company.app.data.UserRepositoryImpl
import com.company.app.domain.UserRepository

val appModule = module {
    // Single 单例 (Repository, Database, Network)
    single<UserRepository> { UserRepositoryImpl() }
    
    // ViewModel 工厂 (每次获取新的，自动注入 Repository)
    viewModelOf(::UserViewModel)
}
```

### 2.2 跨平台 Koin 初始化器 (Common)

关键点： 不要直接在 UI 里 startKoin。写一个 helper 函数供各平台入口调用。

```kotlin
// commonMain/.../di/KoinInit.kt
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule)
}
```

## 3. 路由定义 (Type-Safe Routes)
使用 Kotlin Serialization 定义路由，杜绝硬编码字符串。

```kotlin
// commonMain/.../navigation/Routes.kt
import kotlinx.serialization.Serializable

@Serializable
object UserListRoute

@Serializable
data class UserEditRoute(val userId: String) // 传参
```

## 4. 功能模块开发 (Feature Implementation)

以“用户模块”为例，展示 ViewModel、UI 和 模块化导航的写法。

### 4.1 ViewModel (标准 MVVM)
继承官方 ViewModel。

```kotlin
// commonMain/.../features/user/UserViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    // UI 状态
    val state = MutableStateFlow("Loading...")

    fun loadData() {
        viewModelScope.launch {
           state.value = repository.getData()
        }
    }
    
    fun saveUser(id: String) {
        // 模拟保存逻辑
    }
}
```

### 4.2 模块化导航图 (Feature Navigation Graph)

这是架构的核心： 将导航逻辑下沉到模块内部。

```kotlin
// commonMain/.../features/user/UserNavigation.kt
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel // 引入 Koin

// 扩展函数：将 User 模块挂载到全局导航图上
fun NavGraphBuilder.userGraph(
    navController: NavController
) {
    // 1. 用户列表页
    composable<UserListRoute> { backStackEntry ->
        // Koin 自动注入 ViewModel (生命周期安全)
        val viewModel = koinViewModel<UserViewModel>()
        val state by viewModel.state.collectAsState()

        // --- 核心：接收从编辑页返回的结果 ---
        val savedState = backStackEntry.savedStateHandle
        val result by savedState.getStateFlow<String?>("edit_result", null).collectAsState()
        
        if (result != null) {
             println("收到结果: $result")
             savedState.remove<String>("edit_result") // 消费后清除
        }

        UserListScreen(
            state = state,
            onEditClick = { userId ->
                navController.navigate(UserEditRoute(userId))
            }
        )
    }

    // 2. 用户编辑页
    composable<UserEditRoute> { backStackEntry ->
        // 自动注入同一类型的 ViewModel (全新实例)
        val viewModel = koinViewModel<UserViewModel>() 
        
        // 获取参数
        val route = backStackEntry.toRoute<UserEditRoute>()

        UserEditScreen(
            userId = route.userId,
            onSaveAndExit = {
                // --- 核心：设置返回结果 ---
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("edit_result", "Success")
                
                navController.popBackStack()
            }
        )
    }
}
```

## 5. 全局入口 (App.kt)

这里进行 DI 上下文注入 和 全局导航组装。

```kotlin
// commonMain/.../App.kt
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.koin.compose.KoinContext

@Composable
fun App() {
    // 1. KoinContext: 为 Wasm/iOS 提供依赖注入环境插座
    KoinContext {
        val navController = rememberNavController()

        // 2. NavHost: 管理全局路由和动画
        NavHost(
            navController = navController,
            startDestination = UserListRoute,
            // 全局 Android 风格动画
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            // 3. 组装各模块
            userGraph(navController)
            
            // settingsGraph(navController) ... 其他模块
        }
    }
}
```

## 6. 各平台入口配置 (Platform Entry Points)
这是最后一步，确保 Koin 在 App 启动瞬间被初始化。

### 6.1 Android (MyApplication.kt)

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApplication)
        }
    }
}
```

### 6.2 iOS (MainViewController.kt)
```kotlin
fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) {
    App()
}
```

### 6.3 Web / Wasm (main.kt)

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 必须在 Window 创建前初始化 Koin
    initKoin()
    
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App()
    }
}
```

## 7. 为什么这套架构是正规且可靠的？

1. 内存泄漏防护：

 - koinViewModel() 紧密绑定 Navigation 的生命周期。

 - 当 navController.popBackStack() 时，ViewModel 自动调用 onCleared()，Koin 释放实例，完全不必担心手动管理内存。

2. 类型安全 (Type Safety):

 - 使用 UserEditRoute(id="123") 而不是 Maps("user/edit/123")。这在大型项目中能避免 90% 的拼写错误 Crash。

3. Wasm 兼容性：

 - 通过 KoinContext + initKoin 的分离设计，完美解决了浏览器端没有 Application Context 的问题。

4. 解耦 (Decoupling):

 - UserGraph 只负责用户的流程。App.kt 不需要知道用户编辑页的具体实现，只需要调用 userGraph(navController)。这非常适合多人协作开发。