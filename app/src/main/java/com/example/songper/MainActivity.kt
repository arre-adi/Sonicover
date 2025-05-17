package com.example.songper

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.songper.screens.App
import com.example.songper.viewmodel.SpotifyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions once at startup
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.SET_WALLPAPER,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                123
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.SET_WALLPAPER),
                123
            )
        }

        setContent {
            MaterialTheme {
                App(SpotifyViewModel() )
            }
        }
    }
}