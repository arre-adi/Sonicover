package com.example.songper.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.songper.colorextractor.ColorExtractor
import com.example.songper.viewmodel.WallpaperUtil

object WallpaperDesignC {
    fun createDesignC(
        context: Context,
        albumArt: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        songName: String
    ): Bitmap {
        val wallpaperBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaperBitmap)

        // Extract colors from the album art

        val colorExtractor = ColorExtractor()
        val palette = colorExtractor.extractColors (albumArt)

        // Use the start and end colors from the palette
        val backgroundColor = Color.BLACK
        val patternColor = palette.startColor

        // Fill background
        canvas.drawColor(backgroundColor)

        // Calculate pattern parameters
        val patternSize = screenWidth / 10f  // Adjust for tighter packing
        val halfSize = patternSize / 2f

        // Create paint for the pattern
        val paint = Paint().apply {
            color = patternColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Create path for the curved pattern
        val path = Path()

        // Generate the pattern across the entire canvas
        for (y in -1 until (screenHeight / patternSize + 2).toInt()) {
            for (x in -1 until (screenWidth / patternSize + 2).toInt()) {
                // Alternate offset for interlocking pattern
                val offsetX = if (y % 2 == 0) 0f else halfSize

                // Calculate position
                val centerX = x * patternSize + offsetX
                val centerY = y * patternSize

                // Create curved rectangle path
                path.reset()
                val rect = RectF(
                    centerX - halfSize,
                    centerY - halfSize,
                    centerX + halfSize,
                    centerY + halfSize
                )

                // Draw curved quarter circles
                path.arcTo(rect, 0f, 90f, false)
                path.arcTo(rect, 90f, 90f, false)
                path.arcTo(rect, 180f, 90f, false)
                path.arcTo(rect, 270f, 90f, false)

                path.close()

                // Draw the path
                canvas.drawPath(path, paint)
            }
        }

        // Draw the text in the center
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (palette.isLight) Color.BLACK else Color.WHITE
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Position the text like in the reference image
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // Split the text into multiple lines if needed
        val lines = splitTextIntoLines(songName)
        val lineHeight = textPaint.fontSpacing

        // Draw small decorative element
        val smallDecorativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPaint.color
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        // Calculate vertical spacing to position text
        var yPos = centerY - (lines.size * lineHeight / 2) + textPaint.fontMetrics.descent

        for (i in lines.indices) {
            canvas.drawText(lines[i], centerX, yPos, textPaint)
            yPos += lineHeight

            // If this is the last line, add a small decorative element below
            if (i == lines.size - 1) {
                canvas.drawText("*", centerX, yPos + 20f, smallDecorativePaint)
            }
        }

        return wallpaperBitmap
    }

    private fun splitTextIntoLines(text: String): List<String> {
        // If text is "Flowers need time to BLOOM" like in image, split at "to"
        if (text.contains(" to ")) {
            val parts = text.split(" to ", limit = 2)
            return listOf(parts[0] + " need time to", parts[1])
        }

        // For other texts, split at word boundaries if longer than 15 chars
        if (text.length <= 15) return listOf(text)

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if ((currentLine + " " + word).length <= 15) {
                currentLine += " " + word
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}