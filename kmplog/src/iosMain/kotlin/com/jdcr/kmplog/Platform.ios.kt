package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.timeIntervalSince1970

actual fun platform() = "iOS"

internal actual fun performLog(level: LogLevel, tag: String, message: String) {
    // 直接走 Foundation 提供的 NSLog，避免与 Objective-C 可变参数交互导致的内存问题。
    val prefix = when (level) {
        LogLevel.DEBUG -> "DEBUG"
        LogLevel.INFO -> "INFO"
        LogLevel.WARN -> "WARN"
        LogLevel.ERROR -> "ERROR"
    }
    val messageToLog = "$prefix [$tag] $message"
    NSLog(messageToLog)
}

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()