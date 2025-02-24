package com.example.songper.Design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import com.example.songper.viewModel.WallpaperUtil
import kotlin.math.cos
import kotlin.math.sin


fun createDesignA(
    context: Context,
    albumArt: Bitmap,
    screenWidth: Int,
    screenHeight: Int,
    songName: String
): Bitmap {
    val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaperBitmap)

    // Create a subtle background gradient
    val colorExtractor = WallpaperUtil.ColorExtractor()
    val colors = colorExtractor.extractGradientColors(albumArt)
    val dominantColor = colors.startColor

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

    // Draw album art
    val artSize = (screenWidth * 0.4).toInt()
    val albumLeft = (screenWidth - artSize) / 2f
    val albumTop = (screenHeight - artSize) / 2f

    val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)
    val circularAlbumArt = WallpaperUtil.createCircularBitmap(scaledAlbumArt)

    canvas.drawBitmap(circularAlbumArt, albumLeft, albumTop, null)

    scaledAlbumArt.recycle()
    circularAlbumArt.recycle()

    // Draw circular text in a spiral pattern
    val textPaint = Paint().apply {
        color = if (colors.isLight) Color.BLACK else Color.WHITE
        textSize = 50f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f
    val initialRadius = (screenWidth.coerceAtMost(screenHeight) / 2f) * 0.5f
    val radiusIncrement = 1.2f

    val totalRows = 6

    for (row in 0 until totalRows) {
        val textRadius = initialRadius * Math.pow(radiusIncrement.toDouble(), row.toDouble())
        val angleStep = 360f / (songName.length + row * 2) // Increase spacing as row increases

        for (i in 0 until songName.length + row * 2) {
            val angle = i * angleStep
            val x = centerX + textRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = centerY + textRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            // Calculate the rotation angle for the text
            val rotationAngle = angle + 90f // Rotate text to follow the curve
            canvas.save()
            canvas.rotate(rotationAngle, x.toFloat(), y.toFloat())
            canvas.drawText(songName[i % songName.length].toString(), x.toFloat(),
                y.toFloat(), textPaint)
            canvas.restore()
        }
    }

    return wallpaperBitmap
}
