## 简化跨平台日志方案

### 设计目标
- 保留核心能力：日志级别、懒加载消息、多通道输出、异常记录。
- 结构尽量简单，便于后续在项目里直接复用或二次扩展。
- 通过 `expect/actual` 抽象平台差异，公共代码保持整洁。

### 核心结构
- `LogLevel`：定义 `DEBUG/INFO/WARN/ERROR` 优先级。
- `LogRecord`：描述单条日志（时间戳、级别、Tag、内容、异常、上下文）。
- `LogSink`：日志的落地方（控制台、文件、CrashReporter 等）。
- `LogConfig`：全局配置（最小级别、默认 Sink 列表、默认上下文）。
- `LogManager`：完成级别过滤、记录构建、将日志广播给所有 Sink。
- `Logger`：业务层入口，支持懒执行 Lambda 和可选上下文。
- `ConsoleLogSink`：默认 Sink，调用平台原生日志。
- `TimeProvider` / `ConsoleBridge`：`expect/actual` 跨平台实现。

### 公共模块代码（`commonMain`）

```kotlin
package logging

enum class LogLevel(val priority: Int) {
    DEBUG(10), INFO(20), WARN(30), ERROR(40)
}

data class LogRecord(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val context: Map<String, Any?> = emptyMap()
)

fun interface LogSink {
    fun write(record: LogRecord)
}

data class LogConfig(
    val minLevel: LogLevel = LogLevel.INFO,
    val sinks: List<LogSink> = listOf(ConsoleLogSink),
    val defaultContext: Map<String, Any?> = emptyMap()
)

object LogManager {
    @Volatile private var config: LogConfig = LogConfig()
    private val timeProvider: TimeProvider = TimeProvider()

    fun install(config: LogConfig) {
        this.config = config
    }

    internal fun emit(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        context: Map<String, Any?>
    ) {
        val cfg = config
        if (level.priority < cfg.minLevel.priority) return

        val record = LogRecord(
            timestampMillis = timeProvider.nowMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            context = if (cfg.defaultContext.isEmpty()) context else cfg.defaultContext + context
        )
        cfg.sinks.forEach { sink ->
            runCatching { sink.write(record) }
        }
    }
}

class Logger internal constructor(private val tag: String) {
    inline fun d(ctx: Map<String, Any?> = emptyMap(), message: () -> String) =
        LogManager.emit(LogLevel.DEBUG, tag, message(), null, ctx)

    inline fun i(ctx: Map<String, Any?> = emptyMap(), message: () -> String) =
        LogManager.emit(LogLevel.INFO, tag, message(), null, ctx)

    inline fun w(
        ctx: Map<String, Any?> = emptyMap(),
        throwable: Throwable? = null,
        message: () -> String
    ) = LogManager.emit(LogLevel.WARN, tag, message(), throwable, ctx)

    inline fun e(
        ctx: Map<String, Any?> = emptyMap(),
        throwable: Throwable? = null,
        message: () -> String
    ) = LogManager.emit(LogLevel.ERROR, tag, message(), throwable, ctx)
}

fun logger(tag: String) = Logger(tag)

expect class TimeProvider() {
    fun nowMillis(): Long
}

expect object ConsoleBridge {
    fun log(level: LogLevel, tag: String, message: String)
}

object ConsoleLogSink : LogSink {
    override fun write(record: LogRecord) {
        val throwable = record.throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        val context = if (record.context.isEmpty()) "" else " ${record.context}"
        ConsoleBridge.log(
            record.level,
            record.tag,
            "${record.timestampMillis} ${record.level} ${record.tag}: ${record.message}$context$throwable"
        )
    }
}
```

### 平台实现示例

```kotlin
// androidMain/kotlin/logging/Platform.android.kt
package logging

import android.util.Log

actual class TimeProvider {
    actual fun nowMillis(): Long = System.currentTimeMillis()
}

actual object ConsoleBridge {
    actual fun log(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
}
```

```kotlin
// iosMain/kotlin/logging/Platform.ios.kt
package logging

import platform.Foundation.NSDate
import platform.os.log.OSLog
import platform.os.log.OSLogType
import platform.os.log.os_log

actual class TimeProvider {
    actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual object ConsoleBridge {
    private val subsystem = "com.yourcompany.app" // 可改成 expect/actual 注入

    actual fun log(level: LogLevel, tag: String, message: String) {
        val log = OSLog(subsystem = subsystem, category = tag)
        val type = when (level) {
            LogLevel.DEBUG -> OSLogTypeDebug
            LogLevel.INFO -> OSLogTypeInfo
            LogLevel.WARN -> OSLogTypeError
            LogLevel.ERROR -> OSLogTypeFault
        }
        os_log("%{public}@", log, type, message)
    }
}
```

### 初始化与使用

```kotlin
fun initLogging(isDebug: Boolean, extraSink: LogSink? = null) {
    val sinks = buildList {
        add(ConsoleLogSink)
        extraSink?.let { add(it) }
    }
    LogManager.install(
        LogConfig(
            minLevel = if (isDebug) LogLevel.DEBUG else LogLevel.INFO,
            sinks = sinks,
            defaultContext = mapOf("appVersion" to "1.0.0")
        )
    )
}

private val log = logger("FeatureX")

fun runFeature() {
    log.d { "初始化完成" }
    try {
        // ...
    } catch (t: Throwable) {
        log.e(throwable = t) { "执行失败" }
    }
}
```

### Sink 的意义
- `sink` 代表日志的输出终端，默认只有控制台输出。
- 同一条日志可以依次交给多个 sink，实现“一份代码，输出多份”。
- 想扩展到文件、CrashReporter、远程服务，只需实现额外的 `LogSink` 并添加到 `LogConfig.sinks`。

### JSON 序列化扩展示例

```kotlin
package logging

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class SerializableRecord(
    val timestampMillis: Long,
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String? = null,
    val context: Map<String, Any?> = emptyMap()
)

object JsonFormatter {
    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    fun format(record: LogRecord): String {
        val serializable = SerializableRecord(
            timestampMillis = record.timestampMillis,
            level = record.level.name,
            tag = record.tag,
            message = record.message,
            throwable = record.throwable?.stackTraceToString(),
            context = record.context
        )
        return json.encodeToString(serializable)
    }
}

object JsonConsoleSink : LogSink {
    override fun write(record: LogRecord) {
        ConsoleBridge.log(record.level, record.tag, JsonFormatter.format(record))
    }
}
```

将 `JsonConsoleSink` 加入 `LogConfig.sinks`，即可输出结构化 JSON 日志。若想写文件或上传服务器，可在这个基础上再新增 File/HTTP 的 sink。

### Tag 的灵活用法
- 可以在单例里缓存常用 `Logger`：`val auditLog = logger("Audit")`。
- 写扩展函数自动使用类名：`inline fun Any.logger() = logger(this::class.simpleName ?: "Anonymous")`。
- 需要动态 tag 时，可直接调用 `LogManager.emit` 或给 `Logger` 添加 `withTag` / `tagOverride` 参数。

### 后续可选增强
- 环境开关：基于 `BuildConfig`、编译 flag 控制 `minLevel`、是否写文件。
- 多 Sink 并用：控制台 + 文件 + CrashReporter。
- 结构化信息：在 `context` 放入 `userId`、`traceId` 等元数据。
- 采样/限频：在自定义 sink 内做去重或节流，避免刷屏。
- 应用生命周期：退后台或退出前调用 sink 的 `flush()/shutdown()`（按需添加接口）。

保持这个“核心 + 简化扩展点”的骨架，后续无论是生产环境落盘、集成监控，还是结构化日志，都能在现有基础上平滑添加。这样既不会太笨重，又能满足跨平台日志的主要需求。

