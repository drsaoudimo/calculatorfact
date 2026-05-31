package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.SpectralRepository
import com.example.ui.screens.SpectralDashboard
import com.example.viewmodel.SpectralViewModel
import com.example.viewmodel.SpectralViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database locally
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "sfi_spectral_database"
        )
        .fallbackToDestructiveMigration()
        .build()
        
        val repository = SpectralRepository(db.spectralDao())
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: SpectralViewModel = viewModel(
                        factory = SpectralViewModelFactory(repository)
                    )
                    SpectralDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
