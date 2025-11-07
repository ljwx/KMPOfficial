package com.jdcr.kmplog.base

fun interface LogSink {
    fun write(content: LogContent)
}

object ConsoleLogSink : LogSink {

    override fun write(content: LogContent) {

    }

}