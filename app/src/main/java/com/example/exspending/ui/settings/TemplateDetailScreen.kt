package com.example.exspending.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspending.data.entity.TemplateItemEntity
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.ui.components.ActionBottomBar
import com.example.exspending.ui.theme.AccentBlue
import com.example.exspending.ui.theme.AppBackground
import com.example.exspending.ui.theme.ExpenseRed
import com.example.exspending.ui.util.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    viewModel: TemplateDetailViewModel,
    templateId: Long,
    startInEditMode: Boolean = false,
    onBack: () -> Unit
) {
    val isNew = templateId == 0L
    LaunchedEffect(templateId) { viewModel.load(templateId) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val template = ui.template

    var editing by remember(templateId, startInEditMode) {
        mutableStateOf(startInEditMode || isNew)
    }

    var name by remember(templateId, template?.name) {
        mutableStateOf(if (isNew) "" else template?.name.orEmpty())
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<TemplateItemEntity?>(null) }

    val pickerCategories = remember(ui.categories) {
        ui.categories
            .filter { it.name != SpecialCategories.OPENING_BALANCE }
            .sortedBy { it.name.lowercase() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isNew -> "New template"
                            editing -> "Edit template"
                            else -> template?.name ?: "Template"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!editing && !isNew) {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (editing) {
                ActionBottomBar(reserveNavigationBarSpace = true) {
                    Button(
                        onClick = {
                            if (isNew) {
                                viewModel.create(name, onBack)
                            } else {
                                viewModel.rename(name)
                                onBack()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isNew) "Create" else "Save")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (editing) name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !editing,
                enabled = editing
            )

            Text("Items", style = MaterialTheme.typography.titleMedium)
            ui.items.forEach { item ->
                val catName = ui.categories.firstOrNull { it.id == item.categoryId }?.name ?: "?"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$catName · ${MoneyFormat.format(item.amount)}")
                        if (!item.note.isNullOrBlank()) {
                            Text(item.note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (editing) {
                        IconButton(onClick = { viewModel.moveItem(item.id, up = true) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = { viewModel.moveItem(item.id, up = false) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        IconButton(onClick = { pendingDeleteItem = item }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed
                            )
                        }
                    }
                }
            }

            if (editing) {
                HorizontalDivider()
                Text("Add item", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    val selected = pickerCategories.firstOrNull { it.id == categoryId }
                    OutlinedTextField(
                        value = selected?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        pickerCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val amount = amountText.replace(",", ".").toDoubleOrNull() ?: return@Button
                            val cat = categoryId ?: return@Button
                            viewModel.addItem(cat, amount, note.ifBlank { null })
                            amountText = ""
                            note = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = AppBackground
                        )
                    ) {
                        Text("+ Add item")
                    }
                }
            }
        }
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("Delete item?") },
            text = { Text("Are you sure you want to remove this template item?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteItem = null
                        viewModel.deleteItem(item)
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancel") }
            }
        )
    }
}
