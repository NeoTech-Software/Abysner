/*
 * Abysner - Dive planner
 * Copyright (C) 2025 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component.core

import androidx.annotation.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.neotech.app.abysner.presentation.theme.LocalIsDarkTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun Color.preMixedWith(color: Color): Color {
    val colorInSameColorSpace = if(colorSpace != color.colorSpace) {
        color.convert(colorSpace)
    } else {
        color
    }
    val rPreMixed = (red * alpha) * 0.5f + (colorInSameColorSpace.red * colorInSameColorSpace.alpha) * 0.5f
    val gPreMixed = (green * alpha) * 0.5f + (colorInSameColorSpace.green * colorInSameColorSpace.alpha) * 0.5f
    val bPreMixed = (blue * alpha) * 0.5f + (colorInSameColorSpace.blue * colorInSameColorSpace.alpha) * 0.5f
    return Color(rPreMixed, gPreMixed, bPreMixed, alpha = 1f, colorSpace)
}

/**
 * Retains the hue and saturation of this color and modifies the lightness value to return
 * [count] shades of this color. The shades are equally divided within the given range of min and
 * max lightness, for example 0.15 (15%) to 0.85 (85%) would yield:
 *
 * - For 2 shades: 15% and 85%.
 * - For 4 shades: 15%, 38.33%, 61.67%, 85%.
 */
fun Color.getShades(count: Int, minLightness: Float = 0.3f, maxLightness: Float = 0.7f): List<Color> {
    val hsl = toHsl()
    val step = (maxLightness - minLightness) / (count - 1)

    return (0 until count).map {
        hsl[2] = minLightness + (step * it)
        hsl.hslToColor()
    }
}

fun Color.getGradient(lightnessMiddle: Float? = null, difference: Float = 0.2f): List<Color> {
    val hsl = toHsl()
    val lightness = lightnessMiddle ?: hsl[2]

    // Calculate the adjusted lightness values
    val lowerLightness = lightness - (difference / 2)
    val upperLightness = lightness + (difference / 2)

    // Shift the range if it goes out of bounds
    var shift = 0f
    if (lowerLightness < 0f) {
        shift = -lowerLightness
    } else if (upperLightness > 1f) {
        shift = 1f - upperLightness
    }

    // Create the gradient colors
    hsl[2] = (lowerLightness + shift).coerceIn(0f, 1f)
    val lowerColor = hsl.hslToColor()
    hsl[2] = (upperLightness + shift).coerceIn(0f, 1f)
    val upperColor = hsl.hslToColor()
    hsl[2] = (lightness + shift).coerceIn(0f, 1f)
    val middleColor = hsl.hslToColor()

    return listOf(lowerColor, middleColor, upperColor)
}

@Composable
fun Color.contrastingOnColor(): Color {
    // Choose white for darker surfaces and black for lighter surfaces
    val isDarkTheme = LocalIsDarkTheme.current
    return if (this.luminance() > 0.73) {
        if(isDarkTheme) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    } else {
        if(isDarkTheme) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.inverseOnSurface
        }
    }
}

fun Color.setSaturation(saturation: Float): Color {
    val hsl = toHsl()
    hsl[1] = saturation
    return hsl.hslToColor()
}

/**
 * From: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java#219
 * Apache License, Version 2.0
 */
@Size(3)
private fun Color.toHsl(@Size(3) hsl: FloatArray = FloatArray(3)): FloatArray {
    val colorInRgb = this.toArgb()

    // Convert the ARGB integer value to it's r, g and b components clamped to a range of 0-1.
    val r = ((colorInRgb shr 16) and 0xFF).toFloat() / 255f
    val g = ((colorInRgb shr 8) and 0xFF).toFloat() / 255f
    val b = ((colorInRgb) and 0xFF).toFloat() / 255f

    // Calculate 'lightness' based on the smallest and largest color components average
    // https://en.wikipedia.org/wiki/HSL_and_HSV#Lightness
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    hsl[2] = (max + min) / 2

    if (max == min) {
        // If both the min and max values are the same, then all rgb components must be the same
        // which in RGB color land means that the color is a shade of gray. Which in HSL terms means
        // no saturation, and without any saturation the heu becomes meaningless. So we can set
        // saturation to 0 and the value of hue does not really matter.
        hsl[1] = 0f
        hsl[0] = hsl[1]
    } else {
        val d = max - min

        hsl[1] = if (hsl[2] > 0.5f) d / (2f - max - min) else d / (max + min)
        var h = when (max) {
            r -> ((g - b) / d) % 6f
            g -> ((b - r) / d) + 2f
            // 'else ->' is same as 'b ->'
            else -> ((r - g) / d) + 4f
        }
        h = (h * 60f) % 360f
        hsl[0] = if (h < 0) {
            h + 360f
        } else {
            h
        }
    }
    hsl[0] = hsl[0].constrain(0f, 360f)
    hsl[1] = hsl[1].constrain(0f, 1f)
    hsl[2] = hsl[2].constrain(0f, 1f)
    return hsl
}

/**
 * From: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java#286
 * Apache License, Version 2.0
 */
@Size(3)
fun FloatArray.hslToColor(): Color {
    val h = this[0]
    val s = this[1]
    val l = this[2]

    val c = ((1f - abs(2 * l - 1f)) * s)
    val m = l - 0.5f * c
    val x = (c * (1f - abs((h / 60f % 2f) - 1f)))

    val hueSegment = h.toInt() / 60
    var r = 0
    var g = 0
    var b = 0
    when (hueSegment) {
        0 -> {
            r = round(255 * (c + m)).toInt()
            g = round(255 * (x + m)).toInt()
            b = round(255 * m).toInt()
        }

        1 -> {
            r = round(255 * (x + m)).toInt()
            g = round(255 * (c + m)).toInt()
            b = round(255 * m).toInt()
        }

        2 -> {
            r = round(255 * m).toInt()
            g = round(255 * (c + m)).toInt()
            b = round(255 * (x + m)).toInt()
        }

        3 -> {
            r = round(255 * m).toInt()
            g = round(255 * (x + m)).toInt()
            b = round(255 * (c + m)).toInt()
        }

        4 -> {
            r = round(255 * (x + m)).toInt()
            g = round(255 * m).toInt()
            b = round(255 * (c + m)).toInt()
        }

        5, 6 -> {
            r = round(255 * (c + m)).toInt()
            g = round(255 * m).toInt()
            b = round(255 * (x + m)).toInt()
        }
    }

    r = r.constrain(0, 255)
    g = g.constrain(0, 255)
    b = b.constrain(0, 255)
    val rgb: Int = (255 shl 24) + (r shl 16) + (g shl 8) + b
    return Color(rgb)
}

/**
 * From: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java#598
 * Apache License, Version 2.0
 */
private fun Int.constrain(low: Int, high: Int): Int {
    return if (this < low) { low } else if (this > high) {high } else { this }
}

/**
 * From: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java#594
 * Apache License, Version 2.0
 */
private fun Float.constrain(low: Float, high: Float): Float {
    return if (this < low) { low } else if (this > high) {high } else { this }
}
