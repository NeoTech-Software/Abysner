/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner.decoplan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend2
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.GridStyle
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
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
fun strokeDp(
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

        FlowLegend2(
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
                        outlineStroke = strokeDp(2.dp)
                    )
                    1 -> Symbol(
                        shape = Lineshape,
                        size = 20.dp,
                        outlineBrush = SolidColor(MaterialTheme.colorScheme.error),
                        outlineStroke = strokeDp(1.dp)
                    )
                    2 -> Symbol(
                        shape = Lineshape,
                        size = 20.dp,
                        outlineBrush = SolidColor(MaterialTheme.colorScheme.outline),
                        outlineStroke = strokeDp(widthInDp = 1.dp, pathEffect =  PathEffect.dashPathEffect(intervals = floatArrayOf(15.0f, 15.0f)))
                    )
                }

            }

        )


        XYGraph(
            modifier = modifier,
            xAxisModel = FloatLinearAxisModel(
                minorTickCount = 1,
                minimumMajorTickIncrement = 1f,
                minimumMajorTickSpacing = 48.dp,
                range = 0f..maxOf(divePlan.runtime.toFloat(), 1f),
            ),
            yAxisModel = run {
                val maxDepth = maxOf(divePlan.maximumDepth.toFloat(), 1f)
                FloatLinearAxisModel(
                    minorTickCount = 4,
                    range = -(maxDepth * 1.05f)..(maxDepth * 0.05f),
                    minimumMajorTickSpacing = 24.dp,
                    minimumMajorTickIncrement = 1f,
                )
            },
            xAxisContent = AxisContent(
                style = xaxisStyle,
                labels = {
                    Text(
                        it.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = textAndAxisColor
                    )
                },
                title = {},
            ),
            yAxisContent = AxisContent(
                style = yaxisStyle,
                labels = {
                    Text(
                        it.absoluteValue.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = textAndAxisColor
                    )
                },
                title = {},
            ),
            gridStyle = GridStyle(
                horizontalMajorStyle = null,
                horizontalMinorStyle = null,
                verticalMajorStyle = null,
                verticalMinorStyle = null,
            ),
        ) {

            AreaPlot2(
                // Offset the gradient graph ever so slightly to the top so it does not interfere
                // visually too much with the depth line, 0.25f is essentially equal to 25cm.
                data = buildGfCeilingPlotPoints(divePlan.segments).offset(y = 0.25f).coerceIn(yMax = 0f),
                lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.error),
                    strokeWidth = 1.dp
                ),
                areaBaseline = AreaBaseline.HorizontalLine(0.0f),
                areaStyle = AreaStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.error),
                    alpha = 0.5f
                ),
                animationSpec = if(LocalInspectionMode.current) { none() } else { KoalaPlotTheme.animationSpec }
            )

            LinePlot2(
                data = buildDepthProfilePlotPoints(divePlan.segments),
                lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.primary),
                    strokeWidth = 2.dp
                ),
                animationSpec = if(LocalInspectionMode.current) { none() } else { KoalaPlotTheme.animationSpec }
            )

            LinePlot2(
                data = buildAverageDepthPlotPoints(divePlan.segments),
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

    Surface {
        DecoPlanGraph(
            modifier = Modifier.height(164.dp), divePlan = DivePlan(
                segments = persistentListOf(
                    DiveSegment(
                        0,
                        3,
                        0.0,
                        25.0,
                        cylinder,
                        type = DiveSegment.Type.DECENT,
                        gfCeilingAtEnd = 0.0
                    ),
                    DiveSegment(
                        3,
                        20,
                        25.0,
                        25.0,
                        cylinder,
                        type = DiveSegment.Type.FLAT,
                        gfCeilingAtEnd = 3.0
                    ),
                    DiveSegment(
                        23,
                        3,
                        25.0,
                        5.0,
                        cylinder,
                        type = DiveSegment.Type.ASCENT,
                        gfCeilingAtEnd = 2.0
                    ),
                    DiveSegment(
                        26,
                        3,
                        5.0,
                        5.0,
                        cylinder,
                        type = DiveSegment.Type.DECO_STOP,
                        gfCeilingAtEnd = 1.0
                    ),
                    DiveSegment(
                        29,
                        1,
                        5.0,
                        0.0,
                        cylinder,
                        type = DiveSegment.Type.ASCENT,
                        gfCeilingAtEnd = 0.0
                    ),
                ),
                alternativeAccents = persistentMapOf(),
                cylinders = persistentListOf(),
                configuration = Configuration(),
                totalCns = 0.0,
                totalOtu = 0.0
            )
        )
    }
}
