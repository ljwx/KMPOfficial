package com.jdcr.kmplog.base

data class LogContent(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val context: Map<String, Any?>? = null
)
