package org.example.project.common

import org.example.project.network.model.BaseApiResponse

interface IBaseViewModel {

    fun triggerLoadStateContentData(isRefresh: Boolean = false)

    suspend fun loadStateContentData(isRefresh: Boolean = false)

    fun multiStateRetry(data: String? = null)

    fun pullRefresh(data: String? = null)

    fun <T> commonHandleRequestResult(
        requestResult: Result<BaseApiResponse<T>>,
        isMultiStateContent: Boolean,
        onFail: ((t: Throwable?) -> Unit)? = null,
        onSuccess: (value: T?) -> Unit
    )

}