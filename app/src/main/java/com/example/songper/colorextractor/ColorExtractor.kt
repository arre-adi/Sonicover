package com.example.sonicover.colorextractor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette

class ColorExtractor {
    data class GradientColors(
        val startColor: Int,
        val midColor: Int,
        val endColor: Int,
        val isLight: Boolean
    )

    companion object {
        private const val DARKER_FACTOR = 0.5f
        private const val MID_DARKNESS_FACTOR = 0.8f
    }

    fun extractColors(bitmap: Bitmap): GradientColors {
        val palette = Palette.from(bitmap).generate()


        val dominantColor = palette.getDarkVibrantColor(
            palette.getDominantColor(Color.BLACK)
        )

        // Create mid and darker variants
        val midColor = createMidColor(dominantColor)
        val darkerColor = createDarkerColor(dominantColor)

        return GradientColors(
            startColor = dominantColor,
            midColor = midColor,
            endColor = darkerColor,
            isLight = isColorLight(dominantColor)
        )
    }




    private fun createMidColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Slightly reduce brightness for mid tone
        hsv[2] *= MID_DARKNESS_FACTOR

        return Color.HSVToColor(hsv)
    }

    private fun createDarkerColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Significantly reduce brightness for bottom color
        hsv[2] *= DARKER_FACTOR

        return Color.HSVToColor(hsv)
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }
}