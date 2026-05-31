package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.SpectralDashboard
import com.example.ui.theme.SFISpectralLabTheme
import com.example.viewmodel.SpectralViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SpectralViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SFISpectralLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF0C0F14)
                ) {
                    SpectralDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
