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

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_baseline_check_circle_24
import abysner.composeapp.generated.resources.ic_outline_dangerous_24
import abysner.composeapp.generated.resources.ic_outline_warning_24
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.gasplanning.model.GasPlan
import kotlinx.collections.immutable.persistentListOf
import org.neotech.app.abysner.domain.utilities.format
import androidx.compose.ui.text.font.FontWeight
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.appendBoldLine
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning
import kotlin.math.roundToInt

@Composable
fun GasUsageDetailsDialog(
    gasPlan: GasPlan,
    index: Int,
    onDismissRequest: () -> Unit
) {
    val cylinderGasRequirements = gasPlan[index]
    val sameMixCylinders = gasPlan.filter { it.cylinder.gas == cylinderGasRequirements.cylinder.gas }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("OK")
            }
        },
        text = {
            Column {

                Text(
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = "Cylinder details",
                    style = MaterialTheme.typography.titleLarge
                )

                val capacity = cylinderGasRequirements.cylinder.capacity()
                val labelWidthState = remember { mutableIntStateOf(0) }

                // Cylinder-specific info
                Table(striped = false, defaultRowModifier = Modifier.padding(vertical = 1.dp)) {
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Contents", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${cylinderGasRequirements.cylinder.gas} (${cylinderGasRequirements.cylinder.gas.diveIndustryName()})")
                    }
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Volume", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${cylinderGasRequirements.cylinder.waterVolume.format(1)} ℓ")
                    }
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Pressure", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${cylinderGasRequirements.cylinder.pressure.format(0)} bar")
                    }
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Available gas", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${capacity.format(0)} ℓ")
                    }
                }

                val totalNormal = sameMixCylinders.sumOf { it.normalRequirement }
                val totalRequired = sameMixCylinders.sumOf { it.totalGasRequirement }
                val totalCapacity = sameMixCylinders.sumOf { it.cylinder.capacity() }

                val showTotals = sameMixCylinders.size > 1

                Text(
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    text = "Required for dive",
                    style = MaterialTheme.typography.titleLarge
                )
                Table(striped = false, defaultRowModifier = Modifier.padding(vertical = 1.dp)) {
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Normal", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${totalNormal.format(0)} ℓ")
                    }
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Emergency", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${totalRequired.format(0)} ℓ")
                    }
                    row {
                        Text(
                            modifier = Modifier.uniformLabelWidth(labelWidthState).padding(end = 8.dp),
                            text = "Available", fontWeight = FontWeight.Bold
                        )
                        Text(modifier = Modifier.weight(1f), text = "${totalCapacity.format(0)} ℓ")
                    }
                }

                // Coverage percentage, only shown if multiple cylinders of the same mix make up the total capacity
                if (showTotals) {
                    val coveragePercent = (capacity / totalCapacity * 100).roundToInt()
                    val cylinderIndex = sameMixCylinders.indexOf(cylinderGasRequirements) + 1
                    val gasName = cylinderGasRequirements.cylinder.gas.toString()
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = "This cylinder ($cylinderIndex out of ${sameMixCylinders.size}) covers $coveragePercent% of the $gasName mix requirement.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val totalCapacityFormatted = "${totalCapacity.format(0)} ℓ"

                // Bar pressure for a single cylinder (what divers read on gauges), liters for
                // multiple cylinders (no meaningful single pressure to show).
                val unusedNormalFormatted = if (showTotals) {
                    "${(totalCapacity - totalNormal).format(0)} ℓ"
                } else {
                    "${cylinderGasRequirements.pressureLeft?.format(0)} bar"
                }
                val unusedEmergencyFormatted = if (showTotals) {
                    "${(totalCapacity - totalRequired).format(0)} ℓ"
                } else {
                    "${cylinderGasRequirements.pressureLeftWithEmergency?.format(0)} bar"
                }

                val severity = when {
                    totalNormal > totalCapacity -> AlertSeverity.ERROR
                    totalRequired > totalCapacity -> AlertSeverity.WARNING
                    else -> AlertSeverity.POSITIVE
                }

                val alertMessage = buildAnnotatedString {
                    when (severity) {
                        AlertSeverity.ERROR -> {
                            if (showTotals) appendBoldLine("Together, these ${sameMixCylinders.size} cylinders have a critical gas shortage!")
                            else appendBoldLine("This cylinder has a critical gas shortage!")
                            append("You need at least ")
                            appendBold("${totalNormal.format(0)} ℓ")
                            append(if (showTotals) " for the dive, but only have a combined " else " for the dive, but only have ")
                            appendBold(totalCapacityFormatted)
                            append(".")
                        }
                        AlertSeverity.WARNING -> {
                            if (showTotals) appendBoldLine("Together, these ${sameMixCylinders.size} cylinders have a gas shortage!")
                            else appendBoldLine("This cylinder has a gas shortage!")
                            append("You need ")
                            appendBold("${totalRequired.format(0)} ℓ")
                            append(if (showTotals) " in case of an emergency, but only have a combined " else " in case of an emergency, but only have ")
                            appendBold(totalCapacityFormatted)
                            append(". Without emergencies, there is enough gas.")
                        }
                        AlertSeverity.POSITIVE, AlertSeverity.NONE -> {
                            if (showTotals) appendBoldLine("Together, these ${sameMixCylinders.size} cylinders have enough gas for the dive, even in an emergency.")
                            else appendBoldLine("This cylinder has enough gas for the dive, even in an emergency.")
                            append("After a normal dive about ")
                            appendBold(unusedNormalFormatted)
                            append(" remains, and about ")
                            appendBold(unusedEmergencyFormatted)
                            append(" after an emergency.")
                        }
                    }
                }

                AlertCard(
                    modifier = Modifier.padding(top = 8.dp),
                    severity = severity
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (severity != AlertSeverity.NONE) {
                            Icon(
                                painter = when (severity) {
                                    AlertSeverity.POSITIVE -> painterResource(Res.drawable.ic_baseline_check_circle_24)
                                    AlertSeverity.WARNING -> painterResource(Res.drawable.ic_outline_warning_24)
                                    AlertSeverity.ERROR -> painterResource(Res.drawable.ic_outline_dangerous_24)
                                    AlertSeverity.NONE -> painterResource(Res.drawable.ic_baseline_check_circle_24)
                                },
                                contentDescription = null
                            )
                        }
                        Text(
                            text = alertMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

private fun Modifier.uniformLabelWidth(
    state: MutableIntState,
): Modifier = onSizeChanged { state.intValue = maxOf(state.intValue, it.width) }
    .layout { measurable, constraints ->
        val minWidthPx = state.intValue
        val placeable = measurable.measure(
            constraints.copy(minWidth = maxOf(constraints.minWidth, minWidthPx))
        )
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

@Composable
fun AlertCard(
    modifier: Modifier = Modifier,
    severity: AlertSeverity,
    content: @Composable () -> Unit,
) {
    val (textColor, backgroundColor) = when(severity) {
        AlertSeverity.NONE -> Color.Unspecified to Color.Unspecified
        AlertSeverity.POSITIVE -> MaterialTheme.colorScheme.onPrimary to MaterialTheme.colorScheme.primary
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onWarning to MaterialTheme.colorScheme.warning
        AlertSeverity.ERROR -> MaterialTheme.colorScheme.onError to MaterialTheme.colorScheme.error
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(
        containerColor = backgroundColor,
        contentColor = textColor
    )) {
        content()
    }
}

@Preview
@Composable
private fun TwoCylindersPositivePreview() {
    val cylinderA = CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Nitrox50, 207.0), 800.0, 300.0)
    val cylinderB = CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Nitrox50, 207.0), 800.0, 300.0)
    GasUsageDetailsDialog(gasPlan = persistentListOf(cylinderA, cylinderB), index = 0) {}
}

@Preview
@Composable
private fun TwoCylindersWarningPreview() {
    val cylinderA = CylinderGasRequirements(Cylinder.steel12Liter(Gas.Nitrox50), 2000.0, 900.0)
    val cylinderB = CylinderGasRequirements(Cylinder.steel12Liter(Gas.Nitrox50), 2000.0, 900.0)
    GasUsageDetailsDialog(gasPlan = persistentListOf(cylinderA, cylinderB), index = 0) {}
}

@Preview
@Composable
private fun TwoCylindersErrorPreview() {
    val cylinderA = CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Air, 200.0), 2500.0, 400.0)
    val cylinderB = CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Air, 200.0), 2500.0, 400.0)
    GasUsageDetailsDialog(gasPlan = persistentListOf(cylinderA, cylinderB), index = 0) {}
}

@Preview
@Composable
private fun OneCylinderPositivePreview() {
    GasUsageDetailsDialog(
        gasPlan = persistentListOf(CylinderGasRequirements(Cylinder.steel12Liter(Gas.Air), 1200.0, 600.0)),
        index = 0,
    ) {}
}

@Preview
@Composable
private fun OneCylinderWarningPreview() {
    GasUsageDetailsDialog(
        gasPlan = persistentListOf(CylinderGasRequirements(Cylinder.aluminium80Cuft(Gas.Nitrox50, 207.0), 1900.0, 600.0)),
        index = 0,
    ) {}
}

@Preview
@Composable
private fun OneCylinderErrorPreview() {
    GasUsageDetailsDialog(
        gasPlan = persistentListOf(CylinderGasRequirements(Cylinder.steel12Liter(Gas.Air), 2800.0, 400.0)),
        index = 0,
    ) {}
}

