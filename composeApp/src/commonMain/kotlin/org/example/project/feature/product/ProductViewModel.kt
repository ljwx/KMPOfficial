package org.example.project.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.project.log.KSLog
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.network.exception.ResponseFailException
import org.example.project.network.model.ProductSummaryData
import kotlinx.coroutines.channels.Channel
import org.example.project.page.home.HomeEffect

class ProductViewModel(private val repository: IProductRepository) : ViewModel() {

    val multiState: MutableStateFlow<MultiStateLayoutState> =
        MutableStateFlow(MultiStateLayoutState.Loading)

    val productList = MutableStateFlow<List<ProductSummaryData>>(emptyList())

    private val _effect = Channel<HomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        getList()
    }

    fun getList() {
        viewModelScope.launch {
            repository.getProductList().onSuccess { response ->
                if (response.isSuccessTrue()) {
                    productList.value = response.data!!
                    multiState.value = MultiStateLayoutState.Content
                } else {
                    KSLog.iNet("ViewModel: 请求响应失败，code=${response.code}, message=${response.getMessage()}")
                    multiState.value = MultiStateLayoutState.Error(ResponseFailException(response))
                }
            }.onFailure { exception ->
                KSLog.eNet("ViewModel: 请求异常", exception)
                multiState.value = MultiStateLayoutState.Error(exception)
            }
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _effect.send(org.example.project.page.home.HomeEffect.ShowToast(message))
        }
    }

}