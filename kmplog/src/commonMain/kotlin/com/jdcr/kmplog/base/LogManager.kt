package com.jdcr.kmplog.base

import kotlin.concurrent.Volatile
import kotlin.math.sin

object LogManager {

    @Volatile
    private var config: LogConfig = LogConfig()
    private val timeProvider: TimeProvider = TimeProvider()

    fun install(config: LogConfig) {
        this.config = config
    }

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
            timestampMillis = timeProvider.currentTimeMillis(),
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

}

expect class TimeProvider() {
    fun currentTimeMillis(): Long
}