package com.example.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SpectralRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spectralDao(): SpectralDao
}
