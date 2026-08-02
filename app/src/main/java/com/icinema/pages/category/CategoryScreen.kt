package com.icinema.pages.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onSaved: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(CategoryContract.UiIntent.Load)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CategoryContract.UiEffect.FinishWithResult -> onSaved(effect.changed)
                is CategoryContract.UiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首页分类") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(CategoryContract.UiIntent.ExitWithoutSave) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    val allSelected = state.categories.isNotEmpty() && state.editingCategoryIds.size == state.categories.size
                    TextButton(onClick = { viewModel.handleIntent(CategoryContract.UiIntent.ToggleSelectAll) }) {
                        Text(if (allSelected) "全不选" else "全选")
                    }
                    TextButton(onClick = { viewModel.handleIntent(CategoryContract.UiIntent.SaveAndExit) }) {
                        Text("保存")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        CategoryContent(
            state = state,
            onToggle = { categoryId ->
                viewModel.handleIntent(CategoryContract.UiIntent.ToggleCategory(categoryId))
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryContent(
    state: CategoryContract.UiState,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.isLoading && state.categories.isEmpty()) {
            CircularProgressIndicator()
        }

        Text(
            text = "已显示 ${state.editingCategoryIds.size} / ${state.categories.size} 个分类",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        state.error?.takeIf { state.categories.isEmpty() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.categories.forEach { category ->
                val selected = category.id in state.editingCategoryIds
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}
