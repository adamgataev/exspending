package com.example.exspending.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {
    @Test
    fun runningBalance_includesInitialAndSignedMoves() {
        val result = BalanceCalculator.withRunningBalances(
            listOf(
                BalanceInput(1, 1000.0, CategoryType.INCOME, isInitialBalance = true),
                BalanceInput(2, 100.0, CategoryType.EXPENSE, isInitialBalance = false),
                BalanceInput(3, 50.0, CategoryType.TRANSFER, isInitialBalance = false),
                BalanceInput(4, 200.0, CategoryType.INCOME, isInitialBalance = false)
            )
        )
        assertEquals(1000.0, result[0].runningBalance, 0.001)
        assertEquals(900.0, result[1].runningBalance, 0.001)
        assertEquals(850.0, result[2].runningBalance, 0.001)
        assertEquals(1050.0, result[3].runningBalance, 0.001)
    }
}
