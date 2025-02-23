package com.example.songper.viewModel

import CurrentlyPlaying
import UserProfile
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
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
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.palette.graphics.Palette
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


    object WallpaperUtil {

            class ColorExtractor {
                data class GradientColors(
                    val startColor: Int,
                    val midColor: Int,
                    val endColor: Int,
                    val isLight: Boolean
                )

                companion object {
                    private const val DARKER_FACTOR = 0.5f  // Increased darkness for bottom color
                    private const val MID_DARKNESS_FACTOR = 0.8f  // Slight darkness for middle
                }

                fun extractGradientColors(bitmap: Bitmap): GradientColors {
                    val palette = Palette.from(bitmap).generate()

                    // Get the dominant/vibrant color
                    val dominantColor = palette.getVibrantColor(
                        palette.getDominantColor(Color.BLACK)
                    )

                    // Create mid and darker variants
                    val midColor = createMidColor(dominantColor)
                    val darkerColor = createDarkerColor(dominantColor)

                    return GradientColors(
                        startColor = dominantColor,
                        midColor = midColor,
                        endColor = darkerColor,
                        isLight = isColorLight(dominantColor)
                    )
                }

                private fun createMidColor(color: Int): Int {
                    val hsv = FloatArray(3)
                    Color.colorToHSV(color, hsv)

                    // Slightly reduce brightness for mid tone
                    hsv[2] *= MID_DARKNESS_FACTOR

                    return Color.HSVToColor(hsv)
                }

                private fun createDarkerColor(color: Int): Int {
                    val hsv = FloatArray(3)
                    Color.colorToHSV(color, hsv)

                    // Significantly reduce brightness for bottom color
                    hsv[2] *= DARKER_FACTOR

                    return Color.HSVToColor(hsv)
                }

                private fun isColorLight(color: Int): Boolean {
                    val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
                    return darkness < 0.5
                }
            }


    suspend fun setCustomWallpaperFromUrl(
        context: Context,
        imageUrl: String,
        designType: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val displayMetrics = context.resources.displayMetrics
            val albumArtBitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())

            val wallpaperBitmap = when (designType) {
                "A" -> WallpaperDesigns.createDesignA(
                    context,
                    albumArtBitmap,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
                "B" -> WallpaperDesigns.createDesignB(
                    context,
                    albumArtBitmap,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
                "C" -> WallpaperDesigns.createDesignC(
                    context,
                    albumArtBitmap,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
                else -> createDefaultWallpaperBitmap(
                    albumArtBitmap,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
            }

            wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK)
            albumArtBitmap.recycle()
            wallpaperBitmap.recycle()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createDefaultWallpaperBitmap(
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int
    ): Bitmap {
        val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaperBitmap)

        // Get the three gradient colors
        val colorExtractor = WallpaperUtil.ColorExtractor()
        val colors = colorExtractor.extractGradientColors(albumArt)

        // Create three-color gradient
        val gradient = LinearGradient(
            0f, 0f, // x0, y0 (top)
            0f, screenHeight.toFloat(), // x1, y1 (bottom)
            intArrayOf(
                colors.startColor,
                colors.midColor,
                colors.endColor
            ),
            floatArrayOf(0f, 0.5f, 1f), // Position each color at 0%, 50%, and 100%
            Shader.TileMode.CLAMP
        )

        val backgroundPaint = Paint().apply { shader = gradient }
        canvas.drawPaint(backgroundPaint)



        // Draw album art
        val artSize = (screenWidth * 0.4).toInt()
        val albumLeft = (screenWidth - artSize) / 2f
        val albumTop = (screenHeight - artSize) / 2f

        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)
        val circularAlbumArt = createCircularBitmap(scaledAlbumArt)

        canvas.drawBitmap(circularAlbumArt, albumLeft, albumTop, null)

        scaledAlbumArt.recycle()
        circularAlbumArt.recycle()

        return wallpaperBitmap
    }

    private fun createCircularBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val radius = source.width.coerceAtMost(source.height) / 2f
        canvas.drawCircle(
            source.width / 2f,
            source.height / 2f,
            radius,
            paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }
}

    object WallpaperDesigns {
        fun createDesignA(
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int
        ): Bitmap = com.example.songper.Design.createDesignA(
            context,
            albumArt,
            screenWidth,
            screenHeight
        )



        fun createDesignB(
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int
        ): Bitmap = com.example.songper.Design.createDesignB(
            context,
            albumArt,
            screenWidth,
            screenHeight
        )

        fun createDesignC(
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int
        ): Bitmap = com.example.songper.Design.createDesignC(
            context,
            albumArt,
            screenWidth,
            screenHeight
        )

    public fun createCircularBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val radius = source.width.coerceAtMost(source.height) / 2f
        canvas.drawCircle(
            source.width / 2f,
            source.height / 2f,
            radius,
            paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }
}


    // ViewModel
    class SpotifyViewModel : ViewModel() {
    var isLoggedIn by mutableStateOf(false)
        private set
    var userName by mutableStateOf<String?>(null)
        private set

    var nowPlaying by mutableStateOf<String?>(null)
        private set
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

        var currentlyPlaying by mutableStateOf<String?>(null)

        private const val DEFAULT_POLLING_INTERVAL = 200L // 3 seconds
    }

    private var pollingInterval = DEFAULT_POLLING_INTERVAL


    fun updateWallpaper(designType: String) {
        selectedDesign = designType
        viewModelScope.launch {
            albumArtUrl?.let { url ->
                context?.let { ctx ->
                    try {
                        WallpaperUtil.setCustomWallpaperFromUrl(ctx, url, designType)
                        // Optional: Add success handling
                    } catch (e: Exception) {
                        errorMessage = "Failed to set wallpaper: ${e.message}"
                    }
                }
            }
        }
    }

    fun initialize(appContext: Context) {
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

                            context?.let { ctx ->
                                albumArtUrl?.let { url ->
                                    if (url != lastAlbumArtUrl) {
                                        selectedDesign?.let { design ->
                                            WallpaperUtil.setCustomWallpaperFromUrl(ctx, url, design)
                                        }
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

    class SpotifyBackgroundWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val apiService = SpotifyApiClient.apiService

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = applicationContext.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
                val accessToken = sharedPrefs.getString("access_token", null) ?: return@withContext Result.failure()

                val response = apiService.getCurrentlyPlaying("Bearer $accessToken")

                response.item?.let { track ->
                    val albumArtUrl = track.album.images.firstOrNull()?.url

                    val lastAlbumArtUrl = sharedPrefs.getString("last_album_art_url", null)

                    if (albumArtUrl != null && albumArtUrl != lastAlbumArtUrl) {
                        WallpaperUtil.setCustomWallpaperFromUrl(applicationContext, albumArtUrl)
                            .onSuccess {
                                sharedPrefs.edit().putString("last_album_art_url", albumArtUrl).apply()
                            }
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "spotify_background_work"
    }
}