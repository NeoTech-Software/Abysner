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

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.ic_baseline_check_circle_24
import abysner.composeapp.generated.resources.ic_outline_dangerous_24
import abysner.composeapp.generated.resources.ic_outline_warning_24
import org.jetbrains.compose.ui.tooling.preview.Preview
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.appendBoldLine
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning

@Preview
@Composable
private fun GasUsageDetailsDialogPreview() {
    GasUsageDetailsDialog(cylinderGasRequirements = CylinderGasRequirements(Cylinder.steel12Liter(Gas.Air), 3000.0, 700.0)) {

    }
}

@Composable
fun GasUsageDetailsDialog(
    cylinderGasRequirements: CylinderGasRequirements,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("OK")
            }
        },
        title = { Text("Cylinder details") },
        text = {
            Column {

                val capacity = cylinderGasRequirements.cylinder.capacity()

                val litersRequiredTotalFormatted = "${cylinderGasRequirements.totalGasRequirement.format(0)} liters"
                val litersAvailableTotal = "${capacity.format(0)} liters"
                val litersUnusedTotal = "${(capacity - cylinderGasRequirements.totalGasRequirement).format(0)} liters"

                Text(
                    text = buildAnnotatedString {
                        appendBold("Contents: ")
                        appendLine("${cylinderGasRequirements.cylinder.gas} (${cylinderGasRequirements.cylinder.gas.diveIndustryName()})")
                        appendBold("Volume: ")
                        appendLine("${cylinderGasRequirements.cylinder.waterVolume.format(1)} liters")
                        appendBold("Pressure: ")
                        appendLine("${cylinderGasRequirements.cylinder.pressure.format(0)} bar")
                        appendBold("Available gas: ")
                        append("${cylinderGasRequirements.cylinder.capacity().format(0)} liters")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Gas Required",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildAnnotatedString {
                        appendBold("Normal: ")
                        appendLine("${cylinderGasRequirements.normalRequirement.format(0)} liters")
                        appendBold("Emergency: ")
                        appendLine("${cylinderGasRequirements.totalGasRequirement.format(0)} liters")
                        if(cylinderGasRequirements.pressureLeftWithEmergency != null) {
                            appendBold("Unused: ")
                            append(litersUnusedTotal)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                var severity = AlertSeverity.NONE

                // Summery
                val summery = if(cylinderGasRequirements.pressureLeft == null) {

                    severity = AlertSeverity.ERROR

                    buildAnnotatedString {
                        // Critical gas shortage message
                        appendBoldLine("This cylinder has a critical gas shortage!")
                        append("You need a total of ")
                        appendBold(litersRequiredTotalFormatted)
                        append(" to finish the dive, but this cylinder only has ")
                        appendBold(litersAvailableTotal)
                        append("!")
                    }
                } else if(cylinderGasRequirements.pressureLeftWithEmergency == null) {

                    severity = AlertSeverity.WARNING

                    buildAnnotatedString {
                        // Gas shortage with emergency context
                        appendBoldLine("This cylinder has a gas shortage!")
                        append("You need a total of ")
                        appendBold(litersRequiredTotalFormatted)
                        append(" to finish the dive, but you only have ")
                        appendBold(litersAvailableTotal)
                        append(". Without accounting for emergencies, this cylinder would have enough gas, but you should prepare for unexpected situations.")
                    }
                } else {

                    severity = AlertSeverity.POSITIVE

                    val pressureAfterFinishingDiveNormally = "${cylinderGasRequirements.pressureLeft!!.format(0)} bar"
                    val pressureAfterFinishingDiveAbnormally = "${cylinderGasRequirements.pressureLeftWithEmergency!!.format(0)} bar"

                    buildAnnotatedString {
                        appendBoldLine("This cylinder has enough gas for the dive, even if an emergency occurs.")
                        append("After finishing normally, you will have about ")
                        appendBold(pressureAfterFinishingDiveNormally)
                        append(" left. In case of an emergency, this will be about ")
                        appendBold(pressureAfterFinishingDiveAbnormally)
                        append(".")
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
                        if(severity != AlertSeverity.NONE) {
                            Icon(
                                painter = when (severity) {
                                    AlertSeverity.NONE -> painterResource(Res.drawable.ic_baseline_check_circle_24)
                                    AlertSeverity.POSITIVE -> painterResource(Res.drawable.ic_baseline_check_circle_24)
                                    AlertSeverity.WARNING -> painterResource(Res.drawable.ic_outline_warning_24)
                                    AlertSeverity.ERROR -> painterResource(Res.drawable.ic_outline_dangerous_24)
                                },
                                contentDescription = null
                            )
                        }
                        Text(
                            text = summery,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
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
