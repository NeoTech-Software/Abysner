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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Layout that positions the horizontal and vertical axis as well as the graph area
 */
@Composable
internal fun GasBarChartLayout(
    modifier: Modifier = Modifier,
    horizontalAxis: @Composable (modifier: Modifier) -> Unit,
    verticalAxis: @Composable (modifier: Modifier) -> Unit,
    graph: @Composable (modifier: Modifier) -> Unit,
) {
    // ConstraintLayout uses this internally, but is not yet available on Compose MultiPlatform
    MultiMeasureLayout(
        modifier = modifier,
        content = {
            Box { verticalAxis(Modifier) }
            Box { graph(Modifier) }
            Box { horizontalAxis(Modifier) }
        }
    ) { measurables, constraints ->

        // Measure the vertical axis.
        val verticalAxisMeasurable = measurables[0]
        var verticalAxisPlaceable = verticalAxisMeasurable.measure(constraints)

        // The available width for the graph after reserving space for vertical axis
        val graphWidth = constraints.maxWidth - verticalAxisPlaceable.width

        // Measure the horizontal axis (maxWidth constraint by `graphWidth`)
        val horizontalAxisMeasurable = measurables[2]
        val horizontalAxisPlaceable = horizontalAxisMeasurable.measure(
            constraints.copy(
                maxWidth = graphWidth
            )
        )

        // Measure the graph (maxWidth constraint by `graphWidth`)
        val graphMeasurable = measurables[1]
        val graphPlaceable = graphMeasurable.measure(
            constraints.copy(maxWidth = graphWidth)
        )

        // Measure the vertical labels again to account for the graph height
        verticalAxisPlaceable = verticalAxisMeasurable.measure(
            constraints.copy(
                minHeight = graphPlaceable.height,
                maxHeight = graphPlaceable.height
            )
        )

        // The total height of the layout: the height of the graph plus the height of the horizontal axis
        val totalHeight = graphPlaceable.height + horizontalAxisPlaceable.height

        layout(constraints.maxWidth, totalHeight) {
            // Place the vertical labels at the left
            verticalAxisPlaceable.placeRelative(0, 0)

            // Place the graph next to the vertical labels
            graphPlaceable.placeRelative(verticalAxisPlaceable.width, 0)

            // Place the horizontal axis below the graph
            horizontalAxisPlaceable.placeRelative(
                verticalAxisPlaceable.width,
                graphPlaceable.height
            )
        }
    }
}

internal data class BarSection(
    val value: Float,
    val color: Brush,
    val textColor: Brush,
    val textStyle: TextStyle
)

@Composable
internal fun StackedHorizontalBar(
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float = 200f,
    offset: Float = 0f,
    values: List<BarSection>,
    valueTransformation: (Int, Float) -> String = { _, value -> value.toString() }
) {
    // TextMeasurer for text measurement
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxWidth().height(20.dp).then(modifier)) {
        val barWidth = size.width
        val range = maxValue - minValue
        var currentX = 0f

        // "Draw" empty space for negative range (if minValue < 0)
        if (minValue < 0) {
            val negativeWidth = barWidth * (-minValue / range)
            currentX += (negativeWidth + (barWidth * (offset / range)))
        }

        values.forEachIndexed { index, barSection ->
            val value = barSection.value
            // Normalize value based on the maximum value
            val normalizedValue = value / range
            val segmentWidth = min(barWidth * normalizedValue, barWidth - currentX)

            if (segmentWidth >= 0) {

                drawRect(
                    brush = barSection.color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(segmentWidth, size.height)
                )

                // Prepare the text content and style
                val text = valueTransformation(index, value)

                // Measure the width of the text using TextMeasurer
                val availableWidth = max((segmentWidth - 8.dp.toPx()).toInt(), 0)
                val textLayoutResult = textMeasurer.measure(
                    constraints = Constraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        maxHeight = size.height.toInt()
                    ),
                    maxLines = 1,
                    text = text,
                    style = barSection.textStyle
                )

                // Calculate the position to draw the text (right-aligned)
                val textX = currentX + segmentWidth - textLayoutResult.size.width - 4.dp.toPx()
                val textY = (size.height - textLayoutResult.size.height) / 2f

                // If with is exactly 0, it seems overflow in the text measurer returns false
                if (availableWidth > 0 && !textLayoutResult.didOverflowWidth && !textLayoutResult.didOverflowHeight) {
                    // Rectangle for debugging purposes
                    // drawRect(brush = SolidColor(Color.Yellow), topLeft = Offset(textX, textY), size = Size(textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat()), alpha = 0.5f)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        brush = barSection.textColor,
                        topLeft = Offset(textX, textY),
                        drawStyle = barSection.textStyle.drawStyle
                    )
                }
            }
            currentX += segmentWidth
        }
    }
}
