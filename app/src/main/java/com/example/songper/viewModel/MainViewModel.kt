package com.example.songper.viewModel

import CurrentlyPlaying
import UserProfile
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.Retrofit
import androidx.lifecycle.viewModelScope
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.net.URL


object WallpaperUtil {

    private fun getDominantColor(bitmap: Bitmap): Int {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 24, 24, true) // Downscale for efficiency
        val colorMap = HashMap<Int, Int>() // Map to store color frequencies
        val width = resizedBitmap.width
        val height = resizedBitmap.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = resizedBitmap.getPixel(x, y)
                if (Color.alpha(color) < 128) continue // Ignore transparent pixels

                // Count the color occurrences
                colorMap[color] = colorMap.getOrDefault(color, 0) + 1
            }
        }

        resizedBitmap.recycle()

        // Find the most frequent color
        return colorMap.maxByOrNull { it.value }?.key ?: Color.BLACK // Default to black if no color found
    }




    private fun createCustomWallpaperBitmap(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int
    ): Bitmap {
        // Create a bitmap matching screen dimensions
        val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaperBitmap)

        // Get and set dominant color as background
        val backgroundColor = getDominantColor(albumArt)
        canvas.drawColor(backgroundColor)

        // Calculate dimensions for centered square album art
        val artSize = minOf(screenWidth, screenHeight) / 2 // Make art take up half the smaller screen dimension
        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)

        // Calculate positioning to center the album art
        val left = (screenWidth - artSize) / 2f
        val top = (screenHeight - artSize) / 2f

        // Draw the album art
        canvas.drawBitmap(scaledAlbumArt, left, top, null)

        // Calculate if we need light or dark text based on background color
        val shouldUseLightText = isDarkColor(backgroundColor)
        val textColor = if (shouldUseLightText) Color.WHITE else Color.BLACK

        // Set up text paint for artist name
        val textPaint = Paint().apply {
            color = textColor
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }


        return wallpaperBitmap
    }

    // Helper function to determine if we should use light text
    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    suspend fun setCustomWallpaperFromUrl(
        context: Context,
        imageUrl: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)

                // Get screen dimensions
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                // Download the album art
                val albumArtBitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())

                // Create custom wallpaper bitmap
                val wallpaperBitmap = createCustomWallpaperBitmap(
                    context,
                    albumArtBitmap,
                    screenWidth,
                    screenHeight
                )

                // Set the wallpaper
                wallpaperManager.setBitmap(wallpaperBitmap)

                // Clean up
                albumArtBitmap.recycle()
                wallpaperBitmap.recycle()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}


// API Client
object SpotifyApiClient {
    private const val BASE_URL = "https://api.spotify.com/v1/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SpotifyApiService = retrofit.create(SpotifyApiService::class.java)
}

// API Interface
interface SpotifyApiService {
    @GET("me")
    suspend fun getUserProfile(
        @Header("Authorization") accessToken: String
    ): UserProfile

    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(
        @Header("Authorization") accessToken: String
    ): CurrentlyPlaying
}

// ViewModel
class SpotifyViewModel : ViewModel() {
    var isLoggedIn by mutableStateOf(false)
        private set
    var userName by mutableStateOf<String?>(null)
        private set
    var currentlyPlaying by mutableStateOf<String?>(null)
        private set
    var albumArtUrl by mutableStateOf<String?>(null)
        private set
    private var errorMessage by mutableStateOf<String?>(null)
    private var isSettingWallpaper by mutableStateOf(false)
        private set

    private var accessToken: String? = null
    private var pollingJob: Job? = null
    private var lastAlbumArtUrl: String? = null
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    fun handleLoginResult(response: AuthorizationResponse) {
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                viewModelScope.launch {
                    try {
                        fetchUserData(response.accessToken)
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

    private fun startPollingCurrentTrack() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    accessToken?.let { token ->
                        val response = SpotifyApiClient.apiService.getCurrentlyPlaying("Bearer $token")
                        response.item?.let { track ->
                            currentlyPlaying = "${track.name} by ${track.artists.joinToString { it.name }}"
                            albumArtUrl = track.album.images.firstOrNull()?.url

                            // Check if album art has changed
                            if (albumArtUrl != lastAlbumArtUrl) {
                                albumArtUrl?.let { url ->
                                    context?.let { ctx ->
                                        updateWallpaper(ctx, url)
                                    }
                                }
                                lastAlbumArtUrl = albumArtUrl
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to fetch current track: ${e.message}"
                }
                delay(3000) // Poll every 5 seconds
            }
        }
    }

    private suspend fun updateWallpaper(context: Context, imageUrl: String) {
        isSettingWallpaper = true
        try {
            // Extract artist name from currently playing
            val artistName = currentlyPlaying?.split(" by ")?.lastOrNull() ?: "Unknown Artist"

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
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

