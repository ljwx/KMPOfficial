package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.os.log.OSLog
import platform.os.log.OSLogType

actual fun platform() = "iOS"

internal actual fun performLog(level: LogLevel, tag: String, message: String) {
    val log = OSLog.default
    val logType = when (level) {
        LogLevel.DEBUG -> OSLogType.OS_LOG_TYPE_DEBUG
        LogLevel.INFO -> OSLogType.OS_LOG_TYPE_INFO
        LogLevel.WARN -> OSLogType.OS_LOG_TYPE_DEFAULT
        LogLevel.ERROR -> OSLogType.OS_LOG_TYPE_ERROR
    }
    os_log(logType, log, "%{public}s: %{public}s", tag, message)
}

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()