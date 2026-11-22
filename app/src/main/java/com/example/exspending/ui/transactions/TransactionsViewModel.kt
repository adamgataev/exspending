package com.example.exspending.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.TemplateEntity
import com.example.exspending.domain.CategoryType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TransactionsViewModel(
    private val repository: ExspendingRepository
) : ViewModel() {

    val items: StateFlow<List<TransactionListItem>> =
        repository.observeTransactionListItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<TemplateEntity>> =
        repository.observeTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun applyTemplate(templateId: Long, start: LocalDateTime) {
        viewModelScope.launch {
            repository.applyTemplate(templateId, start)
        }
    }

    fun defaultCategoryIdFor(type: CategoryType, onReady: (Long?) -> Unit) {
        viewModelScope.launch {
            onReady(repository.getCategoriesByType(type).firstOrNull()?.id)
        }
    }
}
