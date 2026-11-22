package com.example.exspending.data

import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.domain.CategoryType

/** Default flat categories inserted on first database create. */
object CategorySeed {
    fun defaults(): List<CategoryEntity> {
        var order = 0
        fun next() = order++

        val income = listOf("Salary", "Investment income", "Other income").map {
            CategoryEntity(name = it, type = CategoryType.INCOME.storageValue, sortOrder = next())
        }
        val expense = listOf(
            "Rent",
            "Utilities",
            "Internet and phone",
            "Insurance",
            "Subscriptions",
            "Groceries",
            "Eating out",
            "Transport",
            "Health",
            "Shopping",
            "Entertainment",
            "Investments",
            "Other"
        ).map {
            CategoryEntity(name = it, type = CategoryType.EXPENSE.storageValue, sortOrder = next())
        }
        val transfer = listOf("Savings").map {
            CategoryEntity(name = it, type = CategoryType.TRANSFER.storageValue, sortOrder = next())
        }
        return income + expense + transfer
    }

    /**
     * Former Dutch default names → English equivalents (for one-time DB cleanup).
     */
    val dutchToEnglish: List<Pair<String, String>> = listOf(
        "Salaris" to "Salary",
        "Beleggingsinkomen" to "Investment income",
        "Overig inkomen" to "Other income",
        "Huur" to "Rent",
        "Nutsvoorzieningen" to "Utilities",
        "Internet en telefoon" to "Internet and phone",
        "Verzekeringen" to "Insurance",
        "Abonnementen" to "Subscriptions",
        "Boodschappen" to "Groceries",
        "Uit eten" to "Eating out",
        "Vervoer" to "Transport",
        "Gezondheid" to "Health",
        "Beleggingen" to "Investments",
        "Overig" to "Other",
        "Sparen" to "Savings"
    )
}
