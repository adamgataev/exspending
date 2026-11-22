package com.example.exspending.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.domain.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemapRequest(
    val category: CategoryEntity,
    val usageCount: Int
)

class CategoriesViewModel(
    private val repository: ExspendingRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> =
        repository.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _remap = MutableStateFlow<RemapRequest?>(null)
    val remap: StateFlow<RemapRequest?> = _remap.asStateFlow()

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = repository.getCategories().size
            repository.upsertCategory(
                CategoryEntity(name = name.trim(), type = type.storageValue, sortOrder = order)
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.upsertCategory(category) }
    }

    fun requestDelete(category: CategoryEntity) {
        viewModelScope.launch {
            val usage = repository.categoryUsageCount(category.id)
            if (usage == 0) {
                repository.deleteCategoryIfUnused(category.id)
            } else {
                _remap.value = RemapRequest(category, usage)
            }
        }
    }

    fun confirmRemap(newCategoryId: Long) {
        val request = _remap.value ?: return
        viewModelScope.launch {
            repository.deleteCategoryRemapping(request.category.id, newCategoryId)
            _remap.value = null
        }
    }

    fun cancelRemap() {
        _remap.value = null
    }
}
