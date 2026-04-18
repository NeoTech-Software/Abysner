/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.about

import org.neotech.app.abysner.presentation.component.core.preMixedWith

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

fun Modifier.waveBackground(): Modifier = composed {

    val isPreview = LocalInspectionMode.current

    val infiniteTransition = rememberInfiniteTransition()

    val wave1HorizontalMovement by infiniteTransition.animateFloat(
        initialValue = if (isPreview) 0.5f else 0f,
        targetValue = if (isPreview) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val wave2HorizontalMovement by infiniteTransition.animateFloat(
        initialValue = if (isPreview) 0.5f else 0f,
        targetValue = if (isPreview) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val wave2VerticalMovement by infiniteTransition.animateFloat(
        initialValue = if (isPreview) 0.5f else 0f,
        targetValue = if (isPreview) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val wave3HorizontalMovement by infiniteTransition.animateFloat(
        initialValue = if (isPreview) 0.5f else 0f,
        targetValue = if (isPreview) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val animationStarted = rememberSaveable { mutableStateOf(false) }
    val cameraVerticalPan = remember {
        Animatable(if (animationStarted.value || isPreview) 1f else 0f)
    }

    LaunchedEffect(cameraVerticalPan) {
        animationStarted.value = true
        cameraVerticalPan.animateTo(
            targetValue = 1f,
            animationSpec = tween(3500, easing = FastOutSlowInEasing)
        )
    }

    // Colors derived from the Abysner logo
    val wave1ColorStart = Color(148, 195, 217)
    val wave1ColorEnd = wave1ColorStart.preMixedWith(Color.Black)

    val wave2ColorStart = Color(6, 38, 56)
    val wave2ColorEnd = wave2ColorStart.preMixedWith(Color.Black)

    val wave3ColorStart = Color(77, 117, 137)
    val wave3ColorEnd = wave3ColorStart.preMixedWith(Color.Black)

    val wave1Brush = Brush.verticalGradient(listOf(wave1ColorStart, wave1ColorEnd))
    val wave2Brush = Brush.verticalGradient(
        listOf(wave2ColorStart.copy(alpha = 0.7f), wave2ColorEnd.copy(alpha = 0.7f))
    )
    val wave3Brush = Brush.verticalGradient(
        listOf(wave3ColorStart.copy(alpha = 0.5f), wave3ColorEnd.copy(alpha = 0.5f))
    )

    drawWithCache {

        val wave1Amplitude = 60.dp.toPx()
        val wave1Length = 200.dp.toPx()

        val wave2Amplitude = 30.dp.toPx()
        val wave2VerticalRange = wave2Amplitude / 2f
        val wave2Length = 120.dp.toPx()

        val wave3Amplitude = 45.dp.toPx()
        val wave3Length = 160.dp.toPx()

        val boundsExtra = size.toRect().copy(
            right = size.width + maxOf(wave1Length, wave2Length, wave3Length)
        )

        // Animate waves down until 66% of the screen is above the surface
        val wavesVerticalCameraPanDistance = boundsExtra.bottom * 0.66f
        // Animate content up starting 20% below the vertical center of the screen
        val contentVerticalCameraPanDistance = boundsExtra.height * 0.2f

        val wave1Path = createWavePath(
            bounds = boundsExtra,
            wavelength = wave1Length,
            amplitude = wave1Amplitude
        )
        val wave2Path = createWavePath(
            bounds = boundsExtra,
            wavelength = wave2Length,
            amplitude = wave2Amplitude,
            extraVerticalOffset = (wave1Amplitude - wave2Amplitude) * 1.5f
        )
        val wave3Path = createWavePath(
            bounds = boundsExtra,
            wavelength = wave3Length,
            amplitude = wave3Amplitude,
            extraVerticalOffset = (wave1Amplitude - wave3Amplitude) * 0.8f
        )

        onDrawWithContent {

            val wavesVerticalOffset =
                wavesVerticalCameraPanDistance * cameraVerticalPan.value

            // Background wave
            translate(
                left = -(wave1Length * wave1HorizontalMovement),
                top = wavesVerticalOffset
            ) {
                drawPath(style = Fill, brush = wave1Brush, path = wave1Path)
            }

            // Middle wave
            translate(
                left = -(wave3Length * wave3HorizontalMovement),
                top = wavesVerticalOffset
            ) {
                drawPath(style = Fill, brush = wave3Brush, path = wave3Path)
            }

            // Content (drawn between background and foreground waves)
            translate(
                top = contentVerticalCameraPanDistance * (1f - cameraVerticalPan.value)
            ) {
                this@onDrawWithContent.drawContent()
            }

            // Foreground wave
            translate(
                left = -(wave2Length * wave2HorizontalMovement),
                top = wavesVerticalOffset - (wave2VerticalRange / 2f) + (wave2VerticalRange * wave2VerticalMovement)
            ) {
                drawPath(style = Fill, brush = wave2Brush, path = wave2Path)
            }
        }
    }
}

/**
 * Creates a wave [Path] using quadratic bezier curves. The wave starts from the top-left with its
 * highest point touching [Rect.top] of the given [bounds]. When [closePath] is true the path is
 * closed to fill the remaining space below the wave.
 */
private fun createWavePath(
    bounds: Rect,
    wavelength: Float,
    amplitude: Float,
    extraVerticalOffset: Float = 0f,
    closePath: Boolean = true,
): Path {
    val distanceBetweenPoints = wavelength / 2f
    val pointCount = ceil(bounds.width / distanceBetweenPoints).toInt() + 1

    return Path().apply {
        var pointX = bounds.left
        for (point in 0..pointCount) {
            val offsetY = bounds.top + amplitude + extraVerticalOffset
            when (point) {
                0 -> if (closePath) {
                    moveTo(pointX, bounds.bottom)
                    lineTo(pointX, offsetY)
                } else {
                    moveTo(pointX, offsetY)
                }

                else -> {
                    val controlY = if (point % 2 == 0) { -amplitude } else { amplitude }
                    quadraticTo(
                        x1 = pointX - (distanceBetweenPoints / 2f),
                        y1 = offsetY + controlY,
                        x2 = pointX,
                        y2 = offsetY
                    )
                }
            }
            pointX = (distanceBetweenPoints * point)
        }
        if (closePath) {
            lineTo(pointX, bounds.bottom)
        }
    }
}

