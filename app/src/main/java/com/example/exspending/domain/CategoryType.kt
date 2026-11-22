package com.example.exspending.domain

/**
 * Direction of money for a category.
 * Amounts on transactions are always stored positive; the sign comes from this type.
 */
enum class CategoryType(val storageValue: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    /** UI label: Income / Expense / Transfer */
    val displayLabel: String
        get() = storageValue.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromStorage(value: String): CategoryType =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: EXPENSE

        fun displayLabelForStorage(value: String): String =
            fromStorage(value).displayLabel
    }
}
