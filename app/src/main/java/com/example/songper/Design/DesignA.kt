package com.example.songper.Design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.example.songper.viewModel.WallpaperDesigns.createCircularBitmap
import com.example.songper.viewModel.WallpaperUtil

fun createDesignA(
    context: Context,
    albumArt: Bitmap,
    screenWidth: Int,
    screenHeight: Int
): Bitmap {
    val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaperBitmap)

    // Create a subtle background gradient
    val dominantColor = WallpaperUtil.getDominantColor(albumArt)
    val lightColor = Color.argb(
        255,
        Color.red(dominantColor) + 50,
        Color.green(dominantColor) + 50,
        Color.blue(dominantColor) + 50
    )

    val gradient = LinearGradient(
        0f, 0f,
        screenWidth.toFloat(), screenHeight.toFloat(),
        dominantColor,
        lightColor,
        Shader.TileMode.CLAMP
    )
    val backgroundPaint = Paint().apply { shader = gradient }
    canvas.drawPaint(backgroundPaint)

    // Draw concentric circles
    val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
        alpha = 50
    }

    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f
    val maxRadius = screenWidth.coerceAtMost(screenHeight) / 2f

    for (i in 1..10) {
        canvas.drawCircle(centerX, centerY, maxRadius * (i / 10f), circlePaint)
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