package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spectral_records")
data class SpectralRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputN: String,
    val factorsText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val frobeniusNorm: Double,
    val dimension: Int,
    val betti0: Int,
    val betti1: Int,
    val betti2: Int,
    val academicBriefing: String
)
