package com.example.songper.Model

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        // Get and set the dominant color as background
        val backgroundColor = getDominantColor(albumArt)
        canvas.drawColor(backgroundColor)

        // Calculate complementary color
        val complementaryColor = getComplementaryColor(backgroundColor)


        // Calculate dimensions for centered square album art
        val artSize = minOf(screenWidth, screenHeight) / 2 // Album art takes up half the smaller screen dimension
        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)

        // Calculate positioning to center the album art
        val left = (screenWidth - artSize) / 2f
        val top = (screenHeight - artSize) / 2f

        // Draw the album art
        canvas.drawBitmap(scaledAlbumArt, left, top, null)

        // Clean up
        scaledAlbumArt.recycle()

        return wallpaperBitmap
    }

    // Function to get the complementary color
    private fun getComplementaryColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180) % 360 // Shift the hue by 180 degrees
        return Color.HSVToColor(hsv)
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

                // Set the wallpaper for lock screen only using FLAG_LOCK
                wallpaperManager.setBitmap(wallpaperBitmap, null, true, WallpaperManager.FLAG_LOCK)

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