package com.example.songper.viewModel

import CurrentlyPlaying
import UserProfile
import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object PermissionUtil {
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        arrayOf(
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun hasPermissions(context: Context): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldShowRationale(activity: Activity): Boolean {
        return requiredPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }
}

object WallpaperUtil {
    suspend fun setWallpaperFromUrl(context: Context, imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)

                // Create a temporary file in the cache directory
                val cacheFile = File(context.cacheDir, "temp_wallpaper.jpg")

                // Download and save the image
                val response = URL(imageUrl).openStream()
                response.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Set the wallpaper
                cacheFile.inputStream().use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    wallpaperManager.setBitmap(bitmap)
                }

                // Clean up the temporary file
                cacheFile.delete()

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
    var errorMessage by mutableStateOf<String?>(null)
    var isSettingWallpaper by mutableStateOf(false)
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
            WallpaperUtil.setWallpaperFromUrl(context, imageUrl)
                .onSuccess {
                    errorMessage = null // Clear any previous error messages
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

