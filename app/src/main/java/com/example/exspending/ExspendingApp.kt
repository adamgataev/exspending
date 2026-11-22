package com.example.exspending

import android.app.Application
import com.example.exspending.data.ExspendingDatabase
import com.example.exspending.data.ExspendingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExspendingApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: ExspendingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = ExspendingDatabase.getInstance(this)
        repository = ExspendingRepository(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            templateDao = db.templateDao(),
            templateItemDao = db.templateItemDao(),
            appMetaDao = db.appMetaDao()
        )
        appScope.launch {
            repository.ensureSeeded()
        }
    }
}
