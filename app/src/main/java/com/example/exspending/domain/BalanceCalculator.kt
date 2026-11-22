package com.example.exspending.domain

/**
 * Computes running balances. Amounts are always positive; [CategoryType] supplies the sign.
 * Transfers leave the spendable cashflow (same as expenses for the single balance).
 */
object BalanceCalculator {
    fun signedDelta(type: CategoryType, amount: Double): Double = when (type) {
        CategoryType.INCOME -> amount
        CategoryType.EXPENSE, CategoryType.TRANSFER -> -amount
    }

    /**
     * [rows] must be ordered oldest → newest (`date_time` ASC).
     * Returns the same order with a running balance after each row.
     */
    fun withRunningBalances(
        rows: List<BalanceInput>
    ): List<BalanceOutput> {
        var balance = 0.0
        return rows.map { row ->
            if (row.isInitialBalance) {
                balance = row.amount
            } else {
                balance += signedDelta(row.type, row.amount)
            }
            BalanceOutput(
                id = row.id,
                runningBalance = balance
            )
        }
    }
}

data class BalanceInput(
    val id: Long,
    val amount: Double,
    val type: CategoryType,
    val isInitialBalance: Boolean
)

data class BalanceOutput(
    val id: Long,
    val runningBalance: Double
)
