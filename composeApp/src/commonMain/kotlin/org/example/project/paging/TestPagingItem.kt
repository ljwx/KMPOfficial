package org.example.project.paging

import org.example.project.common.paging.BasePagingItem

data class TestPagingItem(
    val id: Int,
    val title: String,
    val description: String
) : BasePagingItem