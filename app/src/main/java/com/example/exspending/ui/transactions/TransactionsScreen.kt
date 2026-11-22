package com.example.exspending.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.DateTimeHelper
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.ui.components.ActionBottomBar
import com.example.exspending.ui.theme.AccentBlue
import com.example.exspending.ui.theme.AppBackground
import com.example.exspending.ui.theme.ExpenseRed
import com.example.exspending.ui.theme.IncomeGreen
import com.example.exspending.ui.theme.TextHint
import com.example.exspending.ui.theme.TextPrimary
import com.example.exspending.ui.theme.TextSecondary
import com.example.exspending.ui.theme.TransferAmber
import com.example.exspending.ui.util.MoneyFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Space for action buttons + scrim so list items can scroll underneath. */
private val ActionBarContentPadding = 72.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingTemplateId by remember { mutableStateOf<Long?>(null) }
    var applyDateTime by remember { mutableStateOf(DateTimeHelper.nowString()) }
    var showApplyDatePicker by remember { mutableStateOf(false) }
    var showApplyTimePicker by remember { mutableStateOf(false) }
    val currentBalance = items.firstOrNull()?.runningBalance ?: 0.0

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    Text(
                        text = MoneyFormat.format(currentBalance),
                        modifier = Modifier.padding(end = 16.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "No transactions yet.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                val sections = remember(items) {
                    items.groupBy { it.monthKey }.toList()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ActionBarContentPadding)
                ) {
                    sections.forEach { (_, monthItems) ->
                        stickyHeader {
                            Surface(color = MaterialTheme.colorScheme.background) {
                                Column {
                                    Text(
                                        text = monthItems.first().monthHeader.replaceFirstChar {
                                            it.uppercase()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                        items(monthItems, key = { it.id }) { item ->
                            TransactionRow(
                                item = item,
                                onClick = { onOpenTransaction(item.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.4f to AppBackground.copy(alpha = 0.45f),
                                1f to AppBackground.copy(alpha = 0.78f)
                            )
                        )
                    )
            ) {
                ActionBottomBar(
                    reserveNavigationBarSpace = false,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddIncome,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncomeGreen,
                            contentColor = AppBackground
                        )
                    ) {
                        Text("+ Income")
                    }
                    Button(
                        onClick = onAddExpense,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = AppBackground
                        )
                    ) {
                        Text("+ Expense")
                    }
                    Button(
                        onClick = { showTemplatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = AppBackground
                        )
                    ) {
                        Text("+ Template")
                    }
                }
            }
        }
    }

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Apply template") },
            text = {
                if (templates.isEmpty()) {
                    Text("No templates yet. Create one in Settings.")
                } else {
                    Column {
                        templates.forEach { template ->
                            Text(
                                text = template.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pendingTemplateId = template.id
                                        applyDateTime = DateTimeHelper.nowString()
                                        showTemplatePicker = false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    pendingTemplateId?.let { templateId ->
        AlertDialog(
            onDismissRequest = { pendingTemplateId = null },
            title = { Text("Start date and time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = applyDateTime,
                        onValueChange = {},
                        readOnly = true,
                        enabled = true,
                        label = { Text("Date and time") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showApplyDatePicker = true }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = runCatching { DateTimeHelper.parse(applyDateTime) }
                            .getOrElse { DateTimeHelper.now() }
                        viewModel.applyTemplate(templateId, start)
                        pendingTemplateId = null
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplateId = null }) { Text("Cancel") }
            }
        )
    }

    if (showApplyDatePicker) {
        val current = runCatching { DateTimeHelper.parse(applyDateTime) }.getOrElse { DateTimeHelper.now() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showApplyDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        applyDateTime = DateTimeHelper.format(LocalDateTime.of(date, current.toLocalTime()))
                        showApplyDatePicker = false
                        showApplyTimePicker = true
                    }
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showApplyTimePicker) {
        val current = runCatching { DateTimeHelper.parse(applyDateTime) }.getOrElse { DateTimeHelper.now() }
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showApplyTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = current.toLocalDate()
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        applyDateTime = DateTimeHelper.format(LocalDateTime.of(date, time))
                        showApplyTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showApplyTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Time") },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun TransactionRow(
    item: TransactionListItem,
    onClick: () -> Unit
) {
    val amountColor = when {
        item.isInitialBalance -> TextPrimary
        item.categoryType == CategoryType.INCOME -> IncomeGreen
        item.categoryType == CategoryType.TRANSFER -> TransferAmber
        else -> ExpenseRed
    }
    val amountPrefix = when {
        item.isInitialBalance -> ""
        item.categoryType == CategoryType.INCOME -> "+"
        else -> "−"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.isInitialBalance) SpecialCategories.OPENING_BALANCE else item.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(item.dateTime)
                    if (!item.note.isNullOrBlank() && !item.isInitialBalance) {
                        append(" · ")
                        append(item.note)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${MoneyFormat.format(item.amount)}",
                color = amountColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = MoneyFormat.format(item.runningBalance),
                style = MaterialTheme.typography.labelMedium,
                color = TextHint
            )
        }
    }
}
