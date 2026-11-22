package com.example.exspending.data.excel

import com.example.exspending.data.ExspendingRepository
import com.example.exspending.data.entity.AppMetaEntity
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.data.entity.TemplateEntity
import com.example.exspending.data.entity.TemplateItemEntity
import com.example.exspending.data.entity.TransactionEntity
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.DateTimeHelper
import com.example.exspending.domain.SpecialCategories
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

/**
 * Excel backup (sheet per table) and legacy spending-plan import.
 */
class ExcelImportExport(
    private val repository: ExspendingRepository
) {
    suspend fun exportBackup(output: OutputStream) {
        Workbook(output, "Exspending", "1.0").use { wb ->
            writeCategories(wb, repository.getAllCategories())
            writeTransactions(wb, repository.getAllTransactions())
            writeTemplates(wb, repository.getAllTemplates())
            writeTemplateItems(wb, repository.getAllTemplateItems())
            writeAppMeta(wb, repository.getAllAppMeta())
        }
    }

    suspend fun importBackup(input: InputStream) {
        ReadableWorkbook(input).use { wb ->
            requireSheet(wb, "categories")
            requireSheet(wb, "transactions")
            val categories = readSheet(wb, "categories") { row ->
                CategoryEntity(
                    id = row.longCell(0) ?: 0L,
                    name = row.stringCell(1).orEmpty(),
                    type = row.stringCell(2).orEmpty(),
                    sortOrder = row.intCell(3) ?: 0
                )
            }
            val templates = readSheet(wb, "templates") { row ->
                TemplateEntity(
                    id = row.longCell(0) ?: 0L,
                    name = row.stringCell(1).orEmpty()
                )
            }
            val templateItems = readSheet(wb, "template_items") { row ->
                TemplateItemEntity(
                    id = row.longCell(0) ?: 0L,
                    templateId = row.longCell(1) ?: 0L,
                    sortOrder = row.intCell(2) ?: 0,
                    categoryId = row.longCell(3) ?: 0L,
                    amount = row.doubleCell(4) ?: 0.0,
                    note = row.stringCell(5)
                )
            }
            val transactions = readSheet(wb, "transactions") { row ->
                TransactionEntity(
                    id = row.longCell(0) ?: 0L,
                    dateTime = row.stringCell(1).orEmpty(),
                    amount = row.doubleCell(2) ?: 0.0,
                    categoryId = row.longCell(3) ?: 0L,
                    note = row.stringCell(4),
                    isInitialBalance = row.intCell(5) ?: 0
                )
            }
            val meta = readSheet(wb, "app_meta") { row ->
                AppMetaEntity(
                    key = row.stringCell(0).orEmpty(),
                    value = row.stringCell(1).orEmpty()
                )
            }
            repository.replaceAllData(
                categories = categories,
                transactions = transactions,
                templates = templates,
                templateItems = templateItems,
                meta = meta
            )
        }
        repository.ensureSeeded()
    }

    private fun requireSheet(wb: ReadableWorkbook, name: String) {
        require(wb.findSheet(name).isPresent) {
            "Invalid backup: sheet “$name” is missing"
        }
    }

    /**
     * Legacy format: Datum | Type | Amount | Category | Balance | Notitie
     */
    suspend fun importLegacy(input: InputStream) {
        ReadableWorkbook(input).use { wb ->
            val sheet = wb.firstSheet
            val rows = sheet.read().drop(1).toList()

            data class LegacyRow(
                val date: LocalDate,
                val type: String,
                val amount: Double,
                val category: String,
                val balance: Double?,
                val note: String?
            )

            val parsed = rows.mapNotNull { row ->
                val dateRaw = row.stringCell(0) ?: return@mapNotNull null
                if (dateRaw.isBlank()) return@mapNotNull null
                val date = runCatching { DateTimeHelper.parseDate(dateRaw) }.getOrNull()
                    ?: return@mapNotNull null
                LegacyRow(
                    date = date,
                    type = row.stringCell(1).orEmpty(),
                    amount = row.doubleCell(2) ?: 0.0,
                    category = row.stringCell(3).orEmpty(),
                    balance = row.doubleCell(4),
                    note = row.stringCell(5)
                )
            }
            if (parsed.isEmpty()) return

            val firstBalance = parsed.firstNotNullOfOrNull { it.balance } ?: 0.0
            val times = DateTimeHelper.importTimesForDates(parsed.map { it.date })

            val transactions = mutableListOf<TransactionEntity>()
            val firstTime = times.first().minusMinutes(1)
            val openingCat = repository.findOrCreateCategory(
                name = SpecialCategories.OPENING_BALANCE,
                type = CategoryType.INCOME
            )
            transactions += TransactionEntity(
                dateTime = DateTimeHelper.format(firstTime),
                amount = firstBalance,
                categoryId = openingCat.id,
                note = SpecialCategories.OPENING_BALANCE,
                isInitialBalance = 1
            )

            parsed.forEachIndexed { index, row ->
                val categoryType = mapLegacyCategoryType(row.type, row.category)
                val category = repository.findOrCreateCategory(
                    row.category.ifBlank { "Other" },
                    categoryType
                )
                transactions += TransactionEntity(
                    dateTime = DateTimeHelper.format(times[index]),
                    amount = kotlin.math.abs(row.amount),
                    categoryId = category.id,
                    note = row.note,
                    isInitialBalance = 0
                )
            }

            repository.replaceTransactions(transactions)
        }
    }

    private fun mapLegacyCategoryType(type: String, category: String): CategoryType {
        val cat = category.trim().lowercase()
        if (cat == "savings" || cat == "sparen") {
            return CategoryType.TRANSFER
        }
        if (cat == "investments" || cat == "beleggingen") {
            return CategoryType.EXPENSE
        }
        return when (type.trim().lowercase()) {
            "income" -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
    }

    private fun writeCategories(wb: Workbook, rows: List<CategoryEntity>) {
        val sheet = wb.newWorksheet("categories")
        sheet.value(0, 0, "id")
        sheet.value(0, 1, "name")
        sheet.value(0, 2, "type")
        sheet.value(0, 3, "sort_order")
        rows.forEachIndexed { i, row ->
            val r = i + 1
            sheet.value(r, 0, row.id)
            sheet.value(r, 1, row.name)
            sheet.value(r, 2, row.type)
            sheet.value(r, 3, row.sortOrder)
        }
    }

    private fun writeTransactions(wb: Workbook, rows: List<TransactionEntity>) {
        val sheet = wb.newWorksheet("transactions")
        listOf("id", "date_time", "amount", "category_id", "note", "is_initial_balance")
            .forEachIndexed { c, h -> sheet.value(0, c, h) }
        rows.forEachIndexed { i, row ->
            val r = i + 1
            sheet.value(r, 0, row.id)
            sheet.value(r, 1, row.dateTime)
            sheet.value(r, 2, row.amount)
            sheet.value(r, 3, row.categoryId)
            sheet.value(r, 4, row.note)
            sheet.value(r, 5, row.isInitialBalance)
        }
    }

    private fun writeTemplates(wb: Workbook, rows: List<TemplateEntity>) {
        val sheet = wb.newWorksheet("templates")
        listOf("id", "name").forEachIndexed { c, h -> sheet.value(0, c, h) }
        rows.forEachIndexed { i, row ->
            val r = i + 1
            sheet.value(r, 0, row.id)
            sheet.value(r, 1, row.name)
        }
    }

    private fun writeTemplateItems(wb: Workbook, rows: List<TemplateItemEntity>) {
        val sheet = wb.newWorksheet("template_items")
        listOf("id", "template_id", "sort_order", "category_id", "amount", "note")
            .forEachIndexed { c, h -> sheet.value(0, c, h) }
        rows.forEachIndexed { i, row ->
            val r = i + 1
            sheet.value(r, 0, row.id)
            sheet.value(r, 1, row.templateId)
            sheet.value(r, 2, row.sortOrder)
            sheet.value(r, 3, row.categoryId)
            sheet.value(r, 4, row.amount)
            sheet.value(r, 5, row.note)
        }
    }

    private fun writeAppMeta(wb: Workbook, rows: List<AppMetaEntity>) {
        val sheet = wb.newWorksheet("app_meta")
        sheet.value(0, 0, "key")
        sheet.value(0, 1, "value")
        rows.forEachIndexed { i, row ->
            sheet.value(i + 1, 0, row.key)
            sheet.value(i + 1, 1, row.value)
        }
    }

    private fun <T> readSheet(
        wb: ReadableWorkbook,
        name: String,
        mapper: (Row) -> T
    ): List<T> {
        val sheet = wb.findSheet(name).orElse(null) ?: return emptyList()
        return sheet.read().drop(1).mapNotNull { row ->
            runCatching { mapper(row) }.getOrNull()
        }
    }
}

private fun Row.stringCell(index: Int): String? =
    getCell(index)?.rawValue?.trim()?.takeIf { it.isNotEmpty() }

private fun Row.doubleCell(index: Int): Double? =
    getCell(index)?.asNumber()?.toDouble()
        ?: stringCell(index)?.replace(",", ".")?.toDoubleOrNull()

private fun Row.longCell(index: Int): Long? =
    getCell(index)?.asNumber()?.toLong()
        ?: stringCell(index)?.toLongOrNull()

private fun Row.intCell(index: Int): Int? =
    getCell(index)?.asNumber()?.toInt()
        ?: stringCell(index)?.toIntOrNull()
