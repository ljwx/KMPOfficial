package org.example.project.network.model

interface IBaseApiResponse {

    fun isCodeSuccess(): Boolean

    fun getMessage(): String

}