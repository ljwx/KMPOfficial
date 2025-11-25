package org.example.project.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.log.KSLog
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.network.exception.ResponseFailException
import org.example.project.network.model.BaseApiResponse

abstract class BaseViewModel : ViewModel(), IBaseViewModel {

    private val _multiState = MutableStateFlow<MultiStateLayoutState>(MultiStateLayoutState.Loading)
    val multiState: StateFlow<MultiStateLayoutState> = _multiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    override suspend fun loadStateContentData(isRefresh: Boolean) {

    }

    override fun triggerLoadStateContentData(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                showMultiStateLading()
            }
            try {
                loadStateContentData(isRefresh)
            } catch (e: Exception) {
                showMultiStateError(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun <T> commonHandleRequestResult(
        requestResult: Result<BaseApiResponse<T>>,
        isMultiStateContent: Boolean,
        onFail: ((t: Throwable?) -> Unit)?,
        onSuccess: (value: T?) -> Unit
    ) {
        requestResult.onSuccess {
            if (it.isCodeSuccess()) {
                onSuccess(it.data)
                if (isMultiStateContent) {
                    showMultiStateContent()
                }
            } else {
                val exception = ResponseFailException(it)
                handleRequestFail(isMultiStateContent, exception, onFail)
            }
        }.onFailure {
            handleRequestFail(isMultiStateContent, it, onFail)
        }
    }

    private fun handleRequestFail(
        isMultiStateContent: Boolean,
        throwable: Throwable?,
        onFail: ((t: Throwable?) -> Unit)?
    ) {
        if (onFail == null) {
            onCommonRequestFail(throwable, isMultiStateContent)
        } else {
            onFail(throwable)
        }
    }

    fun onCommonRequestFail(exception: Throwable?, isMultiStateContent: Boolean) {
        if (isMultiStateContent) {
            showMultiStateError(exception)
        }
        KSLog.eNet("请求失败", exception)
    }

    override fun pullRefresh(data: String?) {
        triggerLoadStateContentData(isRefresh = true)
    }

    override fun multiStateRetry(data: String?) {
        triggerLoadStateContentData(isRefresh = false)
    }

    protected fun showMultiStateContent() {
        _multiState.value = MultiStateLayoutState.Content
    }

    protected fun showMultiStateLading() {
        _multiState.value = MultiStateLayoutState.Loading
    }

    protected fun showMultiStateError(t: Throwable?) {
        _multiState.value = MultiStateLayoutState.Error(t)
    }

    protected fun showMultiStateEmpty() {
        _multiState.value = MultiStateLayoutState.Empty
    }
}
