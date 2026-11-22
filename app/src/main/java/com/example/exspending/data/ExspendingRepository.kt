package com.example.exspending.data

import com.example.exspending.data.dao.AppMetaDao
import com.example.exspending.data.dao.CategoryDao
import com.example.exspending.data.dao.TemplateDao
import com.example.exspending.data.dao.TemplateItemDao
import com.example.exspending.data.dao.TransactionDao
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.data.entity.TemplateEntity
import com.example.exspending.data.entity.TemplateItemEntity
import com.example.exspending.data.entity.TransactionEntity
import com.example.exspending.data.model.TransactionWithCategory
import com.example.exspending.domain.BalanceCalculator
import com.example.exspending.domain.BalanceInput
import com.example.exspending.domain.CategoryType
import com.example.exspending.domain.DateTimeHelper
import com.example.exspending.domain.SpecialCategories
import com.example.exspending.ui.transactions.TransactionListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Single entry point for local data. UI observes Flows; mutations recalculate via queries.
 */
class ExspendingRepository(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val templateDao: TemplateDao,
    private val templateItemDao: TemplateItemDao,
    private val appMetaDao: AppMetaDao
) {
    suspend fun ensureSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(CategorySeed.defaults())
        }
        // Migrate older installs: Beleggingen / Investments are expenses, not transfers.
        categoryDao.updateTypeByName("Beleggingen", CategoryType.EXPENSE.storageValue)
        categoryDao.updateTypeByName("Investments", CategoryType.EXPENSE.storageValue)
        // Rename legacy Dutch opening-balance category.
        if (categoryDao.getByName(SpecialCategories.OPENING_BALANCE) == null) {
            categoryDao.renameByName(
                SpecialCategories.OPENING_BALANCE_LEGACY,
                SpecialCategories.OPENING_BALANCE
            )
        }
        migrateDutchCategoriesToEnglish()
    }

    /** Merge or rename former Dutch default categories into English names. */
    private suspend fun migrateDutchCategoriesToEnglish() {
        for ((dutch, english) in CategorySeed.dutchToEnglish) {
            val dutchCat = categoryDao.getByName(dutch) ?: continue
            val englishCat = categoryDao.getByName(english)
            if (englishCat == null) {
                categoryDao.renameByName(dutch, english)
            } else if (dutchCat.id != englishCat.id) {
                deleteCategoryRemapping(dutchCat.id, englishCat.id)
            }
        }
    }

    suspend fun hasNonInitialTransactions(): Boolean = transactionDao.countNonInitial() > 0

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun getCategories(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun getCategoriesByType(type: CategoryType): List<CategoryEntity> =
        categoryDao.getByType(type.storageValue)

    suspend fun upsertCategory(category: CategoryEntity): Long =
        if (category.id == 0L) categoryDao.insert(category) else {
            categoryDao.update(category)
            category.id
        }

    suspend fun categoryUsageCount(categoryId: Long): Int =
        transactionDao.countByCategory(categoryId) + templateItemDao.countByCategory(categoryId)

    /**
     * Remaps linked transactions and template items, then deletes the old category.
     */
    suspend fun deleteCategoryRemapping(oldId: Long, newId: Long) {
        require(oldId != newId) { "Replacement category must differ" }
        transactionDao.remapCategory(oldId, newId)
        templateItemDao.remapCategory(oldId, newId)
        categoryDao.getById(oldId)?.let { categoryDao.delete(it) }
    }

    suspend fun deleteCategoryIfUnused(categoryId: Long): Boolean {
        if (categoryUsageCount(categoryId) > 0) return false
        categoryDao.getById(categoryId)?.let { categoryDao.delete(it) }
        return true
    }

    fun observeTransactionListItems(): Flow<List<TransactionListItem>> =
        transactionDao.observeWithCategoryDesc().map { rowsDesc ->
            val asc = rowsDesc.sortedWith(
                compareBy<TransactionWithCategory> { it.transaction.dateTime }
                    .thenBy { it.transaction.id }
            )
            val balances = BalanceCalculator.withRunningBalances(
                asc.map {
                    BalanceInput(
                        id = it.transaction.id,
                        amount = it.transaction.amount,
                        type = CategoryType.fromStorage(it.categoryType),
                        isInitialBalance = it.transaction.isInitialBalance == 1
                    )
                }
            ).associateBy { it.id }

            rowsDesc.map { row ->
                TransactionListItem(
                    id = row.transaction.id,
                    dateTime = row.transaction.dateTime,
                    amount = row.transaction.amount,
                    note = row.transaction.note,
                    categoryId = row.transaction.categoryId,
                    categoryName = row.categoryName,
                    categoryType = CategoryType.fromStorage(row.categoryType),
                    isInitialBalance = row.transaction.isInitialBalance == 1,
                    runningBalance = balances[row.transaction.id]?.runningBalance ?: 0.0,
                    monthKey = DateTimeHelper.monthKey(DateTimeHelper.parse(row.transaction.dateTime)),
                    monthHeader = DateTimeHelper.monthHeader(DateTimeHelper.parse(row.transaction.dateTime))
                )
            }
        }

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun saveTransaction(entity: TransactionEntity): Long =
        if (entity.id == 0L) transactionDao.insert(entity) else {
            transactionDao.update(entity)
            entity.id
        }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.getById(id)?.let { transactionDao.delete(it) }
    }

    suspend fun upsertInitialBalance(amount: Double, dateTime: String) {
        val existing = transactionDao.getInitialBalance()
        val category = ensureInitialBalanceCategory()
        if (existing == null) {
            transactionDao.insert(
                TransactionEntity(
                    dateTime = dateTime,
                    amount = amount,
                    categoryId = category.id,
                    note = SpecialCategories.OPENING_BALANCE,
                    isInitialBalance = 1
                )
            )
        } else {
            transactionDao.update(
                existing.copy(
                    amount = amount,
                    dateTime = dateTime,
                    categoryId = category.id,
                    note = SpecialCategories.OPENING_BALANCE
                )
            )
        }
    }

    private suspend fun ensureInitialBalanceCategory(): CategoryEntity {
        categoryDao.getByName(SpecialCategories.OPENING_BALANCE)?.let { return it }
        categoryDao.getByName(SpecialCategories.OPENING_BALANCE_LEGACY)?.let { return it }
        val id = categoryDao.insert(
            CategoryEntity(
                name = SpecialCategories.OPENING_BALANCE,
                type = CategoryType.INCOME.storageValue,
                sortOrder = -1
            )
        )
        return categoryDao.getById(id)!!
    }

    fun observeTemplates(): Flow<List<TemplateEntity>> = templateDao.observeAll()

    suspend fun getTemplates(): List<TemplateEntity> = templateDao.getAll()

    suspend fun getTemplate(id: Long): TemplateEntity? = templateDao.getById(id)

    suspend fun saveTemplate(template: TemplateEntity): Long =
        if (template.id == 0L) templateDao.insert(template) else {
            templateDao.update(template)
            template.id
        }

    suspend fun deleteTemplate(id: Long) {
        templateItemDao.deleteForTemplate(id)
        templateDao.getById(id)?.let { templateDao.delete(it) }
    }

    fun observeTemplateItems(templateId: Long): Flow<List<TemplateItemEntity>> =
        templateItemDao.observeForTemplate(templateId)

    suspend fun getTemplateItems(templateId: Long): List<TemplateItemEntity> =
        templateItemDao.getForTemplate(templateId)

    suspend fun saveTemplateItem(item: TemplateItemEntity): Long =
        if (item.id == 0L) templateItemDao.insert(item) else {
            templateItemDao.update(item)
            item.id
        }

    suspend fun deleteTemplateItem(item: TemplateItemEntity) {
        templateItemDao.delete(item)
    }

    /**
     * Applies a template once. [start] is the first item time; each next item +1 minute.
     */
    suspend fun applyTemplate(templateId: Long, start: LocalDateTime) {
        val items = templateItemDao.getForTemplate(templateId)
        items.forEachIndexed { index, item ->
            transactionDao.insert(
                TransactionEntity(
                    dateTime = DateTimeHelper.format(start.plusMinutes(index.toLong())),
                    amount = item.amount,
                    categoryId = item.categoryId,
                    note = item.note,
                    isInitialBalance = 0
                )
            )
        }
    }

    // region Excel / bulk access

    suspend fun getAllCategories() = categoryDao.getAll()
    suspend fun getAllTransactions() = transactionDao.getAllAsc()
    suspend fun getAllTemplates() = templateDao.getAll()
    suspend fun getAllTemplateItems() = templateItemDao.getAll()
    suspend fun getAllAppMeta() = appMetaDao.getAll()

    suspend fun replaceAllData(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        templates: List<TemplateEntity>,
        templateItems: List<TemplateItemEntity>,
        meta: List<com.example.exspending.data.entity.AppMetaEntity>
    ) {
        // Order matters for FK constraints.
        transactionDao.deleteAll()
        templateItemDao.deleteAll()
        templateDao.deleteAll()
        getAllCategories().forEach { categoryDao.delete(it) }

        if (categories.isNotEmpty()) categoryDao.insertAll(categories)
        templates.forEach { templateDao.insert(it) }
        if (templateItems.isNotEmpty()) templateItemDao.insertAll(templateItems)
        if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)
        appMetaDao.deleteAll()
        if (meta.isNotEmpty()) appMetaDao.upsertAll(meta)
    }

    /** Clears only transactions, then inserts [transactions] (categories kept). */
    suspend fun replaceTransactions(transactions: List<TransactionEntity>) {
        transactionDao.deleteAll()
        if (transactions.isNotEmpty()) {
            transactionDao.insertAll(transactions)
        }
    }

    suspend fun findOrCreateCategory(name: String, type: CategoryType): CategoryEntity {
        categoryDao.getByName(name)?.let { return it }
        val id = categoryDao.insert(
            CategoryEntity(
                name = name,
                type = type.storageValue,
                sortOrder = categoryDao.count()
            )
        )
        return categoryDao.getById(id)!!
    }

    // endregion
}
