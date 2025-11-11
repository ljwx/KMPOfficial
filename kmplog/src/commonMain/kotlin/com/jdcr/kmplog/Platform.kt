package com.jdcr.kmplog

import com.jdcr.kmplog.base.LogLevel

expect fun platform(): String

internal expect fun performLog(time: String, level: LogLevel, tag: String, message: String)