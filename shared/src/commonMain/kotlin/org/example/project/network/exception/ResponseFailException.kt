package org.example.project.network.exception

import org.example.project.network.model.BaseApiResponse

class ResponseFailException(private val response: BaseApiResponse<*>) :
    Exception(response.getMessage())