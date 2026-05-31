package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpectralDao {
    @Query("SELECT * FROM spectral_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<SpectralRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpectralRecord): Long

    @Delete
    suspend fun deleteRecord(record: SpectralRecord)

    @Query("DELETE FROM spectral_records")
    suspend fun clearAllRecords()
}
