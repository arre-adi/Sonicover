package com.example.songper.Design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.songper.viewModel.WallpaperDesigns.createCircularBitmap
import kotlin.random.Random

fun createDesignB(
    context: Context,
    albumArt: Bitmap,
    screenWidth: Int,
    screenHeight: Int
): Bitmap {
    val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaperBitmap)

    // Fill with a light background
    canvas.drawColor(Color.WHITE)

    // Create red polka dots
    val dotPaint = Paint().apply {
        color = Color.argb(100, 255, 0, 0)  // Translucent red
        style = Paint.Style.FILL
    }

    val dotSizes = listOf(20f, 30f, 40f, 50f)
    val random = Random(System.currentTimeMillis())

    repeat(50) {
        val x = random.nextFloat() * screenWidth
        val y = random.nextFloat() * screenHeight
        val size = dotSizes[random.nextInt(dotSizes.size)]
        canvas.drawCircle(x, y, size, dotPaint)
    }

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