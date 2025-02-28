package com.example.songper.Design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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

    // Define text paint based on background color
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (colors.isLight) Color.BLACK else Color.WHITE
        textSize = 50f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val innerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (colors.isLight) Color.BLACK else Color.WHITE
        textSize = 45f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Calculate center points
    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f
    val baseRadius = artSize / 2f // Base radius is half the album art size

    // Define the radii for concentric circles
    val circleRadii = listOf(
        baseRadius + 60, baseRadius + 150, baseRadius + 260,
        baseRadius + 380, baseRadius + 500, baseRadius + 610,
        baseRadius + 720, baseRadius + 810
    )

    // Draw concentric circles of text
    for (i in circleRadii.indices) {
        val currentRadius = circleRadii[i]
        val paint = if (i == 0) textPaint else innerTextPaint

        // Calculate optimal repetitions
        val optimalRepetitions = calculateOptimalRepetitions(currentRadius, songName, paint)

        // Draw the text
        drawCircularText(canvas, centerX, centerY, currentRadius, songName, optimalRepetitions, paint)
    }

    // Clean up
    scaledAlbumArt.recycle()
    circularAlbumArt.recycle()

    return wallpaperBitmap
}

private fun calculateOptimalRepetitions(radius: Float, text: String, paint: Paint): Int {
    // Calculate the circumference of the circle
    val circumference = 1.3 * Math.PI.toFloat() * radius

    // Calculate how much space a single copy of the text would take
    val textWidth = paint.measureText("$text  ")

    // Calculate how many times the text would fit around the circle
    val naturalRepetitions = (circumference / textWidth).toInt()

    // Limit to natural repetitions (never more than needed to wrap the circle)
    // Ensure at least one repetition
    return maxOf(1, naturalRepetitions)
}

private fun drawCircularText(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    radius: Float,
    text: String,
    repetitions: Int,
    paint: Paint
) {
    // Create a path for the circular text
    val path = Path().apply {
        addCircle(centerX, centerY, radius, Path.Direction.CW)
    }

    // Create repeated text
    val repeatedText = StringBuilder()
    for (i in 0 until repetitions) {
        repeatedText.append(text)
        if (i < repetitions - 1) {
            repeatedText.append(" ")
        }
    }

    val repeatedTextString = repeatedText.toString()
    val repeatedTextLength = repeatedTextString.length

    // Calculate the circumference of the circle
    val circumference = 2 * Math.PI.toFloat() * radius

    // Calculate spacing for the characters
    val charSpacing = circumference / repeatedTextLength

    // Calculate offset to start from the top position
    val startOffset = -90f * radius * Math.PI.toFloat() / 90f

    // Draw each character of the repeated text
    for (i in 0 until repeatedTextLength) {
        val char = repeatedTextString[i].toString()

        // Calculate the position along the path
        val offset = startOffset + (i * charSpacing)

        // Draw the character
        canvas.drawTextOnPath(char, path, offset, 0f, paint)
    }
}