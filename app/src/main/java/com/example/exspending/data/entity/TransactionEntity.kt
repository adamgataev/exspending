package com.example.exspending.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("category_id"), Index("date_time")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** Brussels local `yyyy-MM-dd HH:mm`. */
    @ColumnInfo(name = "date_time")
    val dateTime: String,

    /** Always positive. Sign comes from the category type. */
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "note")
    val note: String? = null,

    /** 1 = Opening balance row; amount is the opening balance. */
    @ColumnInfo(name = "is_initial_balance")
    val isInitialBalance: Int = 0
)
