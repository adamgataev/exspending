package com.example.exspending.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.DateTimeHelper
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.ui.components.ActionBottomBar
import com.example.exspending.ui.theme.ExpenseRed
import com.example.exspending.ui.theme.TextPrimary
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    viewModel: TransactionEditViewModel,
    transactionId: Long?,
    createType: CategoryType?,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val isNew = transactionId == null || transactionId == 0L
    var editing by remember { mutableStateOf(isNew) }

    LaunchedEffect(transactionId, createType) {
        when {
            transactionId != null && transactionId != 0L -> {
                viewModel.load(transactionId)
                editing = false
            }
            createType != null -> {
                viewModel.startNew(createType)
                editing = true
            }
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val flowIsIncome = when (createType) {
        CategoryType.INCOME -> true
        CategoryType.EXPENSE -> false
        else -> ui.preferredType == CategoryType.INCOME
    }

    val filteredCategories = remember(categories, flowIsIncome) {
        val allowed = if (flowIsIncome) {
            setOf(CategoryType.INCOME.storageValue)
        } else {
            setOf(CategoryType.EXPENSE.storageValue, CategoryType.TRANSFER.storageValue)
        }
        categories
            .filter { it.name != SpecialCategories.OPENING_BALANCE && it.type in allowed }
            .sortedBy { it.name.lowercase() }
    }
    val selectedCategory = categories.firstOrNull { it.id == ui.categoryId }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            ui.isInitialBalance -> SpecialCategories.OPENING_BALANCE
                            isNew && createType == CategoryType.INCOME -> "New income"
                            isNew -> "New expense"
                            editing -> "Edit transaction"
                            else -> "Transaction"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && !ui.isInitialBalance) {
                        if (!editing) {
                            IconButton(onClick = { editing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            if (ui.loaded && editing) {
                ActionBottomBar(reserveNavigationBarSpace = true) {
                    Button(
                        onClick = { viewModel.save(onBack) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isNew) "Create" else "Save")
                    }
                }
            }
        }
    ) { padding ->
        if (!ui.loaded) return@Scaffold

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ui.dateTime,
                    onValueChange = {},
                    readOnly = true,
                    enabled = editing,
                    label = { Text("Date and time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (editing) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }

            OutlinedTextField(
                value = ui.amountText,
                onValueChange = { if (editing) viewModel.updateAmount(it) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = !editing,
                enabled = editing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (!ui.isInitialBalance) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded && editing,
                    onExpandedChange = { if (editing) categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = editing,
                        label = { Text("Category") },
                        trailingIcon = {
                            if (editing) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded && editing,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${category.name} (${CategoryType.displayLabelForStorage(category.type)})"
                                    )
                                },
                                onClick = {
                                    viewModel.updateCategory(category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ui.note,
                    onValueChange = { if (editing) viewModel.updateNote(it) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = !editing,
                    enabled = editing
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onBack)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker && editing) {
        val current = runCatching { DateTimeHelper.parse(ui.dateTime) }.getOrElse { DateTimeHelper.now() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val time = current.toLocalTime()
                        viewModel.updateDateTime(
                            DateTimeHelper.format(LocalDateTime.of(date, time))
                        )
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker && editing) {
        val current = runCatching { DateTimeHelper.parse(ui.dateTime) }.getOrElse { DateTimeHelper.now() }
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = current.toLocalDate()
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        viewModel.updateDateTime(
                            DateTimeHelper.format(LocalDateTime.of(date, time))
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Time") },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
