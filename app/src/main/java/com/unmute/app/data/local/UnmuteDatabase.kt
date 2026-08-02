package com.unmute.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BoardEntity::class,
        CategoryEntity::class,
        CardEntity::class,
        GridProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UnmuteDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun gridProfileDao(): GridProfileDao

    companion object {
        fun build(context: Context): UnmuteDatabase =
            Room.databaseBuilder(context, UnmuteDatabase::class.java, "unmute.db")
                .build()
    }
}
