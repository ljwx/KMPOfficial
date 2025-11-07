package com.jdcr.kmplog.base

data class LogConfig(
    val miniLevel: LogLevel = LogLevel.DEBUG,
    val sinks: List<LogSink> = listOf(ConsoleLogSink),
    val defaultCOntext: Map<String, Any?>? = null
)