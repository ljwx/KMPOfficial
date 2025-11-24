# Compose Navigation 数据传递方案详解

## 场景分析

你的问题很典型：**列表页已经有数据了，详情页不想重新加载**。

有以下几种解决方案：

---

## 方案 1：自定义 NavType（不推荐，太复杂）

### 实现步骤

```kotlin
// 1. 定义自定义 NavType
object ProductSummaryDataNavType : NavType<ProductSummaryData>(isNullableAllowed = false) {
    override fun put(bundle: Bundle, key: String, value: ProductSummaryData) {
        bundle.putString(key, Json.encodeToString(value))
    }

    override fun get(bundle: Bundle, key: String): ProductSummaryData {
        return Json.decodeFromString(bundle.getString(key)!!)
    }

    override fun parseValue(value: String): ProductSummaryData {
        return Json.decodeFromString(Uri.decode(value))
    }

    override fun serializeAsValue(value: ProductSummaryData): String {
        return Uri.encode(Json.encodeToString(value))
    }
}

// 2. 注册到 NavController
val navController = rememberNavController()
navController.navigatorProvider.addNavigator(
    ComposeNavigator().apply {
        // 注册自定义类型
    }
)

// 3. 在路由中指定类型映射
composable<RouterProductDetail>(
    typeMap = mapOf(typeOf<ProductSummaryData>() to ProductSummaryDataNavType)
) { ... }
```

### 缺点
- ❌ 代码量大，维护成本高
- ❌ 仍然需要序列化/反序列化
- ❌ URL 会很长（不适合 Deep Link）
- ❌ 每个自定义类型都要写一遍

---

## 方案 2：SavedStateHandle 缓存（✅ 推荐）

这是 Google 官方推荐的方案，利用 `SavedStateHandle` 在导航时传递数据。

### 实现代码

```kotlin
// 1. 在 HomeViewModel 中添加方法
class ProductViewModel(private val repository: IProductRepository) : ViewModel() {
    // ... 现有代码 ...
    
    // 缓存选中的商品
    fun cacheSelectedProduct(product: ProductSummaryData) {
        // 使用 SavedStateHandle 或全局缓存
        ProductCache.selectedProduct = product
    }
}

// 2. 简单的内存缓存（单例）
object ProductCache {
    var selectedProduct: ProductSummaryData? = null
}

// 3. 导航时缓存数据
navController.navigate(RouterProductDetail(product.id)) {
    // 在导航前缓存
    viewModel.cacheSelectedProduct(product)
}

// 4. 详情页 ViewModel 从缓存读取
class ProductDetailViewModel(
    private val productId: Int,
    private val repository: IProductRepository
) : ViewModel() {
    
    val product: StateFlow<ProductSummaryData?> = flow {
        // 优先从缓存读取
        val cached = ProductCache.selectedProduct
        if (cached != null && cached.id == productId) {
            emit(cached)
        } else {
            // 缓存未命中，重新加载
            val loaded = repository.getProductById(productId)
            emit(loaded)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
```

### 优点
- ✅ 简单直观
- ✅ 避免重复网络请求
- ✅ URL 仍然简洁（只传 ID）
- ✅ 支持配置变更（屏幕旋转等）

---

## 方案 3：共享 ViewModel（适合父子关系）

如果列表页和详情页是父子关系（如 Master-Detail 布局），可以共享 ViewModel。

### 实现代码

```kotlin
// 1. 在父级作用域创建 ViewModel
@Composable
fun ProductListDetailContainer() {
    // 在父级创建 ViewModel
    val sharedViewModel: ProductViewModel = koinViewModel()
    
    NavHost(...) {
        composable<RouterProductList> {
            ProductListScreen(viewModel = sharedViewModel)
        }
        
        composable<RouterProductDetail> {
            // 共享同一个 ViewModel
            ProductDetailScreen(viewModel = sharedViewModel)
        }
    }
}

// 2. DetailScreen 从共享 ViewModel 获取数据
@Composable
fun ProductDetailScreen(
    productId: Int,
    viewModel: ProductViewModel // 共享的 ViewModel
) {
    val product = viewModel.productList.collectAsState()
        .value.find { it.id == productId }
    
    // 使用 product ...
}
```

### 优点
- ✅ 数据自动共享
- ✅ 无需缓存逻辑
- ✅ 状态一致性好

### 缺点
- ❌ 只适合父子页面
- ❌ ViewModel 生命周期管理复杂

---

## 方案 4：Repository 层缓存（最佳实践）

在 Repository 层实现智能缓存，对 ViewModel 透明。

### 实现代码

```kotlin
class ProductRepository : IProductRepository {
    // 内存缓存
    private val cache = mutableMapOf<Int, ProductSummaryData>()
    
    override suspend fun getProductList(): Result<BaseApiResponse<List<ProductSummaryData>>> {
        return apiService.getProductList().onSuccess { response ->
            // 缓存所有商品
            response.data?.forEach { product ->
                cache[product.id] = product
            }
        }
    }
    
    override suspend fun getProductById(id: Int): Result<ProductSummaryData> {
        // 优先从缓存读取
        cache[id]?.let { return Result.success(it) }
        
        // 缓存未命中，从网络加载
        return apiService.getProductDetail(id).map { response ->
            response.data!!.also { cache[it.id] = it }
        }
    }
}

// DetailViewModel 只需要简单调用
class ProductDetailViewModel(
    private val productId: Int,
    private val repository: IProductRepository
) : ViewModel() {
    
    val product = flow {
        // Repository 会自动处理缓存
        repository.getProductById(productId).onSuccess {
            emit(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
```

### 优点
- ✅ 关注点分离（缓存逻辑在 Repository）
- ✅ ViewModel 代码简洁
- ✅ 可以配置缓存策略（过期时间等）
- ✅ 适合大型项目

---

## 推荐方案总结

| 场景 | 推荐方案 |
|------|---------|
| **简单项目** | 方案 2（SavedStateHandle 缓存） |
| **中大型项目** | 方案 4（Repository 层缓存） |
| **父子页面** | 方案 3（共享 ViewModel） |
| **必须传递对象** | 方案 1（自定义 NavType，不推荐） |

## 我的建议

对于你的项目，我推荐**方案 4（Repository 层缓存）**，因为：

1. **架构清晰**：缓存逻辑属于数据层，不应该在 UI 层处理
2. **易于测试**：Repository 可以独立测试缓存逻辑
3. **可扩展**：未来可以轻松添加磁盘缓存、过期策略等
4. **对 ViewModel 透明**：ViewModel 不需要关心数据来自缓存还是网络

要不要我帮你实现 Repository 层的缓存方案？
