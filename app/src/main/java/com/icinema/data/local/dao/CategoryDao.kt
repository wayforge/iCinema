package com.icinema.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.icinema.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY sort ASC")
    suspend fun getParentCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sort ASC")
    suspend fun getChildCategories(parentId: Int): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE show = 1 ORDER BY sort ASC")
    suspend fun getVisibleCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE parentId = :parentId AND show = 1 ORDER BY sort ASC")
    suspend fun getVisibleChildCategories(parentId: Int): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int
}