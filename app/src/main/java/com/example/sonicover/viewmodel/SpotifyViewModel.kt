package com.example.sonicover.viewmodel

import CurrentlyPlaying
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sonicover.spotifycalls.SpotifyApiClient
import com.example.sonicover.spotifycalls.SpotifyForegroundService
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

open class SpotifyViewModel : ViewModel() {
    var isLoggedIn by mutableStateOf(false)
        private set
    open var userName by mutableStateOf<String?>(null)
        set
    open var nowPlaying by mutableStateOf<String?>(null)
        set
    var albumArtUrl by mutableStateOf<String?>(null)
        private set
    var selectedDesign by mutableStateOf<String?>(null)
        private set
    private var errorMessage by mutableStateOf<String?>(null)
    private var isSettingWallpaper by mutableStateOf(false)
        private set

    private var accessToken: String? = null
    private var pollingJob: Job? = null
    private var lastAlbumArtUrl: String? = null
    private var context: Context? = null

    companion object {
        var currentlyPlaying by mutableStateOf<CurrentlyPlaying?>(null)
            private set

        private const val DEFAULT_POLLING_INTERVAL = 100L // 3 seconds
    }

    private var pollingInterval = DEFAULT_POLLING_INTERVAL

    open fun updateWallpaper(designType: String) {
        selectedDesign = designType
        context?.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("selected_design", designType)
            ?.apply()

        // Trigger wallpaper update immediately
        updateWallpaperWithCurrentTrack()
    }

    private fun updateWallpaperWithCurrentTrack() {
        viewModelScope.launch {
            albumArtUrl?.let { url ->
                context?.let { ctx ->
                    try {
                        val songName = currentlyPlaying?.item?.name ?: "Unknown Song"
                        WallpaperUtil.setCustomWallpaperFromUrl(ctx, url, selectedDesign, songName)
                    } catch (e: Exception) {
                        errorMessage = "Failed to set wallpaper: ${e.message}"
                    }
                }
            }
        }
    }

    private fun startPollingCurrentTrack() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    accessToken?.let { token ->
                        val response = SpotifyApiClient.apiService.getCurrentlyPlaying("Bearer $token")
                        response.item?.let { track ->
                            currentlyPlaying = CurrentlyPlaying(track, response.is_playing)
                            albumArtUrl = track.album.images.firstOrNull()?.url

                            context?.let { ctx ->
                                albumArtUrl?.let { url ->
                                    if (url != lastAlbumArtUrl) {
                                        updateWallpaperWithCurrentTrack()
                                        lastAlbumArtUrl = url
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to fetch current track: ${e.message}"
                }
                delay(pollingInterval)
            }
        }
    }

    open fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    fun setPollingInterval(intervalMs: Long) {
        pollingInterval = intervalMs
        // Restart polling with new interval
        if (isLoggedIn) {
            startPollingCurrentTrack()
        }
    }

    private fun startBackgroundWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SpotifyBackgroundWorker>(
            pollingInterval, TimeUnit.MILLISECONDS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SpotifyBackgroundWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleLoginResult(context: Context, response: AuthorizationResponse) {
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                viewModelScope.launch {
                    try {
                        // Save token to preferences for background worker
                        context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("access_token", response.accessToken)
                            .apply()

                        fetchUserData(response.accessToken)
                        startForegroundService(context)
                    } catch (e: Exception) {
                        errorMessage = "Login failed: ${e.message}"
                    }
                }
            }
            AuthorizationResponse.Type.ERROR -> {
                errorMessage = "Login failed: ${response.error}"
            }
            else -> {
                errorMessage = "Login canceled"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService(context: Context) {
        val serviceIntent = Intent(context, SpotifyForegroundService::class.java).apply {
            action = SpotifyForegroundService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }

    private fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, SpotifyForegroundService::class.java).apply {
            action = SpotifyForegroundService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    private suspend fun fetchUserData(token: String) {
        try {
            accessToken = token
            val profile = SpotifyApiClient.apiService.getUserProfile("Bearer $token")
            userName = profile.display_name ?: profile.id
            isLoggedIn = true
            startPollingCurrentTrack()
        } catch (e: Exception) {
            errorMessage = "Failed to fetch user data: ${e.message}"
            isLoggedIn = false
        }
    }


    suspend fun updateWallpaper(context: Context, imageUrl: String) {
        isSettingWallpaper = true
        try {
            WallpaperUtil.setCustomWallpaperFromUrl(context, imageUrl)
                .onSuccess {
                    errorMessage = null
                }
                .onFailure {
                    errorMessage = "Failed to set wallpaper: ${it.message}"
                }
        } finally {
            isSettingWallpaper = false
        }
    }

    fun onLogout() {
        pollingJob?.cancel()
        accessToken = null
        isLoggedIn = false
        userName = null
        currentlyPlaying = null
        albumArtUrl = null
        lastAlbumArtUrl = null

        context?.let { ctx ->
            stopForegroundService(ctx)
            // Clear preferences
            ctx.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}