package com.icinema.pages.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore as FilledExplore
import androidx.compose.material.icons.filled.Person as FilledPerson
import androidx.compose.material.icons.filled.Search as FilledSearch
import androidx.compose.material.icons.outlined.Explore as OutlinedExplore
import androidx.compose.material.icons.outlined.Person as OutlinedPerson
import androidx.compose.material.icons.outlined.Search as OutlinedSearch
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class HomeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Discover("发现", Icons.Filled.FilledExplore, Icons.Outlined.OutlinedExplore),
    Search("搜索", Icons.Filled.FilledSearch, Icons.Outlined.OutlinedSearch),
    Mine("我的", Icons.Filled.FilledPerson, Icons.Outlined.OutlinedPerson)
}
