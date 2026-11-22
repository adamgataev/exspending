package com.example.exspending.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.ui.components.ActionBottomBar
import com.example.exspending.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val remap by viewModel.remap.collectAsStateWithLifecycle()

    var remapExpanded by remember { mutableStateOf(false) }
    var remapTarget by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val grouped = remember(categories) {
        val visible = categories.filter { it.name != SpecialCategories.OPENING_BALANCE }
        CategoryType.entries.mapNotNull { typeEntry ->
            val items = visible.filter {
                CategoryType.fromStorage(it.type) == typeEntry
            }
            if (items.isEmpty()) null else typeEntry to items
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            ActionBottomBar(reserveNavigationBarSpace = true) {
                Button(
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create category")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (typeEntry, items) ->
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column {
                            Text(
                                text = typeEntry.displayLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider()
                        }
                    }
                }
                items(items, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.name,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingDelete = category }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete category?") },
            text = {
                Text("Are you sure you want to delete “${category.name}”?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.requestDelete(category)
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    remap?.let { request ->
        val alternatives = categories.filter {
            it.id != request.category.id && it.name != SpecialCategories.OPENING_BALANCE
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelRemap,
            title = { Text("Remap category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${request.usageCount} records use “${request.category.name}”. " +
                            "Choose a replacement category."
                    )
                    ExposedDropdownMenuBox(
                        expanded = remapExpanded,
                        onExpandedChange = { remapExpanded = it }
                    ) {
                        val selected = alternatives.firstOrNull { it.id == remapTarget }
                        OutlinedTextField(
                            value = selected?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("New category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(remapExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = remapExpanded,
                            onDismissRequest = { remapExpanded = false }
                        ) {
                            alternatives.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        remapTarget = cat.id
                                        remapExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        remapTarget?.let { viewModel.confirmRemap(it) }
                    },
                    enabled = remapTarget != null
                ) { Text("Remap and delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemap) { Text("Cancel") }
            }
        )
    }
}
