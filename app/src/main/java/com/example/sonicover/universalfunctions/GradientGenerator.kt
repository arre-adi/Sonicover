package com.example.sonicover.universalfunctions

// GradientUtils.kt
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

data class GradientColors(
    val startColor: Int,
    val midColor: Int,
    val endColor: Int,
    val colors: IntArray = intArrayOf(startColor, midColor, endColor)
) {
    // Helper methods
    fun toIntArray(): IntArray = colors
    fun getStartColor(): Int = startColor
    fun getMidColor(): Int = midColor
    fun getEndColor(): Int = endColor
}

class GradientUtils {

    companion object {
        private const val DARKER_FACTOR = 0.5f
        private const val MID_DARKNESS_FACTOR = 0.8f
        private const val LIGHTER_FACTOR = 1.3f
        private const val MID_LIGHTER_FACTOR = 1.15f
    }

    // Create gradient from palette colors
    fun createGradientFromPalette(
        palette: AppColorPalette,
        style: GradientStyle = GradientStyle.DARK_TO_DARKER
    ): GradientColors {
        return when (style) {
            GradientStyle.DARK_TO_DARKER -> createDarkGradient(palette.dominantColor)
            GradientStyle.VIBRANT_TO_MUTED -> createVibrantToMutedGradient(palette)
            GradientStyle.LIGHT_TO_DARK -> createLightToDarkGradient(palette)
            GradientStyle.MONOCHROMATIC -> createMonochromaticGradient(palette.dominantColor)
            GradientStyle.COMPLEMENTARY -> createComplementaryGradient(palette.vibrantColor)
        }
    }

    // Your original gradient creation logic
    fun createDarkGradient(baseColor: Int): GradientColors {
        val midColor = createMidColor(baseColor)
        val darkerColor = createDarkerColor(baseColor)

        return GradientColors(
            startColor = baseColor,
            midColor = midColor,
            endColor = darkerColor
        )
    }

    fun createVibrantToMutedGradient(palette: AppColorPalette): GradientColors {
        return GradientColors(
            startColor = palette.vibrantColor,
            midColor = palette.mutedColor,
            endColor = palette.darkMutedColor
        )
    }

    fun createLightToDarkGradient(palette: AppColorPalette): GradientColors {
        return GradientColors(
            startColor = palette.lightVibrantColor,
            midColor = palette.vibrantColor,
            endColor = palette.darkVibrantColor
        )
    }

    fun createMonochromaticGradient(baseColor: Int): GradientColors {
        val lightColor = createLighterColor(baseColor)
        val darkColor = createDarkerColor(baseColor)

        return GradientColors(
            startColor = lightColor,
            midColor = baseColor,
            endColor = darkColor
        )
    }

    fun createComplementaryGradient(baseColor: Int): GradientColors {
        val complementary = getComplementaryColor(baseColor)
        val midColor = blendColors(baseColor, complementary, 0.5f)

        return GradientColors(
            startColor = baseColor,
            midColor = midColor,
            endColor = complementary
        )
    }

    // Create actual GradientDrawable
    fun createGradientDrawable(
        gradientColors: GradientColors,
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM,
        cornerRadius: Float = 0f
    ): GradientDrawable {
        return GradientDrawable(orientation, gradientColors.toIntArray()).apply {
            if (cornerRadius > 0) {
                setCornerRadius(cornerRadius)
            }
        }
    }

    // Color manipulation helpers
    private fun createMidColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= MID_DARKNESS_FACTOR
        return Color.HSVToColor(hsv)
    }

    private fun createDarkerColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= DARKER_FACTOR
        return Color.HSVToColor(hsv)
    }

    private fun createLighterColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * LIGHTER_FACTOR).coerceAtMost(1.0f)
        return Color.HSVToColor(hsv)
    }

    private fun getComplementaryColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180) % 360
        return Color.HSVToColor(hsv)
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * ratio + Color.red(color2) * inverseRatio).toInt()
        val g = (Color.green(color1) * ratio + Color.green(color2) * inverseRatio).toInt()
        val b = (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio).toInt()
        return Color.rgb(r, g, b)
    }
}

enum class GradientStyle {
    DARK_TO_DARKER,
    VIBRANT_TO_MUTED,
    LIGHT_TO_DARK,
    MONOCHROMATIC,
    COMPLEMENTARY
}