package com.example.exspending.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.exspending.data.entity.TransactionEntity

/** Transaction joined with category name/type for list and balance math. */
data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,

    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "category_type")
    val categoryType: String
)
