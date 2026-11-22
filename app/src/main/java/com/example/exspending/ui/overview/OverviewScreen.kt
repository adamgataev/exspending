package com.example.exspending.ui.overview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspending.domain.CategoryTotal
import com.example.exspending.domain.OverviewAggregator
import com.example.exspending.ui.theme.ExpenseRed
import com.example.exspending.ui.theme.IncomeGreen
import com.example.exspending.ui.theme.TextHint
import com.example.exspending.ui.theme.TextPrimary
import com.example.exspending.ui.theme.TextSecondary
import com.example.exspending.ui.theme.TransferAmber
import com.example.exspending.ui.util.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: OverviewViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var modeExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(ui.statsMode) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                actions = {
                    ExposedDropdownMenuBox(
                        expanded = modeExpanded,
                        onExpandedChange = { modeExpanded = it },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(168.dp)
                            .height(52.dp)
                    ) {
                        OutlinedTextField(
                            value = when (ui.statsMode) {
                                OverviewStatsMode.YEARLY -> "Yearly stats"
                                OverviewStatsMode.MONTHLY -> "Monthly stats"
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .height(52.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = modeExpanded,
                            onDismissRequest = { modeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Yearly stats") },
                                onClick = {
                                    viewModel.selectStatsMode(OverviewStatsMode.YEARLY)
                                    modeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Monthly stats") },
                                onClick = {
                                    viewModel.selectStatsMode(OverviewStatsMode.MONTHLY)
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (ui.statsMode) {
                OverviewStatsMode.YEARLY -> YearlyStatsContent(ui = ui, viewModel = viewModel)
                OverviewStatsMode.MONTHLY -> MonthlyStatsContent(ui = ui, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun YearlyStatsContent(
    ui: OverviewUiState,
    viewModel: OverviewViewModel
) {
    val period = ui.period
    YearChips(
        years = ui.years,
        selectedYear = ui.selectedYear,
        includeTotal = true,
        onSelect = viewModel::selectYear
    )

    Text(
        text = period.label,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
    )

    PeriodSummary(period)

    Spacer(Modifier.height(4.dp))
    Text(
        "Income vs expenses by month",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary
    )
    MonthlyCashflowChart(
        months = period.months,
        modifier = Modifier.offset(x = (-8).dp)
    )

    Spacer(Modifier.height(8.dp))
    CategoryPieChart(
        title = "Expenses by category",
        slices = period.expenseByCategory.filter { it.amount > 0.0 }
    )

    val avgDivisor = OverviewAggregator.yearlyAverageDivisor(ui.selectedYear, ui.years.size)
    CategoryAmountTable(
        title = "Income by category (monthly avg)",
        rows = OverviewAggregator.asMonthlyAverages(period.incomeByCategory, avgDivisor),
        amountColor = IncomeGreen
    )
    CategoryAmountTable(
        title = "Expenses by category (monthly avg)",
        rows = OverviewAggregator.asMonthlyAverages(period.expenseByCategory, avgDivisor),
        amountColor = ExpenseRed
    )
}

@Composable
private fun MonthlyStatsContent(
    ui: OverviewUiState,
    viewModel: OverviewViewModel
) {
    val period = ui.period
    YearChips(
        years = ui.years,
        selectedYear = ui.selectedYear,
        includeTotal = false,
        onSelect = { year -> if (year != null) viewModel.selectYear(year) }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewAggregator.monthLabels.forEachIndexed { index, label ->
            val month = index + 1
            FilterChip(
                selected = ui.selectedMonth == month,
                onClick = { viewModel.selectMonth(month) },
                label = { Text(label) }
            )
        }
    }

    Text(
        text = period.label,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
    )

    PeriodSummary(period)

    CategoryAmountTable(
        title = "Income by category",
        rows = period.incomeByCategory,
        amountColor = IncomeGreen
    )
    CategoryAmountTable(
        title = "Expenses by category",
        rows = period.expenseByCategory,
        amountColor = ExpenseRed
    )
}

@Composable
private fun YearChips(
    years: List<Int>,
    selectedYear: Int?,
    includeTotal: Boolean,
    onSelect: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (includeTotal) {
            FilterChip(
                selected = selectedYear == null,
                onClick = { onSelect(null) },
                label = { Text("Total") }
            )
        }
        years.forEach { year ->
            FilterChip(
                selected = selectedYear == year,
                onClick = { onSelect(year) },
                label = { Text(year.toString()) }
            )
        }
    }
}

@Composable
private fun PeriodSummary(period: com.example.exspending.domain.PeriodOverview) {
    SummaryRow("Income", MoneyFormat.format(period.income), IncomeGreen)
    SummaryRow("Expenses", MoneyFormat.format(period.expense), ExpenseRed)
    SummaryRow("Transfers (savings)", MoneyFormat.format(period.transfer), TransferAmber)
    SummaryRow("±", MoneyFormat.format(period.net), TextPrimary)
}

@Composable
fun CategoryAmountTable(
    title: String,
    rows: List<CategoryTotal>,
    amountColor: Color = TextPrimary
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = TextHint.copy(alpha = 0.4f))
        if (rows.isEmpty()) {
            Text("No categories", color = TextHint)
        } else {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.name, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        MoneyFormat.format(row.amount),
                        color = amountColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary)
        Text(value, color = color, fontWeight = FontWeight.SemiBold)
    }
}
