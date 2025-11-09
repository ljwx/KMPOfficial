package com.jdcr.kmplog.base

import com.jdcr.kmplog.performLog

fun interface LogSink {
    fun write(content: LogContent)
}

object ConsoleLogSink : LogSink {

    override fun write(content: LogContent) {
        val fullMessage = buildString {
            append(content.message)
            content.context?.let { append(", $it") }
            content.throwable?.let { "\n${it.stackTraceToString()}" }
        }
        performLog(content.level, content.tag, fullMessage)
    }

}