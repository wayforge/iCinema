package com.icinema.pages.home.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.icinema.pages.home.HomeContract
import com.icinema.pages.home.components.PageHeader
import com.icinema.pages.home.components.SimpleEmptyState
import com.icinema.pages.home.components.VideoGrid

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DiscoverPage(
    state: HomeContract.UiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onOpenCategoryEditor: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discoverState = state.discoverState

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

        VideoGrid(
            videos = discoverState.videos,
            isLoading = discoverState.isLoading,
            isRefreshing = discoverState.isRefreshing,
            isLoadingMore = discoverState.isLoadingMore,
            error = discoverState.error,
            hasMorePages = discoverState.hasMorePages,
            snackbarHostState = snackbarHostState,
            onRetry = onRetry,
            onRefresh = onRefresh,
            onVideoClick = onVideoClick,
            onLoadMore = onLoadMore,
            emptyContent = {
                SimpleEmptyState(
                    title = "暂无内容",
                    subtitle = "试试切换分类或下拉刷新"
                )
            },
            modifier = Modifier.weight(1f),
            enablePullRefresh = true
        )
    }
}
