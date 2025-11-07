package com.jdcr.kmplog

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

expect object PlatformLogger {

    fun i(tag: String, message: String)

    fun d(tag: String, message: String)

    fun w(tag: String, message: String, throwable: Throwable? = null)

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)

}

class Logger internal  constructor(private val tag: String){
}