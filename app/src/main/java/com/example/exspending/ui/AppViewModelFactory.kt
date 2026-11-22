package com.example.exspending.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.excel.ExcelImportExport
import com.example.exspending.ui.overview.OverviewViewModel
import com.example.exspending.ui.settings.CategoriesViewModel
import com.example.exspending.ui.settings.SettingsViewModel
import com.example.exspending.ui.settings.TemplateDetailViewModel
import com.example.exspending.ui.settings.TemplatesViewModel
import com.example.exspending.ui.transactions.TransactionEditViewModel
import com.example.exspending.ui.transactions.TransactionsViewModel

class AppViewModelFactory(
    private val repository: ExspendingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val excel = ExcelImportExport(repository)
        return when {
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(repository) as T
            modelClass.isAssignableFrom(TransactionEditViewModel::class.java) ->
                TransactionEditViewModel(repository) as T
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) ->
                CategoriesViewModel(repository) as T
            modelClass.isAssignableFrom(TemplatesViewModel::class.java) ->
                TemplatesViewModel(repository) as T
            modelClass.isAssignableFrom(TemplateDetailViewModel::class.java) ->
                TemplateDetailViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, excel) as T
            modelClass.isAssignableFrom(OverviewViewModel::class.java) ->
                OverviewViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
