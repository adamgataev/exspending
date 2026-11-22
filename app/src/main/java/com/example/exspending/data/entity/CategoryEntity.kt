package com.example.exspending.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    /** income | expense | transfer */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
