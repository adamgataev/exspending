package com.example.exspending.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.excel.ExcelImportExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: ExspendingRepository,
    private val excel: ExcelImportExport
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun exportBackup(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { excel.exportBackup(it) }
                        ?: error("Cannot write file")
                }
                _message.value = "Export successful"
            }.onFailure {
                _message.value = "Export failed: ${formatError(it)}"
            }
        }
    }

    fun importBackup(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { excel.importBackup(it) }
                        ?: error("Cannot read file")
                }
                _message.value = "Backup imported"
            }.onFailure {
                _message.value = "Import failed: ${formatError(it)}"
            }
        }
    }

    private fun formatError(error: Throwable): String {
        val primary = error.message?.takeIf { it.isNotBlank() }
        val cause = error.cause?.message?.takeIf { it.isNotBlank() && it != primary }
        return when {
            primary != null && cause != null -> "$primary ($cause)"
            primary != null -> primary
            cause != null -> cause
            else -> error.javaClass.simpleName
        }
    }
}
