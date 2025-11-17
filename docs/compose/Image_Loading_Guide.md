# Compose 图片加载指南

本文档介绍在 Compose Multiplatform 中如何显示各种类型的图片，包括本地图片、网络图片和 SVG 图片。

## 目录

1. [本地图片加载](#本地图片加载)
2. [网络图片加载](#网络图片加载)
3. [SVG 图片加载](#svg-图片加载)
4. [图片显示模式（ContentScale）](#图片显示模式contentscale)
5. [图片加载最佳实践](#图片加载最佳实践)
6. [常见问题](#常见问题)

---

## 本地图片加载

### 1. 使用 `painterResource` 加载资源图片

Compose Multiplatform 提供了 `painterResource` 函数来加载放在 `composeResources` 目录下的图片资源。

#### 步骤 1: 添加图片资源

将图片文件放在 `composeApp/src/commonMain/composeResources/drawable/` 目录下：

```
composeApp/src/commonMain/composeResources/
  └── drawable/
      ├── logo.png
      ├── icon.jpg
      └── background.png
```

#### 步骤 2: 在代码中使用

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.logo

@Composable
fun LocalImageExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = "应用 Logo",
        modifier = Modifier.size(100.dp)
    )
}
```

#### 支持的图片格式

- PNG（推荐，支持透明背景）
- JPG/JPEG
- WebP（Android 和部分平台支持）

### 2. 加载 Vector Drawable（Android 平台）

对于 Android 平台，可以使用 `painterResource` 加载 Vector Drawable：

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.ic_arrow_back

@Composable
fun VectorDrawableExample() {
    Image(
        painter = painterResource(Res.drawable.ic_arrow_back),
        contentDescription = "返回图标",
        modifier = Modifier.size(24.dp)
    )
}
```

---

## 网络图片加载

### 方法 1: 使用 Ktor 手动加载（基础方案）

如果你的项目已经集成了 Ktor Client，可以手动下载图片并转换为 `ImageBitmap`：

#### 步骤 1: 创建图片加载工具类

```kotlin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat

class ImageLoader(private val httpClient: HttpClient) {
    suspend fun loadImageBitmap(url: String): ImageBitmap? {
        return try {
            val response: HttpResponse = httpClient.get(url)
            val bytes = response.body<ByteArray>()
            
            // 使用 Skia 解码图片
            val image = Image.makeFromEncoded(bytes)
            val bitmap = Bitmap.makeFromImage(image)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
```

#### 步骤 2: 在 Compose 中使用

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun NetworkImageExample(imageUrl: String) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val imageLoader = remember { ImageLoader(httpClient) }
    
    LaunchedEffect(imageUrl) {
        imageBitmap = imageLoader.loadImageBitmap(imageUrl)
    }
    
    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "网络图片",
            modifier = Modifier.size(200.dp)
        )
    } ?: run {
        // 加载中或加载失败的占位符
        Text("加载中...")
    }
}
```

### 方法 2: 使用 Kamel 图片加载库（推荐）

Kamel 是一个专为 Compose Multiplatform 设计的图片加载库，支持网络图片、缓存和多种格式。

#### 步骤 1: 添加依赖

在 `composeApp/build.gradle.kts` 的 `commonMain.dependencies` 中添加：

```kotlin
commonMain.dependencies {
    // ... 其他依赖
    implementation("io.kamel:kamel-image:0.9.0")
}
```

#### 步骤 2: 使用 AsyncImage 组件

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun NetworkImageWithKamel(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = "网络图片",
        modifier = Modifier.size(200.dp),
        onLoading = {
            CircularProgressIndicator()
        },
        onFailure = {
            Text("加载失败")
        }
    )
}
```

#### 步骤 3: 配置图片加载器（可选）

```kotlin
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.imageLoaderConfig
import io.kamel.image.KamelImage

val kamelConfig = KamelConfig {
    imageLoaderConfig {
        // 配置缓存大小
        maxCacheSize = 50 * 1024 * 1024 // 50MB
        // 配置超时时间
        requestTimeoutMillis = 30_000
    }
}

@Composable
fun App() {
    CompositionLocalProvider(LocalKamelConfig provides kamelConfig) {
        // 你的应用内容
    }
}
```

---

## SVG 图片加载

### 方法 1: 使用 Kamel 加载 SVG（推荐）

Kamel 内置支持 SVG 格式，无需额外配置：

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun SvgImageExample(svgUrl: String) {
    KamelImage(
        resource = asyncPainterResource(svgUrl),
        contentDescription = "SVG 图片",
        modifier = Modifier.size(200.dp)
    )
}
```

### 方法 2: 使用 Compose SVG 库

如果需要更高级的 SVG 功能，可以使用专门的 SVG 库：

#### 步骤 1: 添加依赖

```kotlin
commonMain.dependencies {
    implementation("org.jetbrains.compose.svg:compose-svg:1.0.0")
}
```

#### 步骤 2: 加载 SVG

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.svg.Svg
import org.jetbrains.compose.svg.rememberSvgPainter

@Composable
fun SvgImageExample(svgUrl: String) {
    val painter = rememberSvgPainter(svgUrl)
    
    Svg(
        painter = painter,
        contentDescription = "SVG 图片",
        modifier = Modifier.size(200.dp)
    )
}
```

### 方法 3: 加载本地 SVG 资源

如果 SVG 文件是本地资源，可以将其放在 `composeResources` 目录下：

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.icon_svg

@Composable
fun LocalSvgExample() {
    Image(
        painter = painterResource(Res.drawable.icon_svg),
        contentDescription = "本地 SVG",
        modifier = Modifier.size(100.dp)
    )
}
```

---

## 图片显示模式（ContentScale）

在 Compose 中，`ContentScale` 用于控制图片如何缩放以适应其容器，类似于 Android View 中的 `ImageView.ScaleType`。

### ContentScale 类型详解

#### 1. ContentScale.Fit（对应 Android: `fitCenter`）

**说明**：等比例缩放图片，确保完整显示在容器内，可能会留有空白区域。

**特点**：
- 保持图片宽高比
- 图片完整显示，不会被裁剪
- 如果图片和容器比例不一致，会有空白边

**适用场景**：需要完整显示图片内容，不介意有空白边的情况。

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.logo

@Composable
fun FitScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}
```

#### 2. ContentScale.Crop（对应 Android: `centerCrop`）

**说明**：等比例缩放图片，填充整个容器，超出部分会被裁剪。

**特点**：
- 保持图片宽高比
- 图片会填充整个容器
- 超出容器的部分会被裁剪
- 默认居中对齐

**适用场景**：需要填充整个容器，不介意裁剪部分内容的情况（如背景图、头像等）。

```kotlin
@Composable
fun CropScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
```

#### 3. ContentScale.FillBounds（对应 Android: `fitXY`）

**说明**：拉伸图片以填充整个容器，不保持宽高比。

**特点**：
- 不保持图片宽高比
- 图片会被拉伸或压缩以填充容器
- 可能导致图片变形

**适用场景**：需要完全填充容器，且图片变形可接受的情况（较少使用）。

```kotlin
@Composable
fun FillBoundsScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}
```

#### 4. ContentScale.FillWidth（对应 Android: 无直接对应）

**说明**：等比例缩放图片以填充容器宽度，高度按比例调整。

**特点**：
- 保持图片宽高比
- 宽度填充容器
- 高度按比例调整，可能超出或不足容器高度

**适用场景**：需要宽度填满，高度可变化的情况。

```kotlin
@Composable
fun FillWidthScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}
```

#### 5. ContentScale.FillHeight（对应 Android: 无直接对应）

**说明**：等比例缩放图片以填充容器高度，宽度按比例调整。

**特点**：
- 保持图片宽高比
- 高度填充容器
- 宽度按比例调整，可能超出或不足容器宽度

**适用场景**：需要高度填满，宽度可变化的情况。

```kotlin
@Composable
fun FillHeightScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxHeight(),
        contentScale = ContentScale.FillHeight
    )
}
```

#### 6. ContentScale.Inside（对应 Android: `centerInside`）

**说明**：等比例缩放图片，确保图片完全在容器内，如果图片小于容器则保持原大小。

**特点**：
- 保持图片宽高比
- 图片完整显示，不会被裁剪
- 如果图片小于容器，不会放大
- 如果图片大于容器，会缩小以适应

**适用场景**：需要完整显示图片，且不希望小图片被放大的情况。

```kotlin
@Composable
fun InsideScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Inside
    )
}
```

#### 7. ContentScale.None（对应 Android: `matrix`）

**说明**：不进行任何缩放，按图片原始大小显示。

**特点**：
- 不缩放图片
- 按原始像素大小显示
- 可能超出或小于容器

**适用场景**：需要按原始大小显示图片的情况（较少使用）。

```kotlin
@Composable
fun NoneScaleExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.None
    )
}
```

### ContentScale 与 Android ScaleType 对比表

| Compose ContentScale | Android ScaleType | 说明 |
|---------------------|-------------------|------|
| `ContentScale.Fit` | `fitCenter` | 等比例缩放，完整显示，可能有空白 |
| `ContentScale.Crop` | `centerCrop` | 等比例缩放，填充容器，会裁剪 |
| `ContentScale.FillBounds` | `fitXY` | 拉伸填充，不保持比例 |
| `ContentScale.FillWidth` | - | 填充宽度，高度按比例 |
| `ContentScale.FillHeight` | - | 填充高度，宽度按比例 |
| `ContentScale.Inside` | `centerInside` | 等比例缩放，完整显示，不放大 |
| `ContentScale.None` | `matrix` | 不缩放，原始大小 |

### 自定义对齐方式

`ContentScale` 只控制缩放方式，对齐方式通过 `Alignment` 参数控制：

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
fun AlignedImageExample() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopStart // 顶部左对齐
        // 其他对齐选项：
        // Alignment.TopCenter - 顶部居中
        // Alignment.TopEnd - 顶部右对齐
        // Alignment.CenterStart - 中间左对齐
        // Alignment.Center - 居中（默认）
        // Alignment.CenterEnd - 中间右对齐
        // Alignment.BottomStart - 底部左对齐
        // Alignment.BottomCenter - 底部居中
        // Alignment.BottomEnd - 底部右对齐
    )
}
```

### 实际应用示例

#### 示例 1: 头像图片（圆形裁剪）

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun AvatarImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = "用户头像",
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop, // 裁剪填充，确保圆形
        alignment = Alignment.Center
    )
}
```

#### 示例 2: 商品图片（保持比例）

```kotlin
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ProductImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = "商品图片",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // 1:1 比例
        contentScale = ContentScale.Fit, // 完整显示，不裁剪
        alignment = Alignment.Center
    )
}
```

#### 示例 3: 背景图片（填充容器）

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun BackgroundImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop, // 填充整个背景
        alignment = Alignment.Center
    )
}
```

#### 示例 4: Banner 图片（填充宽度）

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun BannerImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = "Banner 图片",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth, // 填充宽度
        alignment = Alignment.Center
    )
}
```

### 选择建议

- **头像、缩略图**：使用 `ContentScale.Crop` + `CircleShape` 或 `RoundedCornerShape`
- **商品图片、详情图**：使用 `ContentScale.Fit` 或 `ContentScale.Inside` 确保完整显示
- **背景图片**：使用 `ContentScale.Crop` 填充整个容器
- **Banner、轮播图**：使用 `ContentScale.FillWidth` 或 `ContentScale.Crop`
- **图标、小图片**：使用 `ContentScale.Fit` 或 `ContentScale.Inside` 保持清晰度

---

## 图片加载最佳实践

### 1. 添加占位符和错误处理

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ImageWithPlaceholder(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = null,
        modifier = Modifier.size(200.dp),
        onLoading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        onFailure = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("加载失败")
            }
        }
    )
}
```

### 2. 图片尺寸优化

```kotlin
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier

@Composable
fun OptimizedImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f) // 保持 16:9 比例
    )
}
```

### 3. 图片裁剪和缩放

图片的缩放模式请参考 [图片显示模式（ContentScale）](#图片显示模式contentscale) 章节，这里提供一个简单示例：

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
fun ScaledImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop, // 裁剪填充
        alignment = Alignment.Center
    )
}
```

### 4. 圆形图片

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun CircularImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = "头像",
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
    )
}
```

### 5. 图片缓存管理

使用 Kamel 时，图片会自动缓存。如果需要手动清理缓存：

```kotlin
import io.kamel.core.cache.CachePolicy
import io.kamel.image.asyncPainterResource

@Composable
fun CachedImage(imageUrl: String) {
    KamelImage(
        resource = asyncPainterResource(
            data = imageUrl,
            cachePolicy = CachePolicy.Enabled // 启用缓存
        ),
        contentDescription = null
    )
}
```

---

## 常见问题

### Q1: 为什么我的图片不显示？

**可能原因：**
1. 图片路径错误
2. 网络权限未配置（iOS 需要在 Info.plist 中配置）
3. 图片格式不支持

**解决方案：**
- 检查图片路径是否正确
- 确保网络权限已配置
- 使用支持的图片格式（PNG、JPG、WebP、SVG）

### Q2: 如何在 iOS 上加载网络图片？

iOS 需要在 `Info.plist` 中配置网络权限：

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

### Q3: 图片加载很慢怎么办？

**优化建议：**
1. 使用图片缓存（Kamel 自动支持）
2. 压缩图片大小
3. 使用 CDN 加速
4. 添加占位符提升用户体验

### Q4: 如何加载 Base64 图片？

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.unit.dp
import kotlinx.io.encoding.Base64
import kotlinx.io.encoding.decodeBase64Bytes

@Composable
fun Base64Image(base64String: String) {
    val imageBytes = Base64.decodeBase64Bytes(base64String)
    val image = Image.makeFromEncoded(imageBytes)
    val bitmap = Bitmap.makeFromImage(image)
    
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(200.dp)
    )
}
```

### Q5: 如何预加载图片？

```kotlin
import androidx.compose.runtime.LaunchedEffect
import io.kamel.image.asyncPainterResource

@Composable
fun PreloadImage(imageUrl: String) {
    LaunchedEffect(imageUrl) {
        // 预加载图片到缓存
        asyncPainterResource(imageUrl)
    }
    
    // 使用图片
    KamelImage(
        resource = asyncPainterResource(imageUrl),
        contentDescription = null
    )
}
```

---

## 总结

- **本地图片**：使用 `painterResource` 加载 `composeResources` 目录下的图片
- **网络图片**：推荐使用 Kamel 库，简单易用且支持缓存
- **SVG 图片**：Kamel 内置支持，也可以使用专门的 SVG 库
- **最佳实践**：添加占位符、错误处理、合理使用缓存和图片尺寸优化

选择合适的方案根据你的具体需求：
- 如果只需要加载少量网络图片，可以使用 Ktor 手动实现
- 如果需要完整的图片加载功能（缓存、占位符等），推荐使用 Kamel
- 如果需要高级 SVG 功能，可以使用专门的 SVG 库

