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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.songper.viewModel.WallpaperDesigns.createDesignA
import java.net.URL
import java.util.concurrent.TimeUnit


    object WallpaperUtil {
        val currentSongName = SpotifyViewModel.currentlyPlaying ?: "Unknown Song"

        fun drawShapes(
            canvas: Canvas,
            albumLeft: Float,
            albumTop: Float,
            artSize: Int,
            shapeType: String,
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int,
            currentSongName: String
        ) {
            // Center of the song cover
            val centerX = screenWidth / 2f
            val centerY = screenHeight / 2f

            // Paint for text
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            // Draw concentric text
            val radiusIncrement = 60
            var radius = artSize / 2 + radiusIncrement

            repeat(8 ) { circleIndex ->
                val text = currentSongName
                val charArray = text.toCharArray()
                val angleIncrement = 360f / charArray.size

                for (i in charArray.indices) {
                    val angle = Math.toRadians(i * angleIncrement.toDouble())
                    val x = centerX + radius * Math.cos(angle).toFloat()
                    val y = centerY + radius * Math.sin(angle).toFloat()

                    // Draw text vertically
                    canvas.drawText(charArray[i].toString(), x, y, paint)
                }

                radius += radiusIncrement
            }

            // Draw song cover (centered)
            canvas.drawBitmap(
                albumArt,
                centerX - albumArt.width / 2f,
                centerY - albumArt.height / 2f,
                null
            )
        }




        fun getDominantColor(bitmap: Bitmap): Int {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val colorMap = HashMap<Int, Int>()
        val width = resizedBitmap.width
        val height = resizedBitmap.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = resizedBitmap.getPixel(x, y)
                if (Color.alpha(color) < 128) continue
                colorMap[color] = colorMap.getOrDefault(color, 0) + 1
            }
        }

        resizedBitmap.recycle()
        return colorMap.maxByOrNull { it.value }?.key ?: Color.BLACK
    }

        private suspend fun createCustomWallpaperBitmap(
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int,
            shapeType: String? = null
        ): Bitmap {
            val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(wallpaperBitmap)

            // Draw background with dominant color
            val backgroundColor = getDominantColor(albumArt)
            canvas.drawColor(backgroundColor)

            // Calculate sizes
            val glassRectHeight = (screenHeight * 0.66).toInt()
            val glassRectWidth = (screenWidth * 0.5).toInt()

            val artSize = (screenWidth * 0.4).toInt()

            // Calculate positions
            val glassRectLeft = (screenWidth - glassRectWidth) / 2f
            val glassRectTop = (screenHeight - glassRectHeight) / 2f
            val albumLeft = (screenWidth - artSize) / 2f
            val albumTop = (screenHeight - artSize) / 2f

            // Create glassmorphism effect - Fullscreen
            val glassRect = RectF(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())

            // 1. Draw blur layer
            val blurPaint = Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(35f, BlurMaskFilter.Blur.NORMAL)
                color = Color.WHITE
                alpha = 77 // 30% opacity
            }
            canvas.drawRoundRect(glassRect, 40f, 40f, blurPaint)

            // 2. Draw glass gradient
            val gradientPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    glassRect.centerX(),
                    glassRect.centerY(),
                    glassRect.width().coerceAtLeast(glassRect.height()) * 0.96f,
                    intArrayOf(
                        Color.argb(120, 255, 255, 255), // 50% white
                        Color.argb(85, 255, 255, 255),  // 30% white
                        Color.argb(40, 255, 255, 255)   // 10% white
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(glassRect, 40f, 40f, gradientPaint)

            // 3. Draw subtle border
            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.WHITE
                alpha = 51 // 20% opacity
            }
            canvas.drawRoundRect(glassRect, 40f, 40f, borderPaint)

            // 4. Optional: Remove or modify shadow if it's causing unwanted effects
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.OUTER)
                color = Color.TRANSPARENT // Set shadow to transparent if you don't want any tint
            }
            canvas.drawRoundRect(glassRect, 40f, 40f, shadowPaint)

            // Draw the album art
            val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)
            canvas.drawBitmap(scaledAlbumArt, albumLeft, albumTop, null)
            shapeType?.let {
                val currentSongName = SpotifyViewModel.currentlyPlaying ?: "Unknown Song"
                drawShapes(canvas, albumLeft, albumTop, artSize, it, context, albumArt, screenWidth, screenHeight, currentSongName)
            }

            scaledAlbumArt.recycle()
            return wallpaperBitmap
        }

    // Rest of the utility functions remain the same

    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }


    suspend fun setCustomWallpaperFromUrl(
        context: Context,
        imageUrl: String,
        shapeType: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val displayMetrics = context.resources.displayMetrics
            val albumArtBitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())
            val wallpaperBitmap = createCustomWallpaperBitmap(
                context,
                albumArtBitmap,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                shapeType
            )
            wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK)
            albumArtBitmap.recycle()
            wallpaperBitmap.recycle()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

    object WallpaperDesigns {
    private fun getDominantColor(bitmap: Bitmap): Int {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val colorMap = HashMap<Int, Int>()

        for (y in 0 until resizedBitmap.height) {
            for (x in 0 until resizedBitmap.width) {
                val color = resizedBitmap.getPixel(x, y)
                if (Color.alpha(color) < 128) continue
                colorMap[color] = colorMap.getOrDefault(color, 0) + 1
            }
        }

        resizedBitmap.recycle()
        return colorMap.maxByOrNull { it.value }?.key ?: Color.BLACK
    }

    private fun getComplementaryColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180) % 360
        return Color.HSVToColor(hsv)
    }

        fun createDesignA(
            context: Context,
            albumArt: Bitmap,
            screenWidth: Int,
            screenHeight: Int,
            canvas: Canvas,
            albumLeft: Float,
            albumTop: Float,
            artSize: Int,
            songName: String
        ) {
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                textSize = 20f
                color = Color.WHITE
            }

            val radiusIncrement = 40f
            var currentRadius = artSize / 2 + 20f

            for (i in 1..3) {
                canvas.drawCircle(
                    albumLeft + artSize / 2,
                    albumTop + artSize / 2,
                    currentRadius,
                    paint
                )

                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    songName,
                    albumLeft + artSize / 2,
                    albumTop + artSize / 2 - currentRadius,
                    paint
                )

                currentRadius += radiusIncrement
            }
        }


        fun createDesignB(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int
    ): Bitmap {
        val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaperBitmap)

        canvas.drawColor(Color.BLACK)

        val artSize = (screenWidth * 0.4).toInt()
        val albumLeft = (screenWidth - artSize) / 2f
        val albumTop = (screenHeight - artSize) / 2f

        applyDarkGlassmorphism(canvas, screenWidth, screenHeight)

        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)
        val circularAlbumArt = createCircularBitmap(scaledAlbumArt)
        canvas.drawBitmap(circularAlbumArt, albumLeft, albumTop, null)

        scaledAlbumArt.recycle()
        circularAlbumArt.recycle()
        return wallpaperBitmap
    }

    fun createDesignC(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int
    ): Bitmap {
        val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaperBitmap)

        val dominantColor = getDominantColor(albumArt)
        val complementaryColor = getComplementaryColor(dominantColor)

        val gradient = LinearGradient(
            0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(),
            dominantColor, complementaryColor,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = gradient
        }
        canvas.drawPaint(paint)

        val artSize = (screenWidth * 0.4).toInt()
        val albumLeft = (screenWidth - artSize) / 2f
        val albumTop = (screenHeight - artSize) / 2f

        applyFloatingEffect(canvas, screenWidth, screenHeight)

        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)
        drawBitmapWithShadow(canvas, scaledAlbumArt, albumLeft, albumTop)

        scaledAlbumArt.recycle()
        return wallpaperBitmap
    }

    private fun applyGlassmorphism(canvas: Canvas, width: Int, height: Int) {
        val glassRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Blur layer
        val blurPaint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(35f, BlurMaskFilter.Blur.NORMAL)
            color = Color.WHITE
            alpha = 77
        }
        canvas.drawRoundRect(glassRect, 40f, 40f, blurPaint)

        // Glass gradient
        val gradientPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                glassRect.centerX(),
                glassRect.centerY(),
                glassRect.width().coerceAtLeast(glassRect.height()) * 0.96f,
                intArrayOf(
                    Color.argb(120, 255, 255, 255),
                    Color.argb(85, 255, 255, 255),
                    Color.argb(40, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(glassRect, 40f, 40f, gradientPaint)
    }

    private fun applyDarkGlassmorphism(canvas: Canvas, width: Int, height: Int) {
        val glassRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val darkBlurPaint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(35f, BlurMaskFilter.Blur.NORMAL)
            color = Color.BLACK
            alpha = 77
        }
        canvas.drawRoundRect(glassRect, 40f, 40f, darkBlurPaint)
    }

    private fun applyFloatingEffect(canvas: Canvas, width: Int, height: Int) {
        val glassRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val shadowPaint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.OUTER)
            color = Color.BLACK
            alpha = 40
        }
        canvas.drawRoundRect(glassRect, 40f, 40f, shadowPaint)
    }

    private fun createCircularBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        canvas.drawCircle(
            source.width / 2f,
            source.height / 2f,
            source.width.coerceAtMost(source.height) / 2f,
            paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    private fun drawBitmapWithShadow(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float) {
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            color = Color.BLACK
            alpha = 50
        }

        canvas.drawRect(
            left - 10f,
            top - 10f,
            left + bitmap.width + 10f,
            top + bitmap.height + 10f,
            shadowPaint
        )

        canvas.drawBitmap(bitmap, left, top, null)
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
                    WallpaperUtil.setCustomWallpaperFromUrl(ctx, url, designType)
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
                // Get access token from preferences
                val sharedPrefs = applicationContext.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
                val accessToken = sharedPrefs.getString("access_token", null) ?: return@withContext Result.failure()

                // Fetch currently playing track
                val response = apiService.getCurrentlyPlaying("Bearer $accessToken")

                response.item?.let { track ->
                    val albumArtUrl = track.album.images.firstOrNull()?.url

                    // Get last album art URL from preferences
                    val lastAlbumArtUrl = sharedPrefs.getString("last_album_art_url", null)

                    // Update wallpaper if album art has changed
                    if (albumArtUrl != null && albumArtUrl != lastAlbumArtUrl) {
                        WallpaperUtil.setCustomWallpaperFromUrl(applicationContext, albumArtUrl)
                            .onSuccess {
                                // Save new album art URL
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