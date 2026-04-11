package com.icinema.pages.history

import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.WatchHistoryItem
import javax.inject.Inject

interface HistoryBizPort {
    suspend fun loadHistory(): Result<List<WatchHistoryItem>>
    suspend fun deleteItem(id: Long): Result<Unit>
    suspend fun clearAll(): Result<Unit>
}

class RepositoryHistoryBizPort @Inject constructor(
    private val repository: ICmsRepository
) : HistoryBizPort {
    override suspend fun loadHistory(): Result<List<WatchHistoryItem>> {
        return repository.getWatchHistory()
    }

    override suspend fun deleteItem(id: Long): Result<Unit> {
        return repository.deleteHistoryItem(id)
    }

    override suspend fun clearAll(): Result<Unit> {
        return repository.clearWatchHistory()
    }
}
