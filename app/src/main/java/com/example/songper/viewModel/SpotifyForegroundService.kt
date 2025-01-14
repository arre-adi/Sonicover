package com.example.songper.viewModel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.songper.Model.WallpaperUtil
import kotlinx.coroutines.*

class SpotifyForegroundService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spotify Wallpaper")
                .setContentText("Updating wallpaper based on current song")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPolling()
            ACTION_STOP -> stopPolling()
        }
        return START_STICKY
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val sharedPrefs = getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
                    val accessToken = sharedPrefs.getString("access_token", null)

                    if (accessToken != null) {
                        val response = SpotifyApiClient.apiService.getCurrentlyPlaying("Bearer $accessToken")
                        response.item?.let { track ->
                            val albumArtUrl = track.album.images.firstOrNull()?.url
                            val lastAlbumArtUrl = sharedPrefs.getString("last_album_art_url", null)

                            if (albumArtUrl != null && albumArtUrl != lastAlbumArtUrl) {
                                WallpaperUtil.setCustomWallpaperFromUrl(applicationContext, albumArtUrl)
                                    .onSuccess {
                                        sharedPrefs.edit()
                                            .putString("last_album_art_url", albumArtUrl)
                                            .apply()
                                    }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error if needed
                }
                delay(30000) // Poll every 30 seconds
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        stopForeground(true)
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Spotify Wallpaper Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "spotify_wallpaper_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"
    }
}