package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpectralDao {
    @Query("SELECT * FROM spectral_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SpectralRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpectralRecord)

    @Query("DELETE FROM spectral_records")
    suspend fun clearAll()
}
