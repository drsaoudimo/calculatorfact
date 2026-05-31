package com.example.db

import kotlinx.coroutines.flow.Flow

class SpectralRepository(private val spectralDao: SpectralDao) {
    val allRecords: Flow<List<SpectralRecord>> = spectralDao.getAllRecords()

    suspend fun insertRecord(record: SpectralRecord) {
        spectralDao.insertRecord(record)
    }

    suspend fun deleteRecord(id: Int) {
        spectralDao.deleteRecordById(id)
    }

    suspend fun clearHistory() {
        spectralDao.clearAllRecords()
    }
}
