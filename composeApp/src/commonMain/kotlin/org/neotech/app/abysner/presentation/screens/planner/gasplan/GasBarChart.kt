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

package org.neotech.app.abysner.presentation.screens.planner.gasplan

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.desktop.ui.tooling.preview.PreviewWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.component.core.contrastingOnColor
import org.neotech.app.abysner.presentation.component.core.getGradient
import org.neotech.app.abysner.presentation.component.core.getShades
import org.neotech.app.abysner.presentation.component.core.setSaturation
import org.neotech.app.abysner.presentation.component.graphs.BarSection
import org.neotech.app.abysner.presentation.component.graphs.GasBarChartLayout
import org.neotech.app.abysner.presentation.component.graphs.HorizontalGraphAxis
import org.neotech.app.abysner.presentation.component.graphs.StackedHorizontalBar
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconFont
import org.neotech.app.abysner.presentation.theme.appendIcon
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalKoalaPlotApi::class)
@Preview
@Composable
fun GasBarChartPreview() = PreviewWrapper {
    val plan = listOf(
        DiveProfileSection(
            duration = 25,
            depth = 40,
            Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)
        ),
    )

    val divePlan = DivePlanner().addDive(
        plan,
        listOf(Cylinder.aluminium80Cuft(Gas.Nitrox50), Cylinder.aluminium63Cuft(Gas.Nitrox80))
    )

    val gasPlan = GasPlanner().calculateGasPlan(divePlan)
    AbysnerTheme(dynamicColor = false) {
        GasPlanBarChart(gasPlan = gasPlan)
    }
}


@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GasPlanBarChart(
    modifier: Modifier = Modifier,
    gasPlan: GasPlan,
    onGasBarClicked: (CylinderGasRequirements) -> Unit = {},
) {
    Column(modifier = modifier) {
        FlowLegend(
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally),
            itemCount = 3,
            label = {
                val label = when (it) {
                    0 -> "Unused"
                    1 -> "Emergency"
                    2 -> "Normal"
                    else -> error("Unknown legend index")
                }
                Text(text = label, style = MaterialTheme.typography.bodySmall)
            },
            symbol = {

                when (it) {
                    0 -> {
                        val blueShades = MaterialTheme.colorScheme.primary.getShades(
                            2,
                            minLightness = 0.2f,
                            maxLightness = 0.4f
                        )
                        Symbol(
                            shape = CircleShape,
                            size = 20.dp,
                            fillBrush = Brush.horizontalGradient(colors = blueShades),
                        )
                    }

                    1 -> {
                        val redShades = MaterialTheme.colorScheme.error.getShades(
                            2,
                            minLightness = 0.4f,
                            maxLightness = 0.8f
                        )
                        Symbol(
                            shape = CircleShape,
                            size = 20.dp,
                            fillBrush = Brush.horizontalGradient(redShades),
                        )
                    }

                    2 -> Symbol(
                        shape = CircleShape,
                        size = 20.dp,
                        fillBrush = SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        )

        val max = max(gasPlan.maxOf { it.cylinder.pressure }.toFloat(), 200f)
        val min = 0f
        val range = max - min

        val strokeWidth = 2.dp

        GasBarChartLayout(
            horizontalAxis = {
                Column {
                    HorizontalGraphAxis(
                        modifier = Modifier.padding(top = 8.dp),
                        min = min,
                        max = max,
                        axisLineColor = Color.LightGray,
                        tickCount = 6,
                        label = {
                            Text(
                                style = MaterialTheme.typography.labelSmall,
                                text = it.roundToInt().toString()
                            )
                        }
                    )
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            text = "Pressure in bar"
                        )
                    }
                }
            },
            verticalAxis = {
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gasPlan.forEachIndexed { index, gas ->
                        Text(
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                                .wrapContentHeight(align = Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium,
                            text = "${gas.cylinder.gas}"
                        )
                    }
                }
            },
            graph = {
                Column(modifier = it.drawWithContent {

                    drawContent()

                    val negativeWidth = (size.width) * (50f / range)

                    drawLine(
                        color = Color.Red.copy(alpha = 0.5f),
                        strokeWidth = strokeWidth.toPx(),
                        start = Offset(negativeWidth, 0f),
                        end = Offset(negativeWidth, size.height)
                    )

                }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gasPlan.forEach {
                        Box(contentAlignment = Alignment.Center) {
                            GasUsageBar(
                                modifier = Modifier.height(36.dp).clickable {
                                    onGasBarClicked(it)
                                },
                                cylinderGasRequirements = it,
                                maxValue = max,
                                minValue = min
                            )
                            if (it.pressureLeft == null) {
                                val alertMessage = buildAnnotatedString {
                                    appendIcon(IconFont.WARNING)
                                    append("critical gas shortage")
                                }

                                TextAlert(
                                    textStyle = MaterialTheme.typography.labelLarge,
                                    text = alertMessage,
                                    alertSeverity = AlertSeverity.ERROR
                                )
                            } else if(it.pressureLeftWithEmergency == null) {
                                val alertMessage = buildAnnotatedString {
                                    appendIcon(IconFont.WARNING)
                                    append("gas shortage")
                                }

                                TextAlert(
                                    textStyle = MaterialTheme.typography.labelLarge,
                                    text = alertMessage,
                                    alertSeverity = AlertSeverity.WARNING
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun GasUsageBarPreview() = PreviewWrapper {
    AbysnerTheme {
        GasUsageBar(modifier = Modifier.fillMaxWidth().height(48.dp), maxValue = 230f, minValue = 0f)
    }
}

@Composable
fun GasUsageBar(
    modifier: Modifier = Modifier,
    cylinderGasRequirements: CylinderGasRequirements = CylinderGasRequirements(Cylinder.steel12Liter(Gas.Air), 1000.0, 500.0),
    maxValue: Float,
    minValue: Float,
) {

    val pressureLeftWithEmergency = cylinderGasRequirements.pressureLeftWithEmergency?.toFloat() ?: 0f
    val pressureLeftWithoutEmergency = cylinderGasRequirements.pressureLeft?.toFloat() ?: 0f

    val redShades =
        MaterialTheme.colorScheme.error.setSaturation(0.7f).getGradient(lightnessMiddle = 0.65f, difference = 0.2f)
    val blueShades =
        MaterialTheme.colorScheme.primary.getGradient(lightnessMiddle = 0.35f, difference = 0.2f)

    val redForeground = redShades[1].contrastingOnColor()
    val blueForeground = blueShades[1].contrastingOnColor()

    val values = listOf<BarSection>(

        // Gas left
        BarSection(
            value = pressureLeftWithEmergency,
            color = Brush.horizontalGradient(colors = blueShades),
            textColor = SolidColor(blueForeground),
            textStyle = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Left)
        ),
        // Gas used with emergency
        BarSection(
            value = pressureLeftWithoutEmergency - pressureLeftWithEmergency,
            color = Brush.horizontalGradient(colors = redShades),
            textColor = SolidColor(redForeground),
            textStyle = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center)
        ),
        // Gas normally used
        BarSection(
            value = cylinderGasRequirements.cylinder.pressure.toFloat() - pressureLeftWithoutEmergency,
            color = SolidColor(MaterialTheme.colorScheme.outlineVariant),
            textColor = SolidColor(MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.outlineVariant)),
            textStyle = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Right)
        )
    )

    StackedHorizontalBar(
        modifier = modifier,
        values = values,
        maxValue = maxValue,
        minValue = minValue,
        valueTransformation = { index, value ->
            // Don't show numbers if we cannot show the whole bars that those numbers are supposed
            // to represent
            if (cylinderGasRequirements.pressureLeftWithEmergency == null && (index == 0 || index == 1)) {
                ""
            } else if(index == 2 && cylinderGasRequirements.pressureLeft == null) {
                ""
            } else {
                DecimalFormat.format(0, value)
            }
        }
    )
}
