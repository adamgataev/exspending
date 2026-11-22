package com.example.exspending.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.TemplateEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplatesViewModel(
    private val repository: ExspendingRepository
) : ViewModel() {

    val templates: StateFlow<List<TemplateEntity>> =
        repository.observeTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTemplate(name: String = "New template", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.saveTemplate(
                TemplateEntity(name = name.trim().ifBlank { "New template" })
            )
            onCreated(id)
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
