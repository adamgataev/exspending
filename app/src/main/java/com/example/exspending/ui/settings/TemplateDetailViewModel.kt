package com.example.exspending.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.data.entity.TemplateEntity
import com.example.exspending.data.entity.TemplateItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateDetailUi(
    val template: TemplateEntity? = null,
    val items: List<TemplateItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isNew: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateDetailViewModel(
    private val repository: ExspendingRepository
) : ViewModel() {

    private val templateId = MutableStateFlow<Long?>(null)
    private val draftItems = MutableStateFlow<List<TemplateItemEntity>>(emptyList())
    private var nextDraftId = -1L

    val ui: StateFlow<TemplateDetailUi> = templateId.flatMapLatest { id ->
        when {
            id == null -> flowOf(TemplateDetailUi())
            id == 0L -> combine(
                repository.observeCategories(),
                draftItems
            ) { categories, items ->
                TemplateDetailUi(
                    template = TemplateEntity(id = 0, name = ""),
                    items = items,
                    categories = categories,
                    isNew = true
                )
            }
            else -> combine(
                repository.observeTemplates(),
                repository.observeTemplateItems(id),
                repository.observeCategories()
            ) { templates, items, categories ->
                TemplateDetailUi(
                    template = templates.firstOrNull { it.id == id },
                    items = items,
                    categories = categories,
                    isNew = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplateDetailUi())

    fun load(id: Long) {
        if (id == 0L) {
            draftItems.value = emptyList()
            nextDraftId = -1L
        }
        templateId.value = id
    }

    fun rename(name: String) {
        val current = ui.value.template ?: return
        if (current.id == 0L) return
        viewModelScope.launch {
            repository.saveTemplate(current.copy(name = name.trim()))
        }
    }

    fun create(name: String, onCreated: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.saveTemplate(TemplateEntity(name = name.trim()))
            draftItems.value.forEachIndexed { index, item ->
                repository.saveTemplateItem(
                    item.copy(id = 0, templateId = id, sortOrder = index)
                )
            }
            draftItems.value = emptyList()
            onCreated()
        }
    }

    fun addItem(categoryId: Long, amount: Double, note: String?) {
        val id = templateId.value ?: return
        if (id == 0L) {
            val order = draftItems.value.size
            draftItems.value = draftItems.value + TemplateItemEntity(
                id = nextDraftId--,
                templateId = 0,
                sortOrder = order,
                categoryId = categoryId,
                amount = amount,
                note = note
            )
            return
        }
        viewModelScope.launch {
            val order = repository.getTemplateItems(id).size
            repository.saveTemplateItem(
                TemplateItemEntity(
                    templateId = id,
                    sortOrder = order,
                    categoryId = categoryId,
                    amount = amount,
                    note = note
                )
            )
        }
    }

    fun updateItem(item: TemplateItemEntity) {
        if (templateId.value == 0L) {
            draftItems.value = draftItems.value.map { if (it.id == item.id) item else it }
            return
        }
        viewModelScope.launch { repository.saveTemplateItem(item) }
    }

    fun deleteItem(item: TemplateItemEntity) {
        if (templateId.value == 0L) {
            draftItems.value = draftItems.value
                .filterNot { it.id == item.id }
                .mapIndexed { index, row -> row.copy(sortOrder = index) }
            return
        }
        viewModelScope.launch { repository.deleteTemplateItem(item) }
    }

    fun moveItem(itemId: Long, up: Boolean) {
        if (templateId.value == 0L) {
            val items = draftItems.value.toMutableList()
            val index = items.indexOfFirst { it.id == itemId }
            if (index < 0) return
            val target = if (up) index - 1 else index + 1
            if (target !in items.indices) return
            val tmp = items[index]
            items[index] = items[target]
            items[target] = tmp
            draftItems.value = items.mapIndexed { i, row -> row.copy(sortOrder = i) }
            return
        }
        viewModelScope.launch {
            val id = templateId.value ?: return@launch
            val items = repository.getTemplateItems(id).toMutableList()
            val index = items.indexOfFirst { it.id == itemId }
            if (index < 0) return@launch
            val target = if (up) index - 1 else index + 1
            if (target !in items.indices) return@launch
            val a = items[index]
            val b = items[target]
            repository.saveTemplateItem(a.copy(sortOrder = b.sortOrder))
            repository.saveTemplateItem(b.copy(sortOrder = a.sortOrder))
        }
    }
}
