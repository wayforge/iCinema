package com.icinema.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icinema.domain.model.Category

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

@Composable
private fun CategoryBar(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    if (categories.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    leadingIcon = if (selectedCategoryId == null) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    label = { Text("全部") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            items(categories) { category ->
                val categoryIcon = getCategoryIcon(category.name)
                val isSelected = selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

private fun getCategoryIcon(categoryName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        categoryName.contains("电影") -> Icons.Outlined.Movie
        categoryName.contains("剧") || categoryName.contains("连续") -> Icons.Outlined.LiveTv
        categoryName.contains("动漫") || categoryName.contains("动画") || categoryName.contains("卡通") -> Icons.Outlined.Animation
        categoryName.contains("综艺") || categoryName.contains("娱乐") -> Icons.Outlined.Mic
        categoryName.contains("纪录") -> Icons.Outlined.VideoLibrary
        categoryName.contains("短") -> Icons.Outlined.FiberManualRecord
        else -> Icons.Outlined.Category
    }
}
