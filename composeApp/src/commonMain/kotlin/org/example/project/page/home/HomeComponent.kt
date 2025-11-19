package org.example.project.page.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.navigation.ScreenComponent
import org.example.project.navigation.ScreenRouterData
import org.example.project.network.api.ProductApiService
import org.example.project.network.model.ProductSummaryData

/**
 * 首页 Component 接口
 * 这是标准的 Decompose Component 架构，相当于 ViewModel
 */
interface HomeComponent : ScreenComponent {
    val products: StateFlow<List<ProductSummaryData>>
    val multiState: StateFlow<MultiStateLayoutState>

    fun loadProducts()
}

/**
 * 首页 Component 实现
 * Component 的生命周期独立于 Compose 组合，状态不会因为组合被移除而丢失
 *
 * 使用 lifecycle.coroutineScope 管理协程，这是 Decompose 官方推荐的标准做法
 * 协程会在 Component 销毁时自动取消，无需手动管理
 */
class DefaultHomeComponent(
    componentContext: ComponentContext,
    override val config: ScreenRouterData = ScreenRouterData(""),
    private val apiService: ProductApiService = ProductApiService()
) : HomeComponent, ComponentContext by componentContext {

    private val _products = MutableStateFlow<List<ProductSummaryData>>(emptyList())
    override val products: StateFlow<List<ProductSummaryData>> = _products.asStateFlow()

    private val _multiState = MutableStateFlow<MultiStateLayoutState>(MultiStateLayoutState.Loading)
    override val multiState: StateFlow<MultiStateLayoutState> = _multiState.asStateFlow()

    // 使用 coroutineScope 扩展函数创建与生命周期绑定的协程作用域
    // 这个 scope 会在 Component 销毁时自动取消，完美替代 viewModelScope
    // ComponentContext 实现了 LifecycleOwner，所以可以直接调用 coroutineScope()
    private val componentScope = coroutineScope()

    init {
        // 使用与生命周期绑定的协程作用域启动协程
        componentScope.launch {
            loadProducts()
        }
    }

    override fun loadProducts() {
        // 使用与生命周期绑定的协程作用域启动协程
        componentScope.launch {
            apiService.getProducts().onSuccess {
                _products.value = it
                _multiState.value = MultiStateLayoutState.Content
            }.onFailure {
                _products.value = emptyList()
                _multiState.value = MultiStateLayoutState.Content
            }
        }
    }
}

