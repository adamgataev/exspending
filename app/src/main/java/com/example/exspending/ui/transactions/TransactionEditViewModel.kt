package com.example.exspending.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.data.entity.TransactionEntity
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.domain.DateTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionEditUiState(
    val id: Long = 0,
    val dateTime: String = DateTimeHelper.nowString(),
    val amountText: String = "",
    val note: String = "",
    val categoryId: Long? = null,
    val preferredType: CategoryType = CategoryType.EXPENSE,
    val isInitialBalance: Boolean = false,
    val loaded: Boolean = false
)

class TransactionEditViewModel(
    private val repository: ExspendingRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> =
        repository.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(TransactionEditUiState())
    val ui: StateFlow<TransactionEditUiState> = _ui.asStateFlow()

    fun startNew(type: CategoryType) {
        viewModelScope.launch {
            val primary = repository.getCategoriesByType(type)
                .firstOrNull { it.name != SpecialCategories.OPENING_BALANCE }
            val categoryId = primary?.id
            _ui.value = TransactionEditUiState(
                dateTime = DateTimeHelper.nowString(),
                preferredType = type,
                categoryId = categoryId,
                loaded = true
            )
        }
    }

    fun load(id: Long) {
        viewModelScope.launch {
            val tx = repository.getTransaction(id) ?: return@launch
            val category = repository.getCategories().firstOrNull { it.id == tx.categoryId }
            _ui.value = TransactionEditUiState(
                id = tx.id,
                dateTime = tx.dateTime,
                amountText = tx.amount.toString(),
                note = tx.note.orEmpty(),
                categoryId = tx.categoryId,
                preferredType = CategoryType.fromStorage(category?.type ?: CategoryType.EXPENSE.storageValue),
                isInitialBalance = tx.isInitialBalance == 1,
                loaded = true
            )
        }
    }

    fun updateDateTime(value: String) {
        _ui.value = _ui.value.copy(dateTime = value)
    }

    fun updateAmount(value: String) {
        _ui.value = _ui.value.copy(amountText = value)
    }

    fun updateNote(value: String) {
        _ui.value = _ui.value.copy(note = value)
    }

    fun updateCategory(id: Long) {
        val category = categories.value.firstOrNull { it.id == id }
        _ui.value = _ui.value.copy(
            categoryId = id,
            preferredType = CategoryType.fromStorage(
                category?.type ?: _ui.value.preferredType.storageValue
            )
        )
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _ui.value
            val amount = state.amountText.replace(",", ".").toDoubleOrNull() ?: return@launch
            val categoryId = state.categoryId ?: return@launch
            // Validate datetime format.
            runCatching { DateTimeHelper.parse(state.dateTime) }.getOrNull() ?: return@launch

            if (state.isInitialBalance) {
                repository.upsertInitialBalance(amount, state.dateTime)
            } else {
                repository.saveTransaction(
                    TransactionEntity(
                        id = state.id,
                        dateTime = state.dateTime,
                        amount = kotlin.math.abs(amount),
                        categoryId = categoryId,
                        note = state.note.ifBlank { null },
                        isInitialBalance = 0
                    )
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val id = _ui.value.id
            if (id != 0L) repository.deleteTransaction(id)
            onDone()
        }
    }
}
