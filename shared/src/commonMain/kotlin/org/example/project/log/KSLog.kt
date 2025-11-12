package org.example.project.log

import com.jdcr.kmplog.KLog

object KSLog {

    fun iRouter(content: String, throwable: Throwable? = null) {
        KLog.i(ConstLogTag.ROUTER, content, throwable)
    }

    fun wRouter(content: String, throwable: Throwable? = null) {
        KLog.w(ConstLogTag.ROUTER, content, throwable)
    }

    fun eRouter(content: String, throwable: Throwable?) {
        KLog.e(ConstLogTag.ROUTER, content, throwable)
    }

}