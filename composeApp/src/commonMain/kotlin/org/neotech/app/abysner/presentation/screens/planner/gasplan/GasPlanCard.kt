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

package org.neotech.app.abysner.presentation.screens.planner.gasplan

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.gasplanning.model.CylinderGasRequirements
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.domain.utilities.higherThenDelta
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.component.appendBold
import org.neotech.app.abysner.presentation.component.appendBoldLine
import org.neotech.app.abysner.presentation.component.appendBulletPoint
import org.neotech.app.abysner.presentation.component.textfield.ExpandableText
import org.neotech.app.abysner.presentation.getUserReadableMessage
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.screens.planner.decoplan.LoadingBoxWithBlur
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.IconFont
import org.neotech.app.abysner.presentation.theme.appendIcon

@Composable
fun GasPlanCardComponent(
    modifier: Modifier = Modifier,
    divePlanSet: DivePlanSet?,
    planningException: Throwable?,
    isLoading: Boolean,
) {

    val errorMessage: String? = planningException?.getUserReadableMessage()

    Card(modifier = modifier) {

        LoadingBoxWithBlur(isLoading) { loadingModifier ->

            Column(
                modifier = loadingModifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    text = buildAnnotatedString {
                        append("Gas plan")
                        withStyle(MaterialTheme.typography.titleSmall.toSpanStyle()) {
                            if(divePlanSet?.isDeeper == true) {
                                append(" +${divePlanSet.deeper}m")
                            }
                            if(divePlanSet?.isLonger == true) {
                                append(" +${divePlanSet.longer}min")
                            }
                        }
                    }
                )

                if (divePlanSet == null || divePlanSet.isEmpty) {
                    if (errorMessage != null) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Nothing to see here, plan a dive first \uD83D\uDE09",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    val gasRequirements = divePlanSet.gasPlan

                    var showCylinderDetails: Int? by remember(gasRequirements) { mutableStateOf(null) }

                    showCylinderDetails?.let { index ->
                        GasUsageDetailsDialog(gasPlan = gasRequirements, index = index) {
                            showCylinderDetails = null
                        }
                    }

                    GasPlanBarChart(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        gasPlan = gasRequirements,
                    ) { index, _ ->
                        showCylinderDetails = index
                    }

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            .padding(horizontal = 16.dp),
                        text = "Totals (ℓ)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    GasTotalsTable(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        gasPlan = gasRequirements,
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            .padding(horizontal = 16.dp),
                        text = "Cylinders",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    CylindersTable(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        divePlanSet = divePlanSet,
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            .padding(horizontal = 16.dp),
                        text = "Limits",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    GasLimitsTable(
                        modifier = Modifier.padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        divePlanSet
                    )

                    val explanationText = buildAnnotatedString {
                        appendBoldLine("Notes on the gas plan:")
                        appendLine("The gas plan calculates cylinder pressures based on real-world gas behavior, rather than relying on simplified gas law assumptions. The bar chart divides gas usage into three key categories:")
                        appendBulletPoint {
                            appendBold("Normal: ")
                            append("The pressure required (in bars) for a single diver to complete the planned dive under normal conditions.")
                        }
                        appendBulletPoint {
                            appendBold("Emergency: ")
                            append("This reserve pressure ensures a safe ascent if a buddy loses one or more gas mixes at the worst point in the dive. It assumes buddy breathing is possible for all cylinders, including during decompression stops.")
                        }
                        appendBulletPoint {
                            appendBold("Unused: ")
                            append("Any remaining pressure after accounting for both Normal and Emergency requirements.\n")
                        }
                        appendIcon(IconFont.WARNING)
                        appendBold(" Always plan your cylinders carefully, considering the “minimum functional pressure” of your regulators.")
                    }

                    ExpandableText(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        annotatedText = explanationText,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )

                }
            }
        }
    }
}

@Composable
fun CylindersTable(
    modifier: Modifier = Modifier,
    divePlanSet: DivePlanSet,
) {
    Table(
        modifier = modifier,
        header = {
            Text(modifier = Modifier.weight(0.3f), text = "Mix")
            Text(modifier = Modifier.weight(0.3f), text = "Size (ℓ)")
            Text(modifier = Modifier.weight(0.4f), text = "Usage (bar)")
        }
    ) {
        rows(divePlanSet.gasPlan, key = { it.cylinder.uniqueIdentifier }) { usage ->
            Text(
                modifier = Modifier.weight(0.3f),
                text = usage.cylinder.gas.toString(),
            )
            Text(
                modifier = Modifier.weight(0.3f),
                text = DecimalFormat.format(1, usage.cylinder.waterVolume),
            )

            // TODO extract these values to a CylinderUsageModel? That is calculated as part of the gas plan?
            val endPressureBase = usage.cylinder.pressureAfter(volumeUsage = usage.normalRequirement)
            val endPressure = usage.cylinder.pressureAfter(volumeUsage = usage.totalGasRequirement)
            val startPressure = DecimalFormat.format(0, usage.cylinder.pressure)

            var alertSeverity: AlertSeverity = AlertSeverity.NONE
            val pressureText = buildAnnotatedString {
                if (endPressureBase == null && endPressure == null) {
                    alertSeverity = AlertSeverity.ERROR
                    append("$startPressure > empty")
                    appendIcon(IconFont.WARNING)
                } else if (endPressure == null && endPressureBase != null) {
                    alertSeverity = AlertSeverity.WARNING
                    append("$startPressure > ${endPressureBase.format(0)} (")
                    appendIcon(IconFont.WARNING)
                    append("0)")
                } else {
                    alertSeverity = AlertSeverity.NONE
                    append("$startPressure > ${endPressureBase!!.format(0)} (${endPressure!!.format(0)})")
                }
            }

            TextAlert(
                modifier = Modifier.weight(0.4f),
                alertSeverity = alertSeverity,
                text = pressureText,
            )
        }
    }
}

@Composable
fun GasTotalsTable(
    modifier: Modifier = Modifier,
    gasPlan: List<CylinderGasRequirements>,
) {
    Table(
        modifier = modifier,
        header = {
            Text(modifier = Modifier.weight(0.17f), text = "Mix")
            Text(modifier = Modifier.weight(0.29f), text = "Available")
            Text(modifier = Modifier.weight(0.19f), text = "Normal")
            Text(modifier = Modifier.weight(0.27f), text = "Emergency")
        }
    ) {
        rows(gasPlan.groupBy { it.cylinder.gas }.toList(), key = { (gas, _) -> gas }) { (gas, entries) ->
            val totalNormal = entries.sumOf { it.normalRequirement }
            val totalRequired = entries.sumOf { it.totalGasRequirement }
            val totalCapacity = entries.sumOf { it.cylinder.capacity() }

            val alertSeverity = when {
                totalNormal > totalCapacity -> AlertSeverity.ERROR
                totalRequired > totalCapacity -> AlertSeverity.WARNING
                else -> AlertSeverity.NONE
            }

            Text(modifier = Modifier.weight(0.17f), text = gas.toString())
            Text(modifier = Modifier.weight(0.29f), text = DecimalFormat.format(0, totalCapacity))
            Text(modifier = Modifier.weight(0.19f), text = "-${DecimalFormat.format(0, totalNormal)}")
            TextAlert(
                modifier = Modifier.weight(0.27f),
                alertSeverity = alertSeverity,
                text = "-${DecimalFormat.format(0, totalRequired)}",
            )
        }
    }
}

@Composable
fun GasLimitsTable(
    modifier: Modifier = Modifier,
    divePlanSet: DivePlanSet,
) {
    Table(
        modifier = modifier,
        header = {
            Text(modifier = Modifier.weight(0.2f), text = "Mix")
            Text(modifier = Modifier.weight(0.3f), text = "Depth (m)")
            Text(modifier = Modifier.weight(0.3f), text = "Density (g/ℓ)")
            Text(modifier = Modifier.weight(0.2f), text = "PPO2")
        }
    ) {
        rows(
            divePlanSet.base.maximumGasDensities.distinct().sortedBy { it.gas.oxygenFraction },
            key = { it.gas },
        ) { gasAtDepth ->
            Text(modifier = Modifier.weight(0.2f), text = gasAtDepth.gas.toString())
            Text(modifier = Modifier.weight(0.3f), text = "${gasAtDepth.depth.toInt()}m")

            val alertSeverityDensity = when {
                gasAtDepth.density.higherThenDelta(Gas.MAX_GAS_DENSITY, 0.01) -> AlertSeverity.ERROR
                gasAtDepth.density.higherThenDelta(Gas.MAX_RECOMMENDED_GAS_DENSITY, 0.01) -> AlertSeverity.WARNING
                else -> AlertSeverity.NONE
            }
            TextAlert(
                modifier = Modifier.weight(0.3f),
                alertSeverity = alertSeverityDensity,
                text = DecimalFormat.format(2, gasAtDepth.density),
            )

            val alertSeverityPPO2 = if (gasAtDepth.ppo2.higherThenDelta(Gas.MAX_PPO2, 0.01)) {
                AlertSeverity.ERROR
            } else {
                AlertSeverity.NONE
            }
            TextAlert(
                modifier = Modifier.weight(0.2f),
                alertSeverity = alertSeverityPPO2,
                text = DecimalFormat.format(2, gasAtDepth.ppo2),
            )
        }
    }
}

@Preview
@Composable
private fun GasPlanCardComponentPreview() {
    AbysnerTheme {
        GasPlanCardComponent(
            divePlanSet = PreviewData.divePlan,
            planningException = null,
            isLoading = false
        )
    }
}
