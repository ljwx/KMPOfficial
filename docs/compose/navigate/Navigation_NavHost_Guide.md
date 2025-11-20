# KMP 商业级架构完整示例 (Official Stack 2025)

> **核心原则：**
> * **单一事实来源 (SSOT):** 使用 Repository 模式管理数据。
> * **单向数据流 (UDF):** ViewModel 持有 StateFlow，UI 只负责渲染。
> * **高内聚低耦合:** 导航逻辑分散在各个 Feature 模块中，主工程只负责组装。

## 1. 项目目录结构 (Project Structure)

推荐采用 **Feature-First** (按功能分包) 的结构，而非 Layer-First。

```text
commonMain/kotlin/com/company/app
 ├── core/
 │    ├── navigation/
 │    │    └── Routes.kt          # 全局路由定义 (Serializable)
 │    └── di/
 │         └── AppModules.kt      # Koin 依赖注入模块
 ├── data/                        # 数据层 (Room/Ktor)
 ├── domain/                      # 业务层 (Model/Repository Interface)
 ├── features/
 │    ├── user_list/              # [功能模块 A] 用户列表
 │    │    ├── UserListScreen.kt
 │    │    ├── UserListViewModel.kt
 │    │    └── UserListNavigation.kt  <-- 独立的导航配置
 │    └── user_edit/              # [功能模块 B] 编辑用户 (模拟 startActivityForResult)
 │         ├── UserEditScreen.kt
 │         ├── UserEditViewModel.kt
 │         └── UserEditNavigation.kt
 └── App.kt                       # 根入口 (NavHost 组装)
```

## 2. 核心基础设施 (Core Infrastructure)

### 2.1 路由定义 (Type-Safe Routes)
使用 kotlinx.serialization 定义强类型路由。

```kotlin
// core/navigation/Routes.kt
import kotlinx.serialization.Serializable

// 1. 列表页
@Serializable
object UserListRoute

// 2. 详情页 (演示传参)
@Serializable
data class UserDetailRoute(val userId: String)

// 3. 编辑页 (演示返回结果)
@Serializable
data class UserEditRoute(val userId: String?)
```

### 2.2 依赖注入 (Koin)
配置 ViewModel 和 Repository 的注入。

```kotlin
// core/di/AppModules.kt
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Data Layer
    single<UserRepository> { UserRepositoryImpl(get()) } // 假设已有 Room Database
    
    // ViewModels (自动绑定)
    viewModelOf(::UserListViewModel)
    viewModelOf(::UserEditViewModel)
}
```

## 3. 功能模块 A: 用户列表 (接收结果 + 跳转)

### 3.1 ViewModel
```kotlin
// features/user_list/UserListViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UserListViewModel(private val repo: UserRepository) : ViewModel() {
    val uiState = MutableStateFlow<List<User>>(emptyList())

    // 处理从编辑页返回的数据
    fun onUserUpdated(message: String) {
        println("UI 收到反馈: $message")
        refreshData()
    }

    private fun refreshData() { /*...*/ }
}
```

### 3.2 模块化导航配置 (关键！)
注意： 这里处理了“接收返回结果”的逻辑。

```kotlin
// features/user_list/UserListNavigation.kt
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.userListGraph(
    navController: NavController,
    onNavigateToEdit: (String?) -> Unit, // 对外暴露意图
    onNavigateToDetail: (String) -> Unit
) {
    composable<UserListRoute> { backStackEntry ->
        // 注入 ViewModel
        val viewModel = koinViewModel<UserListViewModel>()
        
        // --- 核心：监听从 "编辑页" 返回的结果 (Result Handle) ---
        // 这里的 "edit_result_key" 必须和编辑页约定好
        val savedState = backStackEntry.savedStateHandle
        val editResult by savedState.getStateFlow<String?>("edit_result_key", null)
            .collectAsState()

        // 如果有结果，交给 ViewModel 处理，并清除状态
        if (editResult != null) {
            viewModel.onUserUpdated(editResult!!)
            savedState.remove<String>("edit_result_key")
        }

        // 渲染 UI
        UserListScreen(
            viewModel = viewModel,
            onAddClick = { onNavigateToEdit(null) },
            onItemClick = { userId -> onNavigateToDetail(userId) }
        )
    }
}
```

## 4. 功能模块 B: 用户编辑 (设置返回结果)
这是模拟 setResult / finish 的逻辑。

```kotlin
// features/user_edit/UserEditNavigation.kt
fun NavGraphBuilder.userEditGraph(
    navController: NavController
) {
    composable<UserEditRoute> { 
        // 注入 ViewModel (可以直接在 UI 里注入，也可以在这里注入传进去)
        val viewModel = koinViewModel<UserEditViewModel>()
        
        UserEditScreen(
            onSaveCompleted = { resultMessage ->
                // --- 核心：设置返回结果 ---
                // 1. 获取前一个页面的 SavedStateHandle
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("edit_result_key", resultMessage)
                
                // 2. 返回 (Pop)
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}
```

## 5. 根入口：App 组装 (App.kt)
这是整个 App 的“脊柱”，负责串联所有模块并配置全局动画。
```kotlin
// App.kt
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.koin.compose.KoinContext

@Composable
fun App() {
    // 初始化 Koin (如果没在 Application 级初始化，这里加上 Context)
    KoinContext {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = UserListRoute,
            // --- 全局 iOS/Android 风格平移动画 ---
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            }
        ) {
            // ==========================================
            // 在这里像搭积木一样组装各个模块
            // ==========================================

            // 1. 用户列表模块
            userListGraph(
                navController = navController,
                onNavigateToEdit = { userId ->
                    navController.navigate(UserEditRoute(userId))
                },
                onNavigateToDetail = { userId ->
                    navController.navigate(UserDetailRoute(userId))
                }
            )

            // 2. 用户编辑模块
            userEditGraph(navController)
            
            // 3. 详情模块 (略)
            // userDetailGraph(navController)
        }
    }
}
```

## 6. 为什么这是最佳实践？

1. 可维护性 (Maintainability): App.kt 非常干净。如果 UserList 页面逻辑变得极其复杂，它也只会影响 UserListNavigation.kt，不会污染全局路由文件。

2. 内存安全 (Memory Safety):

 - 使用 koinViewModel() 获取的 ViewModel 会自动绑定到当前的 Navigation Route 范围。

 - 当用户按返回键退出页面时，ViewModel 会自动调用 onCleared()，彻底释放内存（包括 ViewModel 内持有的 StateFlow 和 Repository 引用）。

3. 官方血统 (Official):

 - 使用 savedStateHandle 传递回传结果，这是 Android 官方推荐的机制，能抵抗“进程死亡”带来的数据丢失风险。

 - 使用 Serializable 进行路由，类型安全，编译期就能发现错误。