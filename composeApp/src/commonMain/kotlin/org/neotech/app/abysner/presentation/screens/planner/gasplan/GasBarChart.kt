/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner.gasplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend2
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.collections.immutable.toImmutableList
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import org.neotech.app.abysner.presentation.component.core.getGradient
import org.neotech.app.abysner.presentation.component.core.ifTrue
import org.neotech.app.abysner.presentation.component.core.invisible
import org.neotech.app.abysner.presentation.component.core.sampleGradientAt
import org.neotech.app.abysner.presentation.component.core.uniformLabelWidth
import org.neotech.app.abysner.presentation.component.graphs.LabeledTrack
import org.neotech.app.abysner.presentation.component.graphs.TrackLabel
import org.neotech.app.abysner.presentation.formatting.FIGURE_SPACE
import org.neotech.app.abysner.presentation.formatting.formatCapacity
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconFont
import org.neotech.app.abysner.presentation.theme.appendIcon
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning
import org.neotech.app.abysner.presentation.theme.withTabularFigures
import org.neotech.app.abysner.presentation.utilities.PreviewWrapper
import org.neotech.app.abysner.presentation.utilities.formatPressure

/**
 * Shows every cylinder as a pressure track from 0 to its fill pressure, with the end pressure
 * after a normal dive labeled above and the end pressure after the reserve (or bailout) is
 * consumed labeled below. Tapping a track invokes [onGasBarClicked].
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun GasPlanBarChart(
    modifier: Modifier = Modifier,
    gasPlan: GasPlan,
    unitSystem: UnitSystem,
    emergencyLabel: String = "Reserve",
    usageLabel: String = "Used",
    emergencyIsError: Boolean = false,
    onGasBarClicked: (Int, CylinderGasRequirements) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier) {
        FlowLegend2(
            modifier = Modifier.padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally),
            itemCount = 3,
            label = {
                val label = when (it) {
                    0 -> "Unused"
                    1 -> emergencyLabel
                    2 -> usageLabel
                    else -> error("Unknown legend index")
                }
                Text(text = label, style = MaterialTheme.typography.bodySmall)
            },
            symbol = {
                val brush = when (it) {
                    0 -> Brush.horizontalGradient(
                        MaterialTheme.colorScheme.primary.getGradient(difference = gradientDifference)
                    )
                    1 -> Brush.horizontalGradient(
                        MaterialTheme.colorScheme.primaryContainer.getGradient(difference = gradientDifference)
                    )
                    2 -> Brush.horizontalGradient(
                        MaterialTheme.colorScheme.outlineVariant.getGradient(difference = gradientDifference)
                    )
                    else -> error("Unknown legend index")
                }
                Symbol(shape = CircleShape, size = 20.dp, fillBrush = brush)
            }
        )

        // Widest fill pressure in the plan, so every row can pad to the same digit count.
        val fillPressureDigits = gasPlan.maxOf {
            it.cylinder.pressure.formatPressure(unitSystem, includeUnit = false).length
        }

        val mixLabelWidth = remember { mutableIntStateOf(0) }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            gasPlan.forEachIndexed { index, cylinderGasRequirements ->
                CylinderPressureRow(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable { onGasBarClicked(index, cylinderGasRequirements) },
                    cylinderGasRequirements = cylinderGasRequirements,
                    unitSystem = unitSystem,
                    emergencyLabel = emergencyLabel,
                    emergencyIsError = emergencyIsError,
                    fillPressureDigits = fillPressureDigits,
                    mixLabelWidth = mixLabelWidth
                )
            }
        }
    }
}

@Composable
private fun CylinderPressureRow(
    modifier: Modifier = Modifier,
    cylinderGasRequirements: CylinderGasRequirements,
    unitSystem: UnitSystem,
    emergencyLabel: String,
    emergencyIsError: Boolean,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall.withTabularFigures(),
    warningColor: Color = MaterialTheme.colorScheme.warning,
    errorColor: Color = MaterialTheme.colorScheme.error,
    boundaryLineColor: Color = MaterialTheme.colorScheme.onSurface,
    fillPressureDigits: Int,
    mixLabelWidth: MutableIntState,
) {
    val pressureLeft = cylinderGasRequirements.pressureLeft
    val pressureLeftWithEmergency = cylinderGasRequirements.pressureLeftWithEmergency

    val reserveColors = MaterialTheme.colorScheme.primaryContainer.getGradient(difference = gradientDifference)
    val usedColors = MaterialTheme.colorScheme.outlineVariant.getGradient(difference = gradientDifference)

    val unusedBrush = Brush.horizontalGradient(MaterialTheme.colorScheme.primary.getGradient(difference = gradientDifference))
    val reserveBrush = Brush.horizontalGradient(reserveColors)
    val usedBrush = Brush.horizontalGradient(usedColors)

    val showEmergencyPressure = pressureLeftWithEmergency != null && pressureLeftWithEmergency != pressureLeft

    val cylinder = cylinderGasRequirements.cylinder
    val aboveFraction = ((pressureLeft ?: cylinder.pressure) / cylinder.pressure).toFloat()
    val belowFraction = ((pressureLeftWithEmergency ?: 0.0) / cylinder.pressure).toFloat()

    val insufficientReserveColor = if (emergencyIsError) { errorColor } else { warningColor }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.uniformLabelWidth(mixLabelWidth).padding(end = 8.dp),
            style = MaterialTheme.typography.labelMedium.withTabularFigures(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            text = buildAnnotatedString {
                append(cylinder.gas.toString())
                appendLine()
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                ) {
                    append(cylinder.formatCapacity(unitSystem))
                }
            },
        )

        LabeledTrack(
            modifier = Modifier.weight(1f),
            // Above label always shows the pressure left after a normal dive (no emergency/bailout)
            aboveLabel = TrackLabel(fraction = aboveFraction) {
                PressureLabel(
                    text = pressureLeft?.formatPressure(unitSystem, includeUnit = false).orEmpty(),
                    isVisible = pressureLeft != null,
                    // Match the label color to the bar color at the label's position.
                    backgroundColor = usedColors.sampleGradientAt(aboveFraction),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = labelStyle,
                )
            },
            // Below label show the pressure left after an emergency/bailout, or a warning/error if there isn't enough gas for that.
            belowLabel = TrackLabel(fraction = belowFraction) {
                if (showEmergencyPressure) {
                    PressureLabel(
                        text = pressureLeftWithEmergency.formatPressure(unitSystem, includeUnit = false),
                        isVisible = true,
                        // Match the label color to the bar color at the label's position.
                        backgroundColor = reserveColors.sampleGradientAt(belowFraction),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = labelStyle,
                    )
                } else {
                    PressureLabel(
                        text = buildAnnotatedString {
                            appendIcon(IconFont.WARNING, labelStyle)
                            append(" ${emergencyLabel.lowercase()} < 0")
                        },
                        // At this point either we need to show a warning, or hide the label because:
                        // 1. There is a critical gas shortage in the normal requirement to begin with,
                        //    and an error will be shown over the whole track, no need to show a second warning.
                        // 2. There is no emergency/bailout requirement, for example a CCR oxygen cylinder
                        isVisible = !(pressureLeft == null || pressureLeftWithEmergency == pressureLeft),
                        backgroundColor = insufficientReserveColor,
                        contentColor = if (emergencyIsError) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onWarning
                        },
                        style = labelStyle,
                    )
                }
            },
        ) {

            fun DrawScope.xPositionFor(pressure: Double) =
                (size.width * (pressure / cylinder.pressure)).toFloat()

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier.matchParentSize()
                        .clip(RoundedCornerShape(percent = 50))
                ) {
                    fun alertGradientBrush(alertColor: Color, naturalColor: Color, endX: Float) =
                        Brush.horizontalGradient(
                            listOf(alertColor, naturalColor),
                            startX = 0f,
                            endX = endX,
                        )

                    when {
                        pressureLeft == null -> {
                            drawRect(color = errorColor, size = size)
                        }

                        pressureLeftWithEmergency == null -> {
                            drawRect(
                                brush = alertGradientBrush(
                                    alertColor = insufficientReserveColor,
                                    naturalColor = reserveColors.sampleGradientAt(aboveFraction),
                                    endX = xPositionFor(pressureLeft),
                                ),
                                size = Size(xPositionFor(pressureLeft), size.height),
                            )
                            drawRect(
                                brush = usedBrush,
                                topLeft = Offset(xPositionFor(pressureLeft), 0f),
                                size = Size(
                                    size.width - xPositionFor(pressureLeft),
                                    size.height
                                ),
                            )
                        }

                        else -> {
                            drawRect(
                                brush = unusedBrush,
                                size = Size(
                                    xPositionFor(pressureLeftWithEmergency),
                                    size.height)
                            )
                            drawRect(
                                brush = reserveBrush,
                                topLeft = Offset(xPositionFor(pressureLeftWithEmergency), 0f),
                                size = Size(
                                    xPositionFor(pressureLeft) - xPositionFor(pressureLeftWithEmergency),
                                    size.height
                                )
                            )
                            drawRect(
                                brush = usedBrush,
                                topLeft = Offset(xPositionFor(pressureLeft), 0f),
                                size = Size(size.width - xPositionFor(pressureLeft), size.height)
                            )
                        }
                    }
                }

                // Draw vertical lines, these are not clipped to the rounded corners
                Canvas(modifier = Modifier.matchParentSize()) {
                    pressureLeft?.let {
                        drawLine(
                            color = boundaryLineColor,
                            start = Offset(xPositionFor(it), 0f),
                            end = Offset(xPositionFor(it), size.height),
                            strokeWidth = lineWidth.toPx()
                        )
                    }
                    if (showEmergencyPressure) {
                        drawLine(
                            color = boundaryLineColor,
                            start = Offset(xPositionFor(pressureLeftWithEmergency), 0f),
                            end = Offset(xPositionFor(pressureLeftWithEmergency), size.height),
                            strokeWidth = lineWidth.toPx()
                        )
                    }
                }

                // Added to ever row, but potentially invisible, to make sure they all remain the
                // same height based on the text that can potentially show in them.
                val criticalShortage = if (pressureLeft == null) { "Critical gas shortage" } else { null }
                PressureLabel(
                    text = criticalShortage.orEmpty(),
                    isVisible = criticalShortage != null,
                    backgroundColor = errorColor,
                    contentColor = MaterialTheme.colorScheme.onError,
                    style = labelStyle,
                )
            }
        }

        Text(
            modifier = Modifier.padding(start = 8.dp),
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            text = cylinder.pressure.formatPressure(unitSystem, includeUnit = false)
                .padStart(fillPressureDigits, FIGURE_SPACE),
        )
    }
}

@Composable
private fun PressureLabel(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    style: TextStyle,
    isVisible: Boolean = true,
) = PressureLabel(
    text = AnnotatedString(text),
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    style = style,
    isVisible = isVisible,
)

@Composable
private fun PressureLabel(
    text: AnnotatedString,
    backgroundColor: Color,
    contentColor: Color,
    style: TextStyle,
    isVisible: Boolean = true,
) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .ifTrue(!isVisible) { invisible() }
            .ifTrue(!isVisible) { clearAndSetSemantics {} }
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        text = text,
        // Trims the line's ascent/descent space so that text centers more natural in case of a single line label.
        style = style.copy(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            )
        ),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview
@Composable
fun CylinderPressureRowStatesPreview() = PreviewWrapper {
    val requirements = listOf(
        // CCR: Bailout only cylinder
        CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Air), 0.0, 800.0),
        // CCR: Loop only cylinder (oxygen)
        CylinderGasRequirements(Cylinder.steel3LiterOxygen(), 200.0, 0.0),
        // OC: Enough normal and reserve gas
        CylinderGasRequirements(Cylinder.steel12Liter(Gas.Nitrox50), 800.0, 1200.0),
        // OC: Enough normal gas but not enough reserve
        CylinderGasRequirements(Cylinder.steel12Liter(Gas.Nitrox50), 1900.0, 900.0),
        // OC: Not enough normal gas
        CylinderGasRequirements(Cylinder.steel12Liter(Gas.Air), 2800.0, 400.0),
    )
    AbysnerTheme {
        Surface {
            Column {
                GasPlanBarChart(
                    modifier = Modifier.padding(16.dp),
                    gasPlan = requirements.toImmutableList(),
                    unitSystem = UnitSystem.METRIC,
                    emergencyLabel = "Reserve",
                    emergencyIsError = false,
                )
            }
        }
    }
}

private val lineWidth = 2.dp

private const val gradientDifference = 0.15f
