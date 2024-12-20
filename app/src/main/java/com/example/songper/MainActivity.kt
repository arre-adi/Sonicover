package com.example.songper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.songper.ViewModel.MainViewModel
import com.example.songper.ui.theme.SongperTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setContext(this)

        setContent {
            SongperTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}
