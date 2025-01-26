package com.example.songper.Design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

fun createDesignC(
    context: Context,
    albumArt: Bitmap,
    screenWidth: Int,
    screenHeight: Int
): Bitmap {
    val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaperBitmap)

    // Create a geometric pattern background
    val patternPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    val gridSize = 50f
    for (x in 0 until screenWidth step gridSize.toInt()) {
        for (y in 0 until screenHeight step gridSize.toInt()) {
            // Triangles and lines
            canvas.drawLine(x.toFloat(), y.toFloat(), x + gridSize, y.toFloat(), patternPaint)
            canvas.drawLine(x.toFloat(), y.toFloat(), x + gridSize / 2, y + gridSize, patternPaint)
        }
    }

    // Draw album art with a subtle shadow
    val artSize = (screenWidth * 0.4).toInt()
    val albumLeft = (screenWidth - artSize) / 2f
    val albumTop = (screenHeight - artSize) / 2f

    val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)

    // Draw shadow
    val shadowPaint = Paint().apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        color = Color.DKGRAY
        alpha = 100
    }
    canvas.drawRect(
        albumLeft - 5f,
        albumTop - 5f,
        albumLeft + artSize + 5f,
        albumTop + artSize + 5f,
        shadowPaint
    )

    // Draw album art
    canvas.drawBitmap(scaledAlbumArt, albumLeft, albumTop, null)

    scaledAlbumArt.recycle()

    return wallpaperBitmap
}