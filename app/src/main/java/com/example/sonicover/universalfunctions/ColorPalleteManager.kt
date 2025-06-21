package com.example.sonicover.universalfunctions

// ColorPaletteManager.kt
import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import androidx.core.graphics.toColorInt

data class AppColorPalette(
    val dominantColor: Int,
    val vibrantColor: Int,
    val darkVibrantColor: Int,
    val lightVibrantColor: Int,
    val mutedColor: Int,
    val darkMutedColor: Int,
    val lightMutedColor: Int,
    val isLight: Boolean
) {
    // Helper methods for easy access
    fun getPrimaryColor(): Int = dominantColor
    fun getAccentColor(): Int = vibrantColor
    fun getBackgroundColor(): Int = if (isLight) lightMutedColor else darkMutedColor
    fun getTextColor(): Int = if (isLight) Color.BLACK else Color.WHITE
}

class ColorPaletteManager {

    companion object {
        // Default colors as fallbacks
        private val DEFAULT_DARK_COLORS = mapOf(
            "dominant" to "#2C2C2C".toColorInt(),
            "vibrant" to "#FF6B35".toColorInt(),
            "darkVibrant" to "#CC5429".toColorInt(),
            "lightVibrant" to "#FF8C69".toColorInt(),
            "muted" to "#8A8A8A".toColorInt(),
            "darkMuted" to "#4A4A4A".toColorInt(),
            "lightMuted" to "#CCCCCC".toColorInt()
        )

        private val DEFAULT_LIGHT_COLORS = mapOf(
            "dominant" to "#F5F5F5".toColorInt(),
            "vibrant" to "#FF6B35".toColorInt(),
            "darkVibrant" to "#CC5429".toColorInt(),
            "lightVibrant" to "#FF8C69".toColorInt(),
            "muted" to "#CCCCCC".toColorInt(),
            "darkMuted" to "#999999".toColorInt(),
            "lightMuted" to "#F0F0F0".toColorInt()
        )
    }

    fun extractColorsFromImage(bitmap: Bitmap): AppColorPalette {
        val palette = Palette.from(bitmap).generate()

        val dominantColor = palette.getDominantColor(DEFAULT_DARK_COLORS["dominant"]!!)
        val vibrantColor = palette.getVibrantColor(DEFAULT_DARK_COLORS["vibrant"]!!)
        val darkVibrantColor = palette.getDarkVibrantColor(DEFAULT_DARK_COLORS["darkVibrant"]!!)
        val lightVibrantColor = palette.getLightVibrantColor(DEFAULT_LIGHT_COLORS["lightVibrant"]!!)
        val mutedColor = palette.getMutedColor(DEFAULT_DARK_COLORS["muted"]!!)
        val darkMutedColor = palette.getDarkMutedColor(DEFAULT_DARK_COLORS["darkMuted"]!!)
        val lightMutedColor = palette.getLightMutedColor(DEFAULT_LIGHT_COLORS["lightMuted"]!!)

        val isLight = isColorLight(dominantColor)

        return AppColorPalette(
            dominantColor = dominantColor,
            vibrantColor = vibrantColor,
            darkVibrantColor = darkVibrantColor,
            lightVibrantColor = lightVibrantColor,
            mutedColor = mutedColor,
            darkMutedColor = darkMutedColor,
            lightMutedColor = lightMutedColor,
            isLight = isLight
        )
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }
}