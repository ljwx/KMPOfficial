package org.example.project.network.exception

import org.example.project.network.model.BaseApiResponse

data class ResponseFailException(
    private val response: BaseApiResponse<*>,
    private val code: Int? = response.code
) :
    Exception(response.getMessage()) {

    override fun toString(): String {
        return super.toString()
    }

}