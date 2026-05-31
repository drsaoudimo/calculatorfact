package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spectral_records")
data class SpectralRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val valueN: String,
    val factorsText: String,
    val spectrumText: String,
    val visibilityMetric: Double,
    val isOpacityTriggered: Boolean,
    val modeName: String,
    val timestamp: Long = System.currentTimeMillis()
)
