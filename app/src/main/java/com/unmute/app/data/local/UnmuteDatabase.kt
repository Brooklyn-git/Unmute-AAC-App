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
    ],
    version = 2,
    exportSchema = true,
)
abstract class UnmuteDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun gridProfileDao(): GridProfileDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isPreset INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun build(context: Context): UnmuteDatabase =
            Room.databaseBuilder(context, UnmuteDatabase::class.java, "unmute.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
