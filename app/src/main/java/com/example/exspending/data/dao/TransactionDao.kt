package com.example.exspending.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.exspending.data.entity.TransactionEntity
import com.example.exspending.data.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON c.id = t.category_id
        ORDER BY t.date_time DESC, t.id DESC
        """
    )
    fun observeWithCategoryDesc(): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON c.id = t.category_id
        ORDER BY t.date_time ASC, t.id ASC
        """
    )
    suspend fun getWithCategoryAsc(): List<TransactionWithCategory>

    @Query("SELECT * FROM transactions ORDER BY date_time ASC, id ASC")
    suspend fun getAllAsc(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE is_initial_balance = 1 LIMIT 1")
    suspend fun getInitialBalance(): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE is_initial_balance = 0")
    suspend fun countNonInitial(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun remapCategory(oldCategoryId: Long, newCategoryId: Long)
}
