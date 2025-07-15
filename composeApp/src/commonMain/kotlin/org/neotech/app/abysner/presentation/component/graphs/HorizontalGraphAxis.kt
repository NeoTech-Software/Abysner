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

package org.neotech.app.abysner.presentation.component.graphs

import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.presentation.utilities.PreviewWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalGraphAxis(
    modifier: Modifier = Modifier,
    min: Float,
    max: Float,
    tickCount: Int,
    axisLineColor: Color = Color.Black,
    tickMarkColor: Color = axisLineColor,
    tickMarkLength: Dp = 8.dp,
    /**
     * Vertical space between the tick mark and label.
     */
    labelSpacing: Dp = 4.dp,
    tickMarkSize: Dp = 8.dp,
    lineWidth: Dp = 2.dp,
    label: @Composable (value: Float) -> Unit = DefaultLabelComposable,
    labelMaxWidth: Dp = 96.dp
) {
    val range = max - min
    val tickSpacing = range / (tickCount - 1)
    val ticks = (0 until tickCount).map { index -> min + index * tickSpacing }
    HorizontalGraphAxis(
        modifier = modifier,
        min = min,
        max = max,
        ticksAt = ticks,
        axisLineColor = axisLineColor,
        tickMarkColor = tickMarkColor,
        tickMarkLength = tickMarkLength,
        labelSpacing = labelSpacing,
        tickMarkSize = tickMarkSize,
        lineWidth = lineWidth,
        label = label,
        labelMaxWidth = labelMaxWidth
    )
}


@Composable
fun HorizontalGraphAxis(
    modifier: Modifier = Modifier,
    min: Float,
    max: Float,
    ticksAt: List<Float> = listOf(min, max),
    axisLineColor: Color = Color.Black,
    tickMarkColor: Color = axisLineColor,
    tickMarkLength: Dp = 8.dp,
    /**
     * Vertical space between the tick mark and label.
     */
    labelSpacing: Dp = 4.dp,
    tickMarkSize: Dp = 8.dp,
    lineWidth: Dp = 2.dp,
    label: @Composable (value: Float) -> Unit = DefaultLabelComposable,
    labelMaxWidth: Dp = 96.dp
) {
    BoxWithConstraints(modifier = modifier) {
        val range = (max - min)
        val tickHorizontalPositions = ticksAt.map { value ->
            val normalized = (value - min) / range
            maxWidth * normalized
        }

        Canvas(modifier = Modifier.height(tickMarkSize).fillMaxWidth()) {
            // Draw the axis line
            drawLine(
                color = axisLineColor,
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = size.width, y = 0f),
                strokeWidth = lineWidth.toPx()
            )
            // Draw tick marks
            tickHorizontalPositions.forEach {
                val startX = it.toPx()
                val startY = 0f
                val endX = it.toPx()
                val endY = tickMarkLength.toPx()

                drawLine(
                    color = tickMarkColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = tickMarkSize + labelSpacing),
        ) {
            ticksAt.forEachIndexed { index, value ->
                val labelPosition = tickHorizontalPositions[index]
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(labelMaxWidth)
                        .offset(x = labelPosition - (labelMaxWidth / 2f)),
                    contentAlignment = Alignment.Center
                ) {
                    label(value)
                }
            }
        }
    }
}

private val DefaultLabelComposable: @Composable (value: Float) -> Unit = {
    Text(
        style = MaterialTheme.typography.labelSmall,
        text = "$it"
    )
}

@Preview
@Composable
fun GraphAxisPreview() = PreviewWrapper {
    Box {
        HorizontalGraphAxis(
            min = -100f,
            max = 200f,
            ticksAt = listOf(-100f, 0f, 200f)
        )
    }
}
