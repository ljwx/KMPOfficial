# 在 KMP Compose 项目中使用 ViewModel

在 Kotlin Multiplatform (KMP) 项目中，尤其是在使用 Jetpack Compose 作为 UI 框架时，"ViewModel" 的概念与传统纯 Android 开发中的 `ViewModel` 有着显著的区别。理解这些差异对于构建健壮、可维护的多平台应用至关重要。

## 1. 传统 Android `ViewModel` vs. KMP 中的 "ViewModel"

### 传统 Android `ViewModel` (`androidx.lifecycle.ViewModel`)

- **平台特定**: 这是 Android Jetpack 库的一部分，只能在 Android 平台使用。
- **生命周期感知**: 它的核心特性是能够感知并绑定到 Android 的生命周期（如 `Activity` 或 `Fragment`）。
- **配置变更后存活**: 最重要的功能是在屏幕旋转等配置变更发生时，`ViewModel` 实例能够存活，从而保留其内部的数据状态，避免数据丢失和重新加载。
- **获取方式**: 在 Compose for Android 中，通常使用 `viewModel()` 委托函数来获取实例。

### KMP 项目中的 "ViewModel" (位于 `commonMain`)

- **平台无关**: 这是一个普通的 Kotlin 类，不依赖任何特定于平台的库。它存在于 `commonMain` 中，因此可以被所有目标平台（Android, iOS, Desktop, Web）共享。
- **无生命周期意识**: 它本身不知道任何关于 UI 生命周期的事情。它的创建、保留和销毁需要手动管理或通过多平台框架来处理。
- **角色**: 它的职责与 Android `ViewModel` 相同：分离业务逻辑与UI，为UI持有并提供状态（通常通过 `StateFlow`），并暴露函数来处理用户事件。

**核心区别**: 在 KMP 中，我们不能在 `commonMain` 中直接使用 `androidx.lifecycle.ViewModel`。我们创建的是一个“ViewModel 风格”的普通类，然后采用多平台方案来管理它的生命周期。

## 2. KMP 中 ViewModel 的生命周期管理

既然 `commonMain` 中的 ViewModel 无法自动管理生命周期，我们如何确保它在需要时存在，在不需要时被正确销-毁？答案是使用专门为 KMP 设计的框架。

### 推荐方案: Decompose

[Decompose](https://github.com/arkivanov/Decompose) 是由 JetBrains 工程师开发的、用于 KMP 的组件化导航库。它完美地解决了 ViewModel 的生命周期管理问题。

在 Decompose 中，我们不称之为 "ViewModel"，而是称之为 **Component**。每个 Component 都有自己的、独立于平台的生命周期。

**工作流程**:

1.  **定义 Component**: 在 `commonMain` 中，为你的屏幕或功能创建一个 Component 接口和其实现类。这个实现类就是你的 ViewModel。
2.  **注入 `ComponentContext`**: 让你的 Component 实现类在其构造函数中接收一个 `ComponentContext` 参数。这个 `context` 对象提供了生命周期 (`lifecycle`) 和状态保存 (`stateKeeper`) 的能力。
3.  **使用 Coroutine Scope**: 你可以使用 `essenty-lifecycle-coroutines` 库提供的 `coroutineScope()` 扩展函数来创建协程作用域，这些协程会在 Component 销毁时自动取消，完美替代了 Android 的 `viewModelScope`。

### 示例

**`commonMain/kotlin/.../MyScreenComponent.kt`**
```kotlin
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// 1. 定义接口
interface MyScreenComponent {
    val model: StateFlow<Model>

    fun onButtonClicked()

    data class Model(val text: String)
}

// 2. 创建实现类 (这就是你的 ViewModel)
class DefaultMyScreenComponent(
    componentContext: ComponentContext // 注入 ComponentContext
) : MyScreenComponent, ComponentContext by componentContext { // 委托实现

    // 状态管理
    private val _model = MutableStateFlow(MyScreenComponent.Model("Initial Text"))
    override val model: StateFlow<MyScreenComponent.Model> = _model

    // 使用 coroutineScope() 扩展函数创建与生命周期绑定的协程作用域
    // ComponentContext 实现了 LifecycleOwner，可以直接调用 coroutineScope()
    private val componentScope = coroutineScope()

    init {
        // CoroutineScope 会在 Component 销毁时自动取消
        componentScope.launch { 
            // 在这里处理你的业务逻辑
        }
    }

    override fun onButtonClicked() {
        _model.value = MyScreenComponent.Model("Button Clicked!")
    }
}
```

**依赖配置：**
```kotlin
// build.gradle.kts
implementation("com.arkivanov.essenty:lifecycle-coroutines:2.5.0")
```

**`commonMain/kotlin/.../RootComponent.kt` (导航层)**
```kotlin
// 在导航组件中，Decompose 会负责创建和销毁子组件
class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()
    
    // Decompose 会在导航栈变化时，自动处理 MyScreenComponent 的生命周期
    val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        initialConfiguration = Configuration.ScreenA,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Configuration, context: ComponentContext): RootComponent.Child {
        return when(config) {
            is Configuration.ScreenA -> RootComponent.Child.ScreenA(DefaultMyScreenComponent(context))
            // ... other screens
        }
    }
}
```

## 3. 最佳实践总结

1.  **在 `commonMain` 中创建**: 将你的 ViewModel (或 Decompose Component) 定义为普通 Kotlin 类，并放在 `commonMain` 中，以实现最大程度的代码共享。
2.  **使用 `StateFlow`**: 使用 `kotlinx.coroutines.flow.StateFlow` 来持有和暴露UI状态。
3.  **依赖注入**: 通过构造函数将依赖项（如 Repositories, UseCases）注入到你的 ViewModel 中。
4.  **拥抱多平台框架**: 不要试图手动管理生命周期。使用像 Decompose 这样的库来处理导航和组件的生命周期，这是最健壮、最推荐的做法。
5.  **平台特定逻辑**: 如果需要调用平台特定的API（如获取设备信息），请使用 Kotlin 的 `expect/actual` 机制。在 `commonMain` 中定义 `expect` 函数，然后在每个平台（`androidMain`, `iosMain`）中提供 `actual` 实现。

