package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel
import kotlin.wasm.jsinterop.JsFun

// WASM（浏览器）平台实现，基于 JS interop 转调 console。
actual fun platform(): String = "Wasm"

@JsFun("message => console.debug(message)")
private external fun consoleDebug(message: String)

@JsFun("message => console.info(message)")
private external fun consoleInfo(message: String)

@JsFun("message => console.warn(message)")
private external fun consoleWarn(message: String)

@JsFun("message => console.error(message)")
private external fun consoleError(message: String)

@JsFun("() => Date.now()")
private external fun dateNow(): Double

internal actual fun performLog(level: LogLevel, tag: String, message: String) {
    val formatted = "[$tag] $message"
    when (level) {
        LogLevel.DEBUG -> consoleDebug(formatted)
        LogLevel.INFO -> consoleInfo(formatted)
        LogLevel.WARN -> consoleWarn(formatted)
        LogLevel.ERROR -> consoleError(formatted)
    }
}

internal actual fun currentTimeMillis(): Long = dateNow().toLong()

