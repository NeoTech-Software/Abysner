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

import androidx.compose.desktop.ui.tooling.preview.PreviewWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.presentation.component.none
import org.neotech.app.abysner.presentation.component.core.getShades

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GasPieChart(
    modifier: Modifier,
    gasRequirement: GasPlan
) {

    val emergencyExtra = gasRequirement.extraRequiredForWorstCaseOutOfAirSorted
    val base = gasRequirement.sortedBase

    val gas = base + emergencyExtra

    val baseColor = MaterialTheme.colorScheme.primary
    val colors = remember(gas.size) {
        baseColor.getShades(gas.size)
    }
    val baseColorRed = MaterialTheme.colorScheme.error
    val colorsRed = remember(gas.size) {
        baseColorRed.getShades(gas.size)
    }

    Column(
        modifier = modifier
    ) {
        PieChart(
            values = gas.map { it.second.toFloat() },
            slice = {
                // TODO check with graph library why an index out-of-bound exception can occur:
                //   for some reason is can request a slice that is not in the values, and thus not
                //   in the colors array. This only seems to be a problem on iOS.
                if (it in gas.indices) {
                    // Slices are based on colors going from darker to lighter, the extra gas slices do not
                    // start at the darkest color, instead they pick up where the base color stopped.
                    // This is a nicer transition.
                    if (it >= base.size) {
                        DefaultSlice(color = colorsRed[it], antiAlias = true)
                    } else {
                        DefaultSlice(color = colors[it], antiAlias = true)
                    }
                }
            },
            label = {
                // TODO check with graph library why an index out-of-bound exception can occur:
                //   for some reason is can request a slice that is not in the values, and thus not
                //   in the colors array. This only seems to be a problem on iOS.
                if (it in gas.indices) {
                    val name = gas[it].first.gas.toString()
                    val liters = gas[it].second
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        text = buildAnnotatedString {
                            if (it >= base.size) {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    appendLine(name)
                                }
                                append("${liters.toInt()} liters")
                            } else {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    appendLine(name)
                                }
                                append("${liters.toInt()} liters")
                            }
                        }
                    )
                }
            },
            animationSpec = if (LocalInspectionMode.current) {
                none()
            } else {
                KoalaPlotTheme.animationSpec
            }
        )

        FlowLegend(
            modifier = Modifier.padding(all = 16.dp).align(Alignment.CenterHorizontally),
            itemCount = arrayOf(emergencyExtra.isNotEmpty(), base.isNotEmpty()).count { it },
            label = {
                val label = when(it) {
                    0 -> "Base*"
                    1 -> "(Out-of-air)*"
                    else -> error("Unknown legend index")
                }
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            },
            symbol = {

                when(it) {
                    0 -> Symbol(
                        shape = CircleShape,
                        size = 20.dp,
                        // Gradient from first primary color that is used, until the last blue color.
                        fillBrush = Brush.horizontalGradient(listOf(colors.first(), colors[base.size-1])),
                    )
                    1 -> Symbol(
                        shape = CircleShape,
                        size = 20.dp,
                        // Gradient from first red color that is actually used until the last red color
                        fillBrush = Brush.horizontalGradient(listOf(colorsRed[base.size], colorsRed.last())),
                    )
                }
            }
        )
    }
}

@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
fun GasPieChartPreview() = PreviewWrapper {
    val plan = listOf(
        DiveProfileSection(25, 35, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
    )

    val divePlan = DivePlanner().getDecoPlan(plan, listOf(Cylinder.aluminium80Cuft(Gas.Nitrox50)))

    val gasPlan = GasPlanner().calculateGasPlan(divePlan)

    GasPieChart(
        modifier = Modifier.size(200.dp),
        gasRequirement = gasPlan
    )
}
