package com.example.exspending.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.exspending.data.dao.AppMetaDao
import com.example.exspending.data.dao.CategoryDao
import com.example.exspending.data.dao.TemplateDao
import com.example.exspending.data.dao.TemplateItemDao
import com.example.exspending.data.dao.TransactionDao
import com.example.exspending.data.entity.AppMetaEntity
import com.example.exspending.data.entity.CategoryEntity
import com.example.exspending.data.entity.TemplateEntity
import com.example.exspending.data.entity.TemplateItemEntity
import com.example.exspending.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        TemplateEntity::class,
        TemplateItemEntity::class,
        AppMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ExspendingDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun templateDao(): TemplateDao
    abstract fun templateItemDao(): TemplateItemDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        @Volatile
        private var instance: ExspendingDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS recurrence_rules")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS templates_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO templates_new (id, name) SELECT id, name FROM templates"
                )
                db.execSQL("DROP TABLE templates")
                db.execSQL("ALTER TABLE templates_new RENAME TO templates")
            }
        }

        fun getInstance(context: Context): ExspendingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExspendingDatabase::class.java,
                    "exspending.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
