package org.example.project.feature.product

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.project.common.BaseViewModel
import org.example.project.log.KSLog
import org.example.project.network.model.ProductSummaryData
import org.example.project.page.mainhome.home.HomeEffect

class ProductViewModel(private val repository: IProductRepository) : BaseViewModel() {

    private val _productList = MutableStateFlow<List<ProductSummaryData>>(emptyList())
    val productList: StateFlow<List<ProductSummaryData>> = _productList.asStateFlow()

    private val _effect = Channel<HomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        KSLog.dRouter("ProductViewModel Init: $this")
        triggerLoadStateContentData()
    }

    override suspend fun loadStateContentData(isRefresh: Boolean) {
        super.loadStateContentData(isRefresh)
        val result = repository.getProductList()
        commonHandleRequestResult(result, true) {
            if (it.isNullOrEmpty()) {
                showMultiStateEmpty()
            } else {
                _productList.value = it
            }
        }
    }

    override fun multiStateRetry(data: String?) {
        super.multiStateRetry(data)
        triggerLoadStateContentData()
    }

    override fun pullRefresh(data: String?) {
        super.pullRefresh(data)
        triggerLoadStateContentData(true)
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _effect.send(HomeEffect.ShowToast(message))
        }
    }

    fun getDetail(productId: Int): ProductSummaryData? {
        return productList.value.find { it.id == productId }
    }

    override fun onCleared() {
        super.onCleared()
        KSLog.dRouter("ProductViewModel Cleared: $this")
    }
}