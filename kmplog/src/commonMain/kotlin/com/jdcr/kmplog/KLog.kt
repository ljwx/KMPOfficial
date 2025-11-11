package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogConfig
import com.jdcr.kmplog.base.LogContent
import com.jdcr.kmplog.base.LogLevel
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object KLog {
    @Volatile
    private var config: LogConfig = LogConfig()

    fun install(config: LogConfig) {
        this.config = config
    }

    @OptIn(ExperimentalTime::class)
    internal fun emit(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        val cfg = config
        if (cfg.miniLevel.priority > level.priority) return

        val record = LogContent(
            timestampMillis = Clock.System.now().toEpochMilliseconds(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            context = context
        )

        cfg.sinks.forEach { sink ->
            runCatching {
                sink.write(record)
            }
        }

    }

    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        emit(LogLevel.DEBUG, tag, message)
    }

    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        emit(LogLevel.INFO, tag, message)
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        emit(LogLevel.WARN, tag, message)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        emit(LogLevel.ERROR, tag, message)
    }

}