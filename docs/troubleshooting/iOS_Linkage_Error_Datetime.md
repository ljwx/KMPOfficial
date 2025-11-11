# iOS 链接错误排查：kotlinx-datetime 版本冲突导致的 IrLinkageError

## 问题日期
2025-11-11

## 问题现象

### 运行时崩溃
iOS 应用启动时立即崩溃，报以下错误：

```
Uncaught Kotlin exception: kotlin.internal.IrLinkageError: 
Can not get instance of singleton 'System': 
No class found for symbol 'kotlinx.datetime/Clock.System|null[0]'
```

### 关键堆栈信息
```
at 4   KotlinProject.debug.dylib  kfun:com.jdcr.kmplog.KLog#emit(...)
```

错误发生在 `kmplog` 模块尝试调用 `Clock.System.now()` 时。

## 问题根源

### 核心原因：依赖版本冲突

1. **项目配置的版本**：`libs.versions.toml` 中定义 `kotlinx-datetime = "0.6.0"`
2. **实际使用的版本**：依赖树中传递依赖了 `kotlinx-datetime 0.7.1`
3. **导致的问题**：
   - Gradle 尝试导出 `0.6.0` 版本到 iOS Framework
   - 但构建时实际包含的是 `0.7.1` 版本
   - 运行时找不到 `Clock.System`，因为版本不匹配

### 版本冲突来源

通过构建日志分析，`kotlinx-datetime 0.7.1` 被以下库传递依赖引入：
- `androidx.lifecycle:lifecycle-*:2.9.4`
- `org.jetbrains.androidx.lifecycle:lifecycle-*:2.9.5`
- 其他 Compose Multiplatform 库

这些库的 iOS 版本内部依赖了更新的 `kotlinx-datetime 0.7.1`。

## 诊断过程

### 1. 初步判断
最初的错误信息指向 `Clock.System` 找不到，这通常有两种可能：
- 依赖没有正确包含到 iOS Framework
- 依赖版本不匹配

### 2. 第一轮尝试（失败）
**尝试的方案**：在 iOS Framework 配置中添加 `export(libs.kotlinx.datetime)`

```kotlin
// composeApp/build.gradle.kts
iosTarget.binaries.framework {
    export(projects.kmplog)
    export(libs.kotlinx.datetime)  // ← 添加
}

// kmplog/build.gradle.kts
iosTarget.binaries.framework {
    export(libs.kotlinx.datetime)  // ← 添加
}
```

**结果**：构建成功但运行时仍然崩溃，说明问题不是简单的依赖缺失。

### 3. 关键发现（构建日志分析）
**关键日志片段**：
```
warning: Following libraries are specified to be exported with -Xexport-library, 
but not included to the build:
/Users/.../kotlinx-datetime-iossimulatorarm64/0.6.0/...

Included libraries:
.../kotlinx-datetime-iossimulatorarm64/0.7.1/...
```

**这段警告揭示了核心问题**：
- 配置要求导出 `0.6.0`
- 但实际构建包含的是 `0.7.1`
- **版本不匹配导致运行时链接失败**

### 4. 为什么很难发现？

1. **构建成功**：Gradle 构建没有报错，只有一条容易被忽略的警告
2. **运行时崩溃**：错误信息只说"找不到符号"，没有提示版本问题
3. **隐式传递依赖**：版本升级是由传递依赖悄悄引入的，不在直接配置中
4. **缓存问题**：Xcode 缓存了旧的 Framework，即使修改配置也不生效

## 解决方案

### 最终修复

**1. 统一版本到实际使用的 0.7.1**

```kotlin
// gradle/libs.versions.toml
kotlinx-datetime = "0.7.1"  // 从 0.6.0 升级到 0.7.1
```

**2. 确保依赖正确配置**

```kotlin
// kmplog/build.gradle.kts
sourceSets {
    commonMain {
        dependencies {
            api(libs.kotlinx.datetime)  // 使用 api 而不是 implementation
        }
    }
}

kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "kmplogKit"
            export(libs.kotlinx.datetime)  // 导出到 iOS Framework
        }
    }
}
```

```kotlin
// composeApp/build.gradle.kts
sourceSets {
    commonMain.dependencies {
        api(projects.kmplog)
        api(libs.kotlinx.datetime)  // 必须声明为直接依赖才能 export
    }
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(projects.kmplog)
            export(libs.kotlinx.datetime)  // 必须显式导出传递依赖
        }
    }
}
```

**3. 清理所有缓存**

```bash
# 清理 Gradle 构建产物
rm -rf composeApp/build
rm -rf kmplog/build
rm -rf shared/build

# 清理 Xcode 缓存（在 Xcode 中）
# Cmd + Shift + K (Clean Build Folder)

# 可选：删除 Xcode DerivedData
rm -rf ~/Library/Developer/Xcode/DerivedData
```

## 关键经验教训

### 1. 如何识别版本冲突

**看 Gradle 构建日志中的警告**：
```
warning: Following libraries are specified to be exported with -Xexport-library, 
but not included to the build:
```

这个警告表明：
- 你尝试导出的库版本 ≠ 实际包含的库版本
- 需要检查 `Included libraries` 列表，找出实际版本

### 2. 依赖配置规则

在 KMP iOS Framework 中要导出一个库，必须同时满足：

1. **在模块的 `commonMain`（或 `iosMain`）中声明为 `api` 依赖**
   ```kotlin
   sourceSets {
       commonMain.dependencies {
           api(libs.some.library)  // 不是 implementation
       }
   }
   ```

2. **在 iOS binaries 配置中使用 `export()`**
   ```kotlin
   iosTarget.binaries.framework {
       export(libs.some.library)
   }
   ```

3. **如果导出的模块有传递依赖，传递依赖也必须显式导出**
   ```kotlin
   iosTarget.binaries.framework {
       export(projects.myModule)
       export(libs.myModuleDependency)  // 不会自动传递！
   }
   ```

### 3. 版本冲突的诊断方法

**方法 1：查看 Gradle 构建日志**
```bash
# 在 Xcode 构建时，展开 "Compile Kotlin Framework" 步骤
# 查找 "warning: Following libraries..." 和 "Included libraries:"
```

**方法 2：运行依赖树命令（需要 Java 环境）**
```bash
./gradlew :composeApp:dependencies --configuration iosSimulatorArm64CompileKlibraries | grep kotlinx-datetime
```

**方法 3：检查 Gradle 缓存目录**
```bash
# 查看实际下载的版本
ls ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-datetime-*/
```

### 4. 清理缓存的重要性

版本冲突修复后，必须彻底清理缓存：

```bash
# Gradle 缓存
rm -rf build/
rm -rf */build/

# Xcode 缓存（最重要！）
# 在 Xcode 中：Product → Clean Build Folder (Cmd + Shift + K)

# Xcode DerivedData（强烈推荐）
rm -rf ~/Library/Developer/Xcode/DerivedData
```

**为什么必须清理 Xcode 缓存？**
- Xcode 会缓存编译好的 Framework
- 即使 Gradle 重新生成了 Framework，Xcode 可能还在用旧的
- 必须删除 DerivedData 才能强制重新加载

## 如何避免类似问题

### 1. 定期检查依赖版本

使用 Gradle 的版本目录管理依赖时，注意：
```kotlin
// ❌ 错误：可能被传递依赖覆盖
api(libs.kotlinx.datetime)  // 版本在 libs.versions.toml 中定义

// ✅ 正确：强制使用特定版本
api(libs.kotlinx.datetime) {
    // 如果需要强制版本
    version {
        strictly("0.7.1")
    }
}
```

### 2. 关注 Gradle 构建警告

不要忽略构建日志中的 `warning`，特别是：
- `Following libraries are specified to be exported...`
- `Cannot infer a bundle ID...`
- `Function ... can not be called...`

### 3. 版本管理最佳实践

**在 `libs.versions.toml` 中**：
```toml
[versions]
# 相关库使用相同的大版本
kotlinx-datetime = "0.7.1"
kotlinx-coroutines = "1.10.2"
androidx-lifecycle = "2.9.5"
```

**检查依赖冲突**：
```bash
# 查看某个模块的依赖树
./gradlew :composeApp:dependencies --configuration iosSimulatorArm64Api

# 查看版本冲突
./gradlew :composeApp:dependencyInsight --configuration iosSimulatorArm64Api --dependency kotlinx-datetime
```

### 4. iOS Framework 导出检查清单

当遇到 `No class found for symbol` 错误时，依次检查：

- [ ] 依赖是否在 `commonMain` 中声明为 `api`？
- [ ] 依赖是否在 `iosTarget.binaries.framework` 中 `export()`？
- [ ] 配置的版本是否与实际构建版本一致？
- [ ] 传递依赖是否也显式导出了？
- [ ] 是否清理了 Xcode DerivedData？

### 5. 实验性 API 处理

如果升级后遇到实验性 API 警告：

```kotlin
// 在使用实验性 API 的文件顶部添加
@file:OptIn(ExperimentalDatetimeApi::class)

// 或在 build.gradle.kts 中全局启用
kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlinx.datetime.ExperimentalDatetimeApi")
    }
}
```

## 总结

这个问题的隐蔽性在于：
1. **编译通过**但**运行时崩溃**，中间隔着一层
2. **警告信息**很容易被忽略，buried 在大量构建日志中
3. **版本冲突**是由**传递依赖**隐式引入的
4. **Xcode 缓存**掩盖了修改效果，造成"改了没用"的假象

**关键诊断线索**：
```
warning: Following libraries are specified to be exported with -Xexport-library, 
but not included to the build:
/Users/.../kotlinx-datetime-iossimulatorarm64/0.6.0/...

Included libraries:
.../kotlinx-datetime-iossimulatorarm64/0.7.1/...
```

这段警告直接指出了版本不匹配的事实！

**未来遇到类似问题的排查步骤**：
1. ✅ 查看完整的 Xcode 构建日志（展开 "Compile Kotlin Framework"）
2. ✅ 查找 `warning:` 和 `note:` 消息
3. ✅ 对比 "specified to be exported" 和 "Included libraries" 的版本
4. ✅ 统一版本后，彻底清理缓存（Gradle + Xcode）
5. ✅ 重新构建并运行

## 相关文档
- [KMP Version Compatibility](../KMP_Version_Compatibility.md)
- [iOS Troubleshooting Guide](KMP_iOS_Troubleshooting_Guide.md)

