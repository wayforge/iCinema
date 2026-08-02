package com.icinema.pages.home

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {
    private val reducer = HomeReducer()

    @Test
    fun discoverFirstLoadStartedCreatesCategoryLoadingState() {
        val next = reducer.reduce(
            current = HomeContract.UiState(),
            mutation = HomeContract.Mutation.DiscoverLoadStarted(
                page = 1,
                isRefresh = false,
                categoryId = 2
            )
        )

        val section = next.discoverStates.getValue(2)
        assertEquals(2, next.selectedCategoryId)
        assertTrue(section.isLoading)
        assertFalse(section.isBackgroundLoading)
        assertFalse(section.isRefreshing)
        assertFalse(section.isLoadingMore)
        assertEquals(1, section.currentPage)
    }

    @Test
    fun discoverCachedCategorySwitchKeepsVideosAndDoesNotClearState() {
        val existing = sampleVideo(id = 1)
        val next = reducer.reduce(
            current = HomeContract.UiState(
                discoverStates = mapOf(
                    null to HomeContract.VideoSectionState(),
                    5 to HomeContract.VideoSectionState(
                        videos = listOf(existing),
                        currentPage = 3,
                        error = "stale error"
                    )
                )
            ),
            mutation = HomeContract.Mutation.CategoryChanged(categoryId = 5)
        )

        val section = next.discoverStates.getValue(5)
        assertEquals(5, next.selectedCategoryId)
        assertFalse(section.isLoading)
        assertFalse(section.isBackgroundLoading)
        assertFalse(section.isRefreshing)
        assertEquals(listOf(existing), section.videos)
        assertEquals(3, section.currentPage)
        assertEquals("stale error", section.error)
    }

    @Test
    fun discoverRefreshStartedOnlyUpdatesTargetCategoryState() {
        val all = sampleVideo(id = 1)
        val category = sampleVideo(id = 2)
        val next = reducer.reduce(
            current = HomeContract.UiState(
                selectedCategoryId = 5,
                discoverStates = mapOf(
                    null to HomeContract.VideoSectionState(videos = listOf(all)),
                    5 to HomeContract.VideoSectionState(
                        videos = listOf(category),
                        currentPage = 2
                    )
                )
            ),
            mutation = HomeContract.Mutation.DiscoverLoadStarted(
                page = 1,
                isRefresh = true,
                categoryId = 5
            )
        )

        val allSection = next.discoverStates.getValue(null)
        val categorySection = next.discoverStates.getValue(5)
        assertEquals(listOf(all), allSection.videos)
        assertTrue(categorySection.isRefreshing)
        assertFalse(categorySection.isLoading)
        assertFalse(categorySection.isBackgroundLoading)
        assertEquals(listOf(category), categorySection.videos)
    }

    @Test
    fun discoverLoadMoreSuccessMergesTargetCategoryAndDeduplicates() {
        val existing = sampleVideo(id = 1)
        val duplicate = sampleVideo(id = 1)
        val next = reducer.reduce(
            current = HomeContract.UiState(
                selectedCategoryId = 5,
                discoverStates = mapOf(
                    5 to HomeContract.VideoSectionState(
                        videos = listOf(existing),
                        currentPage = 1,
                        isLoadingMore = true
                    )
                )
            ),
            mutation = HomeContract.Mutation.DiscoverLoadSucceeded(
                videos = listOf(duplicate, sampleVideo(id = 2)),
                page = 2,
                categoryId = 5
            )
        )

        val section = next.discoverStates.getValue(5)
        assertEquals(listOf(1L, 2L), section.videos.map { it.id })
        assertFalse(section.isLoadingMore)
        assertEquals(2, section.currentPage)
    }

    @Test
    fun visibleCategoriesPruneHiddenCategoryCacheAndFallbackToAll() {
        val next = reducer.reduce(
            current = HomeContract.UiState(
                selectedCategoryId = 7,
                discoverStates = mapOf(
                    null to HomeContract.VideoSectionState(videos = listOf(sampleVideo(id = 1))),
                    7 to HomeContract.VideoSectionState(videos = listOf(sampleVideo(id = 7)))
                )
            ),
            mutation = HomeContract.Mutation.VisibleCategoriesUpdated(
                visibleCategories = listOf(Category(id = 2, name = "电影", parentId = null)),
                selectedCategoryIds = setOf(2)
            )
        )

        assertNull(next.selectedCategoryId)
        assertTrue(next.discoverStates.containsKey(null))
        assertFalse(next.discoverStates.containsKey(7))
    }

    @Test
    fun searchRefreshStartedUsesResultRefreshingState() {
        val existing = sampleVideo(id = 1)
        val next = reducer.reduce(
            current = HomeContract.UiState(
                searchState = HomeContract.SearchSectionState(
                    input = "movie",
                    query = "movie",
                    hasSearched = true,
                    results = HomeContract.VideoSectionState(
                        videos = listOf(existing),
                        currentPage = 2
                    )
                )
            ),
            mutation = HomeContract.Mutation.SearchLoadStarted(
                page = 1,
                query = "movie",
                isRefresh = true
            )
        )

        assertTrue(next.searchState.isSearching)
        assertTrue(next.searchState.hasSearched)
        assertFalse(next.searchState.results.isLoading)
        assertFalse(next.searchState.results.isBackgroundLoading)
        assertTrue(next.searchState.results.isRefreshing)
        assertFalse(next.searchState.results.isLoadingMore)
        assertEquals(listOf(existing), next.searchState.results.videos)
    }

    private fun sampleVideo(id: Long): Video {
        return Video(
            id = id,
            name = "Video $id",
            pic = "",
            picThumb = null,
            actor = null,
            director = null,
            content = null,
            area = null,
            year = null,
            typeId = null,
            typeName = null,
            playFrom = null,
            playUrl = null,
            total = null
        )
    }
}
