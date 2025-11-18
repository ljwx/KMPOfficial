# KMP 网络请求集成指南

本指南将帮助你在 Kotlin Multiplatform 项目中集成 Ktor Client 进行网络请求。

## 📋 前置条件

- ✅ 项目已配置 `kotlinx-serialization-json`（已完成）
- ✅ 项目支持 Android、iOS、JVM、JS、WASM 平台

## 🚀 集成步骤

### 步骤 1：在 `gradle/libs.versions.toml` 中添加 Ktor Client 依赖定义

在 `[libraries]` 部分添加以下内容：

```toml
# Ktor Client 核心库（跨平台）
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }

# 各平台引擎实现
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-ios = { module = "io.ktor:ktor-client-ios", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }  # JVM 平台
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }  # JS 平台
ktor-client-curl = { module = "io.ktor:ktor-client-curl", version.ref = "ktor" }  # WASM 平台（使用 curl）

# 内容协商和序列化支持
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# 日志支持（可选，用于调试）
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
```

**说明：**
- `ktor-client-core`：核心库，所有平台都需要
- 平台引擎：每个平台需要对应的引擎实现
- `ktor-client-content-negotiation`：用于自动序列化/反序列化 JSON
- `ktor-serialization-kotlinx-json`：JSON 序列化支持

### 步骤 2：在 `shared/build.gradle.kts` 中添加依赖

在 `commonMain.dependencies` 中添加：

```kotlin
commonMain.dependencies {
    // ... 现有依赖 ...
    
    // Ktor Client 核心
    implementation(libs.ktor.client.core)
    
    // 内容协商和 JSON 序列化
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // 日志支持（可选）
    implementation(libs.ktor.client.logging)
}
```

在平台特定的源集中添加引擎：

```kotlin
androidMain.dependencies {
    implementation(libs.ktor.client.android)
}

iosMain.dependencies {
    implementation(libs.ktor.client.ios)
}

jvmMain.dependencies {
    implementation(libs.ktor.client.cio)
}

jsMain.dependencies {
    implementation(libs.ktor.client.js)
}

wasmJsMain.dependencies {
    implementation(libs.ktor.client.curl)
}
```

### 步骤 3：创建网络请求代码结构

建议在 `shared/src/commonMain/kotlin/org/example/project/` 下创建以下目录结构：

```
network/
├── api/           # API 接口定义
│   └── ApiService.kt
├── model/         # 数据模型（使用 @Serializable）
│   └── ApiResponse.kt
├── client/        # HTTP 客户端配置
│   └── HttpClientFactory.kt
└── repository/    # 数据仓库层（可选）
    └── DataRepository.kt
```

### 步骤 4：创建 HTTP 客户端工厂

创建 `shared/src/commonMain/kotlin/org/example/project/network/client/HttpClientFactory.kt`：

```kotlin
package org.example.project.network.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(): HttpClient {
        return HttpClient {
            // 安装内容协商插件，自动处理 JSON
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true  // 忽略未知字段
                    isLenient = true         // 宽松模式
                    encodeDefaults = false    // 不编码默认值
                })
            }
            
            // 安装日志插件（可选，用于调试）
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO  // 或 LogLevel.ALL 查看详细信息
            }
            
            // 可以在这里添加其他插件，如：
            // - HttpRequestRetry：重试机制
            // - HttpTimeout：超时配置
            // - HttpRedirect：重定向处理
        }
    }
}
```

### 步骤 5：创建数据模型

创建 `shared/src/commonMain/kotlin/org/example/project/network/model/ApiResponse.kt`：

```kotlin
package org.example.project.network.model

import kotlinx.serialization.Serializable

// 通用 API 响应包装类
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
)

// 示例：产品数据模型
@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val price: Double,
    val description: String? = null
)
```

### 步骤 6：创建 API 服务接口

创建 `shared/src/commonMain/kotlin/org/example/project/network/api/ApiService.kt`：

```kotlin
package org.example.project.network.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.example.project.network.client.HttpClientFactory
import org.example.project.network.model.ApiResponse
import org.example.project.network.model.ProductResponse

class ApiService {
    private val client = HttpClientFactory.create()
    
    // 基础 URL（可以从配置文件读取）
    private val baseUrl = "https://api.example.com"
    
    // 示例：获取产品列表
    suspend fun getProducts(): Result<List<ProductResponse>> {
        return try {
            val response: ApiResponse<List<ProductResponse>> = client.get("$baseUrl/products") {
                // 可以在这里添加请求头、参数等
                headers {
                    append("Accept", "application/json")
                }
            }.body()
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 示例：获取单个产品详情
    suspend fun getProductById(id: String): Result<ProductResponse> {
        return try {
            val response: ApiResponse<ProductResponse> = client.get("$baseUrl/products/$id") {
                headers {
                    append("Accept", "application/json")
                }
            }.body()
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 示例：POST 请求
    suspend fun createProduct(product: ProductResponse): Result<ProductResponse> {
        return try {
            val response: ApiResponse<ProductResponse> = client.post("$baseUrl/products") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(product)
            }.body()
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 记得在不需要时关闭客户端（通常在应用退出时）
    fun close() {
        client.close()
    }
}
```

### 步骤 7：在 Compose 中使用

在 Compose 组件中使用网络请求：

```kotlin
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.example.project.network.api.ApiService

@Composable
fun ProductListScreen() {
    val apiService = remember { ApiService() }
    var products by remember { mutableStateOf<List<ProductResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            apiService.getProducts()
                .onSuccess { 
                    products = it
                    error = null
                }
                .onFailure { 
                    error = it.message
                }
            isLoading = false
        }
    }
    
    // UI 渲染
    if (isLoading) {
        // 显示加载中
    } else if (error != null) {
        // 显示错误
    } else {
        // 显示产品列表
    }
}
```

## 🔧 高级配置

### 添加超时配置

在 `HttpClientFactory.kt` 中：

```kotlin
import io.ktor.client.plugins.*
import io.ktor.client.plugins.timeout.*

HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000  // 30秒
        connectTimeoutMillis = 10000  // 10秒
        socketTimeoutMillis = 30000   // 30秒
    }
    // ... 其他配置
}
```

### 添加请求重试

```kotlin
import io.ktor.client.plugins.*
import io.ktor.client.plugins.retry.*

HttpClient {
    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnTimeout = true
        retryOnSocketTimeout = true
        retryOnException { request, cause ->
            cause is IOException
        }
    }
    // ... 其他配置
}
```

### 添加认证 Token

```kotlin
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

HttpClient {
    install(Auth) {
        bearer {
            loadTokens {
                // 从存储中加载 token
                BearerTokens(accessToken = "your-token", refreshToken = "")
            }
        }
    }
    // ... 其他配置
}
```

## 📝 注意事项

1. **平台权限**：
   - Android：确保 `AndroidManifest.xml` 中有网络权限
   - iOS：确保 `Info.plist` 中配置了网络权限（已完成）

2. **错误处理**：建议统一封装错误处理逻辑

3. **资源管理**：记得在适当时机关闭 HttpClient

4. **线程安全**：Ktor Client 是线程安全的，可以在协程中安全使用

5. **测试**：可以使用 `ktor-client-mock` 进行单元测试

## 🧪 测试建议

创建测试文件验证网络请求：

```kotlin
// shared/src/commonTest/kotlin/org/example/project/network/ApiServiceTest.kt
import kotlin.test.Test
import kotlin.test.assertTrue

class ApiServiceTest {
    @Test
    fun testGetProducts() = runTest {
        val apiService = ApiService()
        val result = apiService.getProducts()
        assertTrue(result.isSuccess || result.isFailure) // 根据实际情况调整
    }
}
```

## 📚 参考资源

- [Ktor Client 官方文档](https://ktor.io/docs/client.html)
- [Kotlinx Serialization 文档](https://github.com/Kotlin/kotlinx.serialization)
- [Ktor Client 示例](https://github.com/ktorio/ktor-samples)


