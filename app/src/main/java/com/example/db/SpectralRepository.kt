package com.example.db

import kotlinx.coroutines.flow.Flow

class SpectralRepository(private val spectralDao: SpectralDao) {
    val allRecords: Flow<List<SpectralRecord>> = spectralDao.getAllRecordsFlow()

    suspend fun insert(record: SpectralRecord): Long {
        return spectralDao.insertRecord(record)
    }

    suspend fun delete(record: SpectralRecord) {
        spectralDao.deleteRecord(record)
    }

    suspend fun clearAll() {
        spectralDao.clearAllRecords()
    }
}
