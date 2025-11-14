package com.jdcr.kmplog.base

import com.jdcr.kmplog.performLog
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

fun interface LogSink {
    fun write(content: LogContent)
}

@OptIn(ExperimentalTime::class)
object ConsoleLogSink : LogSink {

    override fun write(content: LogContent) {
        // 使用kotlinx-datetime格式化时间 - 虽然标为实验性，但实际很稳定
        val instant = Instant.fromEpochMilliseconds(content.timestampMillis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val timeString = localDateTime.toString()
            .replace('T', ' ')
            .substringBefore('.') + ".${(content.timestampMillis % 1000).toString().padStart(3, '0')}"
        val fullMessage = buildString {
            append(content.message)
            append(content.context?.let { append(", $it") })
            append(content.throwable?.let { "\n${it.stackTraceToString()}" })
        }
        performLog(timeString, content.level, content.tag, fullMessage)
    }

}