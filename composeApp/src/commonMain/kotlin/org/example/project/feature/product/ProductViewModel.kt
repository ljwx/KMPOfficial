package org.example.project.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.example.project.log.KSLog
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.network.exception.ResponseFailException
import org.example.project.network.model.ProductSummaryData

class ProductViewModel(private val repository: IProductRepository) : ViewModel() {

    val multiState: MutableStateFlow<MultiStateLayoutState> =
        MutableStateFlow(MultiStateLayoutState.Loading)

    val productList = MutableStateFlow<List<ProductSummaryData>>(emptyList())

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

}