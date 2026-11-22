package com.example.exspending.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.domain.OverviewAggregator
import com.example.exspending.domain.PeriodOverview
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class OverviewStatsMode {
    YEARLY,
    MONTHLY
}

data class OverviewUiState(
    val statsMode: OverviewStatsMode = OverviewStatsMode.YEARLY,
    val years: List<Int> = emptyList(),
    /** null = all-time total (yearly mode only) */
    val selectedYear: Int? = null,
    val selectedMonth: Int = 1,
    val period: PeriodOverview = PeriodOverview(
        label = "Total",
        income = 0.0,
        expense = 0.0,
        transfer = 0.0,
        months = emptyList(),
        incomeByCategory = emptyList(),
        expenseByCategory = emptyList()
    )
)

class OverviewViewModel(
    repository: ExspendingRepository
) : ViewModel() {

    private val statsMode = MutableStateFlow(OverviewStatsMode.YEARLY)
    private val selectedYear = MutableStateFlow<Int?>(null)
    private val selectedMonth = MutableStateFlow(LocalDate.now().monthValue)

    val ui: StateFlow<OverviewUiState> = combine(
        repository.observeTransactionListItems(),
        repository.observeCategories(),
        statsMode,
        selectedYear,
        selectedMonth
    ) { items, categories, mode, year, month ->
        val years = OverviewAggregator.years(items)
        val today = LocalDate.now()

        when (mode) {
            OverviewStatsMode.YEARLY -> {
                val effectiveYear = when {
                    year != null && year in years -> year
                    years.isNotEmpty() && year == null -> null
                    years.isNotEmpty() -> years.first()
                    else -> null
                }
                val period = if (effectiveYear == null) {
                    OverviewAggregator.forAll(items, categories)
                } else {
                    OverviewAggregator.forYear(items, categories, effectiveYear)
                }
                OverviewUiState(
                    statsMode = mode,
                    years = years,
                    selectedYear = effectiveYear,
                    selectedMonth = month,
                    period = period
                )
            }
            OverviewStatsMode.MONTHLY -> {
                val effectiveYear = when {
                    year != null && year in years -> year
                    years.isNotEmpty() -> years.first()
                    else -> today.year
                }
                val effectiveMonth = month.coerceIn(1, 12)
                OverviewUiState(
                    statsMode = mode,
                    years = years,
                    selectedYear = effectiveYear,
                    selectedMonth = effectiveMonth,
                    period = OverviewAggregator.forMonth(
                        items = items,
                        categories = categories,
                        year = effectiveYear,
                        month = effectiveMonth
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun selectStatsMode(mode: OverviewStatsMode) {
        statsMode.value = mode
        if (mode == OverviewStatsMode.MONTHLY) {
            val today = LocalDate.now()
            if (selectedYear.value == null) {
                selectedYear.value = today.year
            }
            selectedMonth.value = today.monthValue
        }
    }

    fun selectYear(year: Int?) {
        selectedYear.value = year
        if (statsMode.value == OverviewStatsMode.MONTHLY && year != null) {
            val today = LocalDate.now()
            if (year == today.year) {
                selectedMonth.value = today.monthValue
            }
        }
    }

    fun selectMonth(month: Int) {
        selectedMonth.value = month.coerceIn(1, 12)
    }
}
