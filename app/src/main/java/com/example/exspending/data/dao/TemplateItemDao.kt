package com.example.exspending.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.exspending.data.entity.TemplateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateItemDao {
    @Query(
        """
        SELECT * FROM template_items
        WHERE template_id = :templateId
        ORDER BY sort_order ASC, id ASC
        """
    )
    fun observeForTemplate(templateId: Long): Flow<List<TemplateItemEntity>>

    @Query(
        """
        SELECT * FROM template_items
        WHERE template_id = :templateId
        ORDER BY sort_order ASC, id ASC
        """
    )
    suspend fun getForTemplate(templateId: Long): List<TemplateItemEntity>

    @Query("SELECT * FROM template_items ORDER BY template_id ASC, sort_order ASC, id ASC")
    suspend fun getAll(): List<TemplateItemEntity>

    @Query("SELECT COUNT(*) FROM template_items WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TemplateItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TemplateItemEntity>): List<Long>

    @Update
    suspend fun update(item: TemplateItemEntity)

    @Delete
    suspend fun delete(item: TemplateItemEntity)

    @Query("DELETE FROM template_items WHERE template_id = :templateId")
    suspend fun deleteForTemplate(templateId: Long)

    @Query("DELETE FROM template_items")
    suspend fun deleteAll()

    @Query("UPDATE template_items SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun remapCategory(oldCategoryId: Long, newCategoryId: Long)
}
