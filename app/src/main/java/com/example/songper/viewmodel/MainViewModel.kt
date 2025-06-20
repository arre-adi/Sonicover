package com.example.sonicover.viewmodel

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sonicover.colorextractor.ColorExtractor
import com.example.sonicover.spotifycalls.SpotifyApiClient
import java.net.URL


object WallpaperUtil {

        data class GradientColors(
            val startColor: Int,
            val endColor: Int,
            val isLight: Boolean
        )




        suspend fun setCustomWallpaperFromUrl(
            context: Context,
            imageUrl: String,
            designType: String? = null,
            songName: String? = null
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
                        displayMetrics.heightPixels,
                        songName ?: "Unknown Song"
                    )



                "B" -> {
                    Log.d("WallpaperUtil", "Creating Design B wallpaper")
                    WallpaperDesigns.createDesignB(
                        context,
                        albumArtBitmap,
                        displayMetrics.widthPixels,
                        displayMetrics.heightPixels,
                        songName ?: "Unknown Song"
                    )
                }
                    else -> createDefaultWallpaperBitmap(
                        albumArtBitmap,
                        displayMetrics.widthPixels,
                        displayMetrics.heightPixels
                    )
                }


            Log.d("WallpaperUtil", "Wallpaper bitmap created - Size: ${wallpaperBitmap.width}x${wallpaperBitmap.height}")

            wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK)
            Log.d("WallpaperUtil", "Wallpaper set successfully")

            albumArtBitmap.recycle()
            wallpaperBitmap.recycle()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WallpaperUtil", "Error setting wallpaper", e)
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
        val colorExtractor = ColorExtractor()
        val colors = colorExtractor.extractColors (albumArt)

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

    fun createCircularBitmap(source: Bitmap): Bitmap {
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
        screenHeight: Int,
        songName: String
    ): Bitmap = com.example.sonicover.design.createDesignA(
        context,
        albumArt,
        screenWidth,
        screenHeight,
        songName
    )

    fun createDesignB(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        songName: String
    ): Bitmap = com.example.sonicover.design.WallpaperDesignB.createDesignB(
        context,
        albumArt,
        screenWidth,
        screenHeight,
        songName
    )

    fun createDesignC(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        songName: String
    ): Bitmap = com.example.sonicover.design.WallpaperDesignC.createDesignC(
        context,
        albumArt,
        screenWidth,
        screenHeight,
        songName
    )
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
                val selectedDesign = sharedPrefs.getString("selected_design", "default") ?: "default"

                val response = apiService.getCurrentlyPlaying("Bearer $accessToken")

                response.item?.let { track ->
                    val albumArtUrl = track.album.images.firstOrNull()?.url
                    val lastAlbumArtUrl = sharedPrefs.getString("last_album_art_url", null)

                    if (albumArtUrl != null && albumArtUrl != lastAlbumArtUrl) {
                        val songName = track.name
                        WallpaperUtil.setCustomWallpaperFromUrl(applicationContext, albumArtUrl, selectedDesign, songName)
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
