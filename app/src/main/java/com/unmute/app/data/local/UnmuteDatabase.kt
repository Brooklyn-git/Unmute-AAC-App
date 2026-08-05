package com.unmute.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BoardEntity::class,
        CategoryEntity::class,
        CardEntity::class,
        GridProfileEntity::class,
        WordUsageEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class UnmuteDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun gridProfileDao(): GridProfileDao
    abstract fun wordUsageDao(): WordUsageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isPreset INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN symbolType INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE categories ADD COLUMN symbolValue TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE categories
                    SET symbolType = (
                            SELECT imageType FROM cards
                            WHERE cards.categoryId = categories.id
                            ORDER BY orderIndex LIMIT 1
                        ),
                        symbolValue = (
                            SELECT imageValue FROM cards
                            WHERE cards.categoryId = categories.id
                            ORDER BY orderIndex LIMIT 1
                        )
                    WHERE symbolValue = ''
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN shortcutCategoryId INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_usage` (
                        `word` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `uses` INTEGER NOT NULL,
                        `lastUsed` INTEGER NOT NULL,
                        PRIMARY KEY(`word`, `language`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): UnmuteDatabase =
            Room.databaseBuilder(context, UnmuteDatabase::class.java, "unmute.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
