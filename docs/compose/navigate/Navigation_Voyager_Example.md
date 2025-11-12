# Voyager 导航使用示例

Voyager 是一个更简洁的 Compose 导航库，相比 Decompose 使用起来更简单直观。

## 1. 添加依赖

在 `composeApp/build.gradle.kts` 的 `commonMain.dependencies` 中添加：

```kotlin
commonMain.dependencies {
    // Voyager 核心
    implementation("cafe.adriel.voyager:voyager-navigator:1.1.0")
    // Screen 动画
    implementation("cafe.adriel.voyager:voyager-transitions:1.1.0")
    // 底部导航栏支持
    implementation("cafe.adriel.voyager:voyager-tab-navigator:1.1.0")
    // 底部导航栏 Material 3 样式
    implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:1.1.0")
}
```

## 2. 定义 Screen

### 基础 Screen（无参数）

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("首页")
            
            // 导航到其他页面
            Button(onClick = { 
                navigator.push(ProductDetailScreen(productId = "123"))
            }) {
                Text("查看商品详情")
            }
        }
    }
}
```

### 带参数的 Screen

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// 方式1：使用 data class（推荐，类型安全）
data class ProductDetailScreen(
    val productId: String,
    val productName: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Text("商品ID: $productId")
                productName?.let { Text("商品名称: $it") }
                
                Button(onClick = { 
                    navigator.pop() // 返回上一页
                }) {
                    Text("返回")
                }
                
                Button(onClick = { 
                    navigator.pop(Result("购买成功")) // 返回并传递结果
                }) {
                    Text("购买")
                }
            }
        }
    }
}

// 方式2：使用 ScreenKey（适合简单参数）
object ProductDetailScreenKey : ScreenKey {
    override val key: String = "product_detail"
}

class ProductDetailScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenKey = navigator.lastItem.key as? ProductDetailScreenKey
        
        // 从 ScreenKey 获取参数（需要自己解析）
        // ...
    }
}
```

### 带返回结果的 Screen

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class SelectProductScreen(
    val onProductSelected: (String) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Text("选择商品")
                
                Button(onClick = { 
                    onProductSelected("商品A")
                    navigator.pop()
                }) {
                    Text("选择商品A")
                }
                
                Button(onClick = { 
                    onProductSelected("商品B")
                    navigator.pop()
                }) {
                    Text("选择商品B")
                }
            }
        }
    }
}

// 使用方式
@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.currentOrThrow
    
    Button(onClick = { 
        navigator.push(
            SelectProductScreen { selectedProduct ->
                // 处理选择结果
                println("选择了: $selectedProduct")
            }
        )
    }) {
        Text("选择商品")
    }
}
```

## 3. 设置导航器

### 基础用法

```kotlin
package org.example.project.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import org.example.project.routes.HomeScreen

@Composable
fun AppRoot() {
    Navigator(HomeScreen) { navigator ->
        // 这里可以添加全局 UI，比如导航栏、状态栏等
        navigator.saveableState("app_navigation") // 保存导航状态
    }
}
```

### 带动画的导航器

```kotlin
package org.example.project.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.example.project.routes.HomeScreen

@Composable
fun AppRoot() {
    Navigator(
        screen = HomeScreen,
        onBackPressed = { navigator ->
            if (!navigator.canPop) {
                // 处理返回键，比如退出应用
                false
            } else {
                navigator.pop()
                true
            }
        }
    ) { navigator ->
        SlideTransition(navigator)
    }
}
```

### 自定义动画

```kotlin
import cafe.adriel.voyager.transitions.SlideTransition
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.ScaleTransition

// 使用内置动画
FadeTransition(navigator)
ScaleTransition(navigator)
SlideTransition(navigator)

// 自定义动画
@Composable
fun CustomTransition(navigator: Navigator) {
    val transition = rememberScreenTransition(navigator)
    
    transition.screen?.Content()
}
```

## 4. 导航操作

### 基本导航

```kotlin
val navigator = LocalNavigator.currentOrThrow

// 推入新页面
navigator.push(ProductDetailScreen("123"))

// 返回上一页
navigator.pop()

// 返回并传递结果
navigator.pop("结果数据")

// 替换当前页面
navigator.replace(NewScreen())

// 替换整个栈（清除所有页面）
navigator.replaceAll(HomeScreen())

// 检查是否可以返回
if (navigator.canPop) {
    navigator.pop()
}

// 获取当前页面
val currentScreen = navigator.lastItem

// 获取页面栈大小
val stackSize = navigator.size
```

### 带结果的导航

```kotlin
// 方式1：使用回调（推荐）
navigator.push(
    SelectProductScreen { selectedProduct ->
        // 处理结果
        println("选择了: $selectedProduct")
    }
)

// 方式2：使用 pop 返回值
navigator.push(SelectProductScreen())
// 在 SelectProductScreen 中调用 navigator.pop(result)
```

## 5. 完整示例

### 主应用入口

```kotlin
package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.example.project.routes.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Navigator(HomeScreen) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
```

### 首页 Screen

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "首页",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    navigator.push(ProductDetailScreen(productId = "123"))
                }
            ) {
                Text("查看商品详情")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    navigator.push(
                        SelectProductScreen { selectedProduct ->
                            // 处理选择结果
                            println("选择了: $selectedProduct")
                        }
                    )
                }
            ) {
                Text("选择商品")
            }
        }
    }
}
```

### 商品详情 Screen

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class ProductDetailScreen(
    val productId: String,
    val productName: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("商品详情") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "商品ID: $productId",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                productName?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "商品名称: $it",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        navigator.pop("购买成功")
                    }
                ) {
                    Text("购买")
                }
            }
        }
    }
}
```

### 选择商品 Screen

```kotlin
package org.example.project.routes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class SelectProductScreen(
    val onProductSelected: (String) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("选择商品") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "请选择商品",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        onProductSelected("商品A")
                        navigator.pop()
                    }
                ) {
                    Text("选择商品A")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        onProductSelected("商品B")
                        navigator.pop()
                    }
                ) {
                    Text("选择商品B")
                }
            }
        }
    }
}
```

## 6. 优势对比

### Voyager vs Decompose

| 特性 | Voyager | Decompose |
|------|---------|-----------|
| **代码量** | 简洁，Screen 只需实现接口 | 需要 ComponentContext、路由注册等 |
| **参数传递** | 直接使用 data class 构造函数 | 需要序列化/反序列化 |
| **返回结果** | 回调函数或 pop(result) | 需要 requestId 和回调管理 |
| **学习曲线** | 简单直观 | 需要理解 Component 概念 |
| **状态保存** | 自动保存（saveableState） | 需要手动配置序列化 |
| **多平台支持** | ✅ 支持 | ✅ 支持 |
| **启动模式** | 需要手动实现 | 内置支持 |

### Voyager 的优势

1. **更简洁**：Screen 就是普通的 data class 或 object，不需要复杂的配置
2. **类型安全**：参数通过构造函数传递，编译时检查
3. **易于理解**：符合 Compose 的声明式思维
4. **自动状态保存**：使用 `saveableState` 可以自动保存导航状态
5. **灵活的结果处理**：可以使用回调函数，更直观

### Decompose 的优势

1. **启动模式**：内置 SINGLE_TOP、SINGLE_TASK 等模式
2. **生命周期管理**：更细粒度的生命周期控制
3. **组件化**：适合大型应用的组件化架构

## 7. 迁移建议

如果要从 Decompose 迁移到 Voyager：

1. **替换依赖**：移除 Decompose，添加 Voyager
2. **简化 Screen**：将 `ScreenRouteHandler` 改为 `Screen`
3. **简化导航**：使用 `navigator.push()` 替代复杂的路由系统
4. **参数传递**：使用 data class 构造函数替代序列化
5. **结果处理**：使用回调函数替代 requestId 机制

Voyager 更适合中小型应用，代码更简洁易维护。

