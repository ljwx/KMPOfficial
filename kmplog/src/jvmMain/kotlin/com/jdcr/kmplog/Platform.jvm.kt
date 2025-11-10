package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel

// JVM 平台实现，便于在 Compose Desktop 或服务器环境复用。
actual fun platform(): String = "JVM"

internal actual fun performLog(level: LogLevel, tag: String, message: String) {
    val formatted = "[$tag] $message"
    when (level) {
        LogLevel.DEBUG -> println("DEBUG: $formatted")
        LogLevel.INFO -> println("INFO: $formatted")
        LogLevel.WARN -> System.err.println("WARN: $formatted")
        LogLevel.ERROR -> System.err.println("ERROR: $formatted")
    }
}

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

