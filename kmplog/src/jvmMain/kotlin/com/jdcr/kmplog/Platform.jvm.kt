package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel

actual fun platform() = "JVM"

internal actual fun performLog(time: String, level: LogLevel, tag: String, message: String) {
    val prefix = when (level) {
        LogLevel.DEBUG -> "DEBUG"
        LogLevel.INFO -> "INFO"
        LogLevel.WARN -> "WARN"
        LogLevel.ERROR -> "ERROR"
    }
    println("[$time] $prefix/$tag: $message")
}

