package com.example.exspending.domain

import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.ui.transactions.TransactionListItem

data class MonthCashflow(
    val month: Int,
    val label: String,
    val income: Double,
    val expense: Double,
    val transfer: Double
) {
    val net: Double get() = income - expense - transfer
}

data class CategoryTotal(
    val name: String,
    val amount: Double
)

data class PeriodOverview(
    val label: String,
    val income: Double,
    val expense: Double,
    val transfer: Double,
    val months: List<MonthCashflow>,
    val incomeByCategory: List<CategoryTotal>,
    val expenseByCategory: List<CategoryTotal>
) {
    val net: Double get() = income - expense - transfer
}

object OverviewAggregator {
    val monthLabels = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun years(items: List<TransactionListItem>): List<Int> =
        items
            .filter { !it.isInitialBalance }
            .map { DateTimeHelper.parse(it.dateTime).year }
            .distinct()
            .sortedDescending()

    fun forYear(
        items: List<TransactionListItem>,
        categories: List<CategoryEntity>,
        year: Int
    ): PeriodOverview {
        val filtered = items.filter {
            !it.isInitialBalance && DateTimeHelper.parse(it.dateTime).year == year
        }
        return build(items = filtered, categories = categories, label = "$year")
    }

    fun forAll(
        items: List<TransactionListItem>,
        categories: List<CategoryEntity>
    ): PeriodOverview {
        val filtered = items.filter { !it.isInitialBalance }
        return build(items = filtered, categories = categories, label = "Total")
    }

    fun forMonth(
        items: List<TransactionListItem>,
        categories: List<CategoryEntity>,
        year: Int,
        month: Int
    ): PeriodOverview {
        val filtered = items.filter {
            !it.isInitialBalance &&
                DateTimeHelper.parse(it.dateTime).year == year &&
                DateTimeHelper.parse(it.dateTime).monthValue == month
        }
        return build(
            items = filtered,
            categories = categories,
            label = "${monthLabels[month - 1]} $year",
            singleMonth = month
        )
    }

    /** Monthly average divisor for yearly tables: 12 per year, or years×12 for Total. */
    fun yearlyAverageDivisor(selectedYear: Int?, yearCount: Int): Double =
        if (selectedYear == null) (yearCount.coerceAtLeast(1) * 12).toDouble() else 12.0

    fun asMonthlyAverages(rows: List<CategoryTotal>, divisor: Double): List<CategoryTotal> {
        val safe = divisor.coerceAtLeast(1.0)
        return rows.map { it.copy(amount = it.amount / safe) }
    }

    private fun build(
        items: List<TransactionListItem>,
        categories: List<CategoryEntity>,
        label: String,
        singleMonth: Int? = null
    ): PeriodOverview {
        var income = 0.0
        var expense = 0.0
        var transfer = 0.0
        val monthMap = (1..12).associateWith {
            MonthCashflow(it, monthLabels[it - 1], 0.0, 0.0, 0.0)
        }.toMutableMap()
        val incomeCats = mutableMapOf<String, Double>()
        val expenseCats = mutableMapOf<String, Double>()

        items.forEach { item ->
            val dt = DateTimeHelper.parse(item.dateTime)
            val month = monthMap.getValue(dt.monthValue)
            when (item.categoryType) {
                CategoryType.INCOME -> {
                    income += item.amount
                    monthMap[dt.monthValue] = month.copy(income = month.income + item.amount)
                    incomeCats[item.categoryName] =
                        (incomeCats[item.categoryName] ?: 0.0) + item.amount
                }
                CategoryType.EXPENSE -> {
                    expense += item.amount
                    monthMap[dt.monthValue] = month.copy(expense = month.expense + item.amount)
                    expenseCats[item.categoryName] =
                        (expenseCats[item.categoryName] ?: 0.0) + item.amount
                }
                CategoryType.TRANSFER -> {
                    transfer += item.amount
                    monthMap[dt.monthValue] = month.copy(transfer = month.transfer + item.amount)
                }
            }
        }

        val months = if (singleMonth != null) {
            listOf(monthMap.getValue(singleMonth))
        } else {
            monthMap.values.sortedBy { it.month }
        }

        return PeriodOverview(
            label = label,
            income = income,
            expense = expense,
            transfer = transfer,
            months = months,
            incomeByCategory = fillCategoryRows(
                categories = categories,
                type = CategoryType.INCOME,
                totals = incomeCats
            ),
            expenseByCategory = fillCategoryRows(
                categories = categories,
                type = CategoryType.EXPENSE,
                totals = expenseCats
            )
        )
    }

    fun fillCategoryRows(
        categories: List<CategoryEntity>,
        type: CategoryType,
        totals: Map<String, Double>
    ): List<CategoryTotal> =
        categories
            .filter {
                it.name != SpecialCategories.OPENING_BALANCE &&
                    CategoryType.fromStorage(it.type) == type
            }
            .sortedBy { it.name.lowercase() }
            .map { cat ->
                CategoryTotal(name = cat.name, amount = totals[cat.name] ?: 0.0)
            }
}
