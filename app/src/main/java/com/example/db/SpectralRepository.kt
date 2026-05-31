package com.example.db

import kotlinx.coroutines.flow.Flow

class SpectralRepository(private val dao: SpectralDao) {
    val allRecords: Flow<List<SpectralRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: SpectralRecord) {
        dao.insertRecord(record)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
