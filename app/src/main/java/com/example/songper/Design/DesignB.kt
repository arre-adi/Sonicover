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
import kotlin.random.Random

fun createDesignB(
    context: Context,
    albumArt: Bitmap,
    screenWidth: Int,
    screenHeight: Int
): Bitmap {
    val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaperBitmap)

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