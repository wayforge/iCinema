package com.icinema.pages.category

import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.Category
import javax.inject.Inject

interface CategoryBizPort {
    suspend fun loadCategories(): Result<List<Category>>
}

class RepositoryCategoryBizPort @Inject constructor(
    private val repository: ICmsRepository
) : CategoryBizPort {
    override suspend fun loadCategories(): Result<List<Category>> {
        return repository.getCategoryList()
    }
}
