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

    fun iNet(content: String, throwable: Throwable? = null) {
        KLog.i(ConstLogTag.NETWORK, content, throwable)
    }

    fun wNet(content: String, throwable: Throwable? = null) {
        KLog.w(ConstLogTag.NETWORK, content, throwable)
    }

    fun eNet(content: String, throwable: Throwable?) {
        KLog.e(ConstLogTag.NETWORK, content, throwable)
    }

    fun iMultiState(content: String, throwable: Throwable? = null) {
        KLog.i(ConstLogTag.MULTI_STATE, content, throwable)
    }

    fun wMultiState(content: String, throwable: Throwable? = null) {
        KLog.w(ConstLogTag.MULTI_STATE, content, throwable)
    }

    fun eMultiState(content: String, throwable: Throwable?) {
        KLog.e(ConstLogTag.MULTI_STATE, content, throwable)
    }

}