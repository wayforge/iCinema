package com.icinema.pages.home.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.icinema.domain.model.Category
import com.icinema.pages.home.HomeContract
import com.icinema.pages.home.components.PageHeader
import com.icinema.pages.home.components.SimpleEmptyState
import com.icinema.pages.home.components.VideoGrid
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DiscoverPage(
    state: HomeContract.UiState,
    snackbarHostState: SnackbarHostState,
    onRetry: (Int?) -> Unit,
    onVideoClick: (Long) -> Unit,
    onRefresh: (Int?) -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onOpenCategoryEditor: () -> Unit,
    onLoadMore: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryPages = remember(state.visibleCategories) {
        listOf<Category?>(null) + state.visibleCategories
    }
    val selectedPage = categoryPages.indexOfFirst { it?.id == state.selectedCategoryId }
        .takeIf { it >= 0 }
        ?: 0
    val currentSelectedCategoryId by rememberUpdatedState(state.selectedCategoryId)
    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { categoryPages.size }
    )

    LaunchedEffect(selectedPage, categoryPages.size) {
        if (pagerState.currentPage != selectedPage && selectedPage in categoryPages.indices) {
            pagerState.scrollToPage(selectedPage)
        }
    }

    LaunchedEffect(pagerState, categoryPages) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val categoryId = categoryPages.getOrNull(page)?.id
                if (categoryId != currentSelectedCategoryId) {
                    onCategorySelected(categoryId)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(
            sectionTitle = "发现",
            actions = {
                IconButton(onClick = onOpenCategoryEditor) {
                    Icon(
                        imageVector = Icons.Outlined.ViewWeek,
                        contentDescription = "管理分类标签"
                    )
                }
            }
        )

        CategoryBar(
            categories = state.visibleCategories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = onCategorySelected
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { page -> categoryPages[page]?.id ?: ALL_CATEGORY_PAGE_KEY }
        ) { page ->
            val categoryId = categoryPages[page]?.id
            val discoverState = state.discoverStates[categoryId] ?: HomeContract.VideoSectionState()
            VideoGrid(
                videos = discoverState.videos,
                isLoading = discoverState.isLoading,
                isBackgroundLoading = discoverState.isBackgroundLoading,
                isRefreshing = discoverState.isRefreshing,
                isLoadingMore = discoverState.isLoadingMore,
                error = discoverState.error,
                hasMorePages = discoverState.hasMorePages,
                snackbarHostState = snackbarHostState,
                onRetry = { onRetry(categoryId) },
                onRefresh = { onRefresh(categoryId) },
                onVideoClick = onVideoClick,
                onLoadMore = { onLoadMore(categoryId) },
                emptyContent = {
                    SimpleEmptyState(
                        title = "暂无内容",
                        subtitle = "试试切换分类或下拉刷新"
                    )
                },
                modifier = Modifier.fillMaxSize(),
                enablePullRefresh = true
            )
        }
    }
}

private const val ALL_CATEGORY_PAGE_KEY = -1
