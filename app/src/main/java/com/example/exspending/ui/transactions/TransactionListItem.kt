package com.example.exspending.ui.transactions

import com.example.exspending.domain.CategoryType

/** UI model for one row in the transaction list (includes derived running balance). */
data class TransactionListItem(
    val id: Long,
    val dateTime: String,
    val amount: Double,
    val note: String?,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: CategoryType,
    val isInitialBalance: Boolean,
    val runningBalance: Double,
    val monthKey: String,
    val monthHeader: String
)
