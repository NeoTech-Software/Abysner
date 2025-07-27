/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.diveplanning.model.DivePlan
import org.neotech.app.abysner.presentation.component.none
import kotlin.math.absoluteValue

val Lineshape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                moveTo(0f, size.height / 2f)
                lineTo(size.width, size.height / 2f)
            }
        )

    @Suppress("SameReturnValue")
    override fun toString(): String = "LineShape"
}

@Composable
fun StrokeDp(
    widthInDp: Dp = 0.dp,
    miter: Float = Stroke.DefaultMiter,
    cap: StrokeCap = Stroke.DefaultCap,
    join: StrokeJoin = Stroke.DefaultJoin,
    pathEffect: PathEffect? = null
): Stroke {
    return with(LocalDensity.current) {
        Stroke(
            widthInDp.toPx(),
            miter,
            cap,
            join,
            pathEffect
        )
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun DecoPlanGraph(
    modifier: Modifier,
    divePlan: DivePlan
) {

    val textAndAxisColor = MaterialTheme.colorScheme.outline

    val xaxisStyle = rememberAxisStyle(
        color = MaterialTheme.colorScheme.outlineVariant,
        majorTickSize = 0.dp,
        lineWidth = 1.dp,
    )

    val yaxisStyle = rememberAxisStyle(
        color = MaterialTheme.colorScheme.outlineVariant,
        majorTickSize = 0.dp,
        lineWidth = 1.dp,
    )
    
    Column {

        FlowLegend(
            modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.CenterHorizontally),
            itemCount = 3,
            label = {
                val label = when(it) {
                    0 -> "Depth"
                    1 -> "GF ceiling"
                    2 -> "Avg. depth"
                    else -> error("Unknown legend index")
                }
                Text(text = label, style = MaterialTheme.typography.bodySmall)
                    },
            symbol = {

                when(it) {
                    0 -> Symbol(
                        shape = Lineshape,
                        size = 20.dp,
                        outlineBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        outlineStroke = StrokeDp(2.dp)
                    )
                    1 -> Symbol(
                        shape = Lineshape,
                        size = 20.dp,
                        outlineBrush = SolidColor(MaterialTheme.colorScheme.error),
                        outlineStroke = StrokeDp(1.dp)
                    )
                    2 -> Symbol(
                        shape = Lineshape,
                        size = 20.dp,
                        outlineBrush = SolidColor(MaterialTheme.colorScheme.outline),
                        outlineStroke = StrokeDp(widthInDp = 1.dp, pathEffect =  PathEffect.dashPathEffect(intervals = floatArrayOf(15.0f, 15.0f)))
                    )
                }

            }

        )


        XYGraph(
            xAxisStyle = xaxisStyle,
            yAxisStyle = yaxisStyle,
            modifier = modifier,
            horizontalMajorGridLineStyle = null,
            verticalMajorGridLineStyle = null,
            horizontalMinorGridLineStyle = null,
            verticalMinorGridLineStyle = null,
            xAxisModel = FloatLinearAxisModel(
                minorTickCount = 1,
                minimumMajorTickIncrement = 1f,
                minimumMajorTickSpacing = 48.dp,
                range = 0f..divePlan.runtime.toFloat(),
            ),
            yAxisModel = FloatLinearAxisModel(
                minorTickCount = 4,
                range = -(divePlan.maximumDepth.toFloat() * 1.05f)..(divePlan.maximumDepth.toFloat() * 0.05f),
                minimumMajorTickSpacing = 24.dp,
                minimumMajorTickIncrement = 1f,
            ),
            xAxisLabels = {
                Text(
                    it.toInt().toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = textAndAxisColor
                )
            },
            yAxisLabels = {
                Text(
                    it.absoluteValue.toInt().toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = textAndAxisColor
                )
            },
            yAxisTitle = { },
        ) {

            AreaPlot(
                data = //listOf(DefaultPoint(0f, 0f)) +
                        divePlan.segments.map { segment ->
                            // End should be used here, but by shifting the whole graph 1 minute to the left,
                            // it will never visually touch the dive line, which makes for a slightly nicer
                            // UX (although be it a slightly less accurate graph (as long as the
                            // underlying data is accurate this should not matter)
                            DefaultPoint(segment.start.toFloat(), -segment.gfCeilingAtEnd.toFloat())
                        } + DefaultPoint(divePlan.runtime.toFloat(), -divePlan.segments.last().gfCeilingAtEnd.toFloat()),
                lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.error),
                    strokeWidth = 1.dp
                ),
                areaBaseline = AreaBaseline.ConstantLine(0.0f),
                areaStyle = AreaStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.error),
                    alpha = 0.5f
                ),
                animationSpec = if(LocalInspectionMode.current) { none() } else { KoalaPlotTheme.animationSpec }
            )

            var runtime = 0L
            LinePlot(
                data = divePlan.segments.map { segment ->
                    DefaultPoint(runtime.toFloat(), -segment.startDepth.toFloat()).also {
                        runtime += segment.duration
                    }
                } + DefaultPoint(runtime.toFloat(), -divePlan.segments.last().endDepth.toFloat()),
                lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.primary),
                    strokeWidth = 2.dp
                ),
                animationSpec = if(LocalInspectionMode.current) { none() } else { KoalaPlotTheme.animationSpec }
            )

            var runningAverage = 0.0
            var runningDuration = 0L

            val points = divePlan.segments.flatMapIndexed { index: Int, segment: DiveSegment ->
                val range = if (index < divePlan.segments.size - 1) {
                    0..<segment.duration
                } else {
                    0..segment.duration
                }
                range.map {
                    val depthAtMinute = segment.depthAt(it)
                    runningAverage += depthAtMinute
                    runningDuration += 1
                    val runningAverageDepth = runningAverage / runningDuration
                    DefaultPoint(runningDuration.toFloat() - 1, -runningAverageDepth.toFloat())
                }
            }

            LinePlot(
                data = points,
                lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.outline),
                    pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(15.0f, 15.0f)),
                    strokeWidth = 1.dp
                ),
                animationSpec = if(LocalInspectionMode.current) { none() } else { KoalaPlotTheme.animationSpec }
            )
        }
    }
}

@Preview
@Composable
private fun DecoPlanGraphPreview() {

    val cylinder = Cylinder.steel12Liter(Gas.Air)

    DecoPlanGraph(
        modifier = Modifier, divePlan = DivePlan(
            segments = listOf(
                DiveSegment(0,5, 0.0, 25.0, cylinder, isDecompression = false, gfCeilingAtEnd = 0.0),

                DiveSegment(5,20, 25.0, 20.0, cylinder, isDecompression = false, gfCeilingAtEnd = 0.0),
                DiveSegment(25,20, 25.0, 0.0, cylinder, isDecompression = false, gfCeilingAtEnd = 0.0),

                ),
            alternativeAccents = emptyMap(),
            decoGasses = emptyList(),
            configuration = Configuration(),
            totalCns = 0.0,
            totalOtu = 0.0
        )
    )
}
