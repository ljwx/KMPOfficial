package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel
import kotlin.js.Date
import kotlin.js.console

// JS 平台实现（浏览器/Node 共用）。
actual fun platform(): String = "JS"

internal actual fun performLog(level: LogLevel, tag: String, message: String) {
    val formatted = "[$tag] $message"
    when (level) {
        LogLevel.DEBUG -> console.log(formatted)
        LogLevel.INFO -> console.info(formatted)
        LogLevel.WARN -> console.warn(formatted)
        LogLevel.ERROR -> console.error(formatted)
    }
}

internal actual fun currentTimeMillis(): Long = Date.now().toLong()

