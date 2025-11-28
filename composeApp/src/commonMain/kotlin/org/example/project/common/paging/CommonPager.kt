package org.example.project.common.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow

private class CommonPagingSource<Item : BasePagingItem>(
    private val startIndex: Int,
    private val loadData: (index: Int, pageSize: Int) -> List<Item>
) :
    PagingSource<Int, Item>() {

    override fun getRefreshKey(state: PagingState<Int, Item>): Int? {
        return null
//        state.anchorPosition?.let { anchorPosition ->
//            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
//                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
//        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
        return try {
            val index = params.key ?: startIndex
            val pageSize = params.loadSize
            val result = loadData(index, pageSize)
            LoadResult.Page(
                data = result,
                prevKey = if (index == startIndex) null else index - 1,
                nextKey = if (result.isEmpty()) null else index + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }


}

fun <Item : BasePagingItem> createCommonPager(
    startIndex: Int = 0,
    pageSize: Int = 25,
    loadData: (index: Int, pageSize: Int) -> List<Item>
): Flow<PagingData<Item>> {
    return Pager(
        config = PagingConfig(
            pageSize = pageSize,
            enablePlaceholders = false,
            initialLoadSize = pageSize,
            prefetchDistance = 5,
        ),
        pagingSourceFactory = { CommonPagingSource(startIndex, loadData) }
    ).flow
}