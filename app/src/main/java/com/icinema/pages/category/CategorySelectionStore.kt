package com.icinema.pages.category

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorySelectionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasSavedSelection(): Boolean {
        return preferences.contains(KEY_SELECTED_CATEGORY_IDS)
    }

    fun loadSelectedCategoryIds(): Set<Int> {
        return preferences
            .getStringSet(KEY_SELECTED_CATEGORY_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    fun saveSelectedCategoryIds(categoryIds: Set<Int>) {
        preferences.edit()
            .putStringSet(KEY_SELECTED_CATEGORY_IDS, categoryIds.map { it.toString() }.toSet())
            .apply()
    }

    fun resolveVisibleCategoryIds(
        selectedCategoryIds: Set<Int>,
        allCategoryIds: Set<Int>
    ): Set<Int> {
        if (allCategoryIds.isEmpty()) return emptySet()
        return selectedCategoryIds.intersect(allCategoryIds)
    }

    companion object {
        private const val PREF_NAME = "home_category_editor"
        private const val KEY_SELECTED_CATEGORY_IDS = "selected_category_ids"
    }
}
