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

package org.neotech.app.abysner.presentation.screens.planner.gasplan

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.domain.utilities.higherThenDelta
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.component.textfield.ExpandableText
import org.neotech.app.abysner.presentation.getUserReadableMessage
import org.neotech.app.abysner.presentation.preview.PreviewData
import org.neotech.app.abysner.presentation.screens.planner.decoplan.GasPieChart
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GasPieChart(
                            modifier = Modifier.widthIn(max = 350.dp),
                            gasRequirement = gasRequirements
                        )
                    }

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

                    ExpandableText(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = "Note: 'Base' represents the gas requirement for a single diver to normally complete the plan. 'Out-of-air' represents the extra gas that is needed for a safe ascent (including deco) should a buddy lose one or more gas mixes at the worst possible time during the dive (calculated using the out-of-air SAC rate). This out-of-air calculation assumes buddy breathing is possible, however with deco gasses this may not always be the case and you may have to take turns using the deco gas. No extra (bottom) gas is accounted for those situations! Also keep in mind that you need to plan your tanks carefully taking into account 'minimum functional pressure' of your regulators.",
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
            Text(
                modifier = Modifier.weight(0.1f),
                text = "No.",
            )
            Text(
                modifier = Modifier.weight(0.2f),
                text = "Mix",
            )
            Text(
                modifier = Modifier.weight(0.2f),
                text = "Size (ℓ)",
            )
            Text(
                modifier = Modifier.weight(0.3f),
                text = "Usage (bar)"
            )
        }
    ) {

        val gasRequirements = divePlanSet.gasPlan
        gasRequirements.forEachIndexed { index, usage ->
            row {
                Text(
                    modifier = Modifier.weight(0.1f),
                    text = (index + 1).toString(),
                )
                Text(
                    modifier = Modifier.weight(0.2f),
                    text = usage.gas.gas.toString(),
                )
                val size = DecimalFormat.format(1, usage.gas.waterVolume)
                Text(
                    modifier = Modifier.weight(0.2f),
                    text = size,
                )

                // TODO extract these values to a CylinderUsageModel? That is calculated as part of the gas plan?
                val endPressureBase = usage.gas.pressureAfter(volumeUsage = usage.amount)
                val endPressure = usage.gas.pressureAfter(volumeUsage = usage.amountTotal)

                val startPressure = DecimalFormat.format(0, usage.gas.pressure)

                var alertSeverity: AlertSeverity = AlertSeverity.NONE
                val pressureText = buildAnnotatedString {
                    if (endPressureBase == null && endPressure == null) {
                        alertSeverity = AlertSeverity.ERROR
                        append("$startPressure > empty")
                        appendIcon(IconFont.WARNING)
                    } else if(endPressure == null && endPressureBase != null) {
                        alertSeverity = AlertSeverity.ERROR
                        append("$startPressure > ${endPressureBase.format(0)} (")
                        appendIcon(IconFont.WARNING)
                        append("0)")
                    } else {
                        alertSeverity = AlertSeverity.NONE
                        append("$startPressure > ${endPressureBase!!.format(0)} (${endPressure!!.format(0)})")
                    }
                }

                TextAlert(
                    modifier = Modifier.weight(0.3f),
                    alertSeverity = alertSeverity,
                    text = pressureText
                )
            }
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
            Text(
                modifier = Modifier.weight(0.2f),
                text = "Mix",
            )
            Text(
                modifier = Modifier.weight(0.3f),
                text = "Depth (m)",
            )
            Text(
                modifier = Modifier.weight(0.3f),
                text = "Density (g/ℓ)",
            )
            Text(
                modifier = Modifier.weight(0.2f),
                text = "PPO2"
            )
        }
    ) {

        (divePlanSet.base.maximumGasDensities)
            .distinct().sortedBy { it.gas.oxygenFraction }.forEach {

                row {
                    Text(
                        modifier = Modifier.weight(0.2f),
                        text = it.gas.toString(),
                    )

                    Text(
                        modifier = Modifier.weight(0.3f),
                        text = "${it.depth.toInt()}m",
                    )

                    val density = DecimalFormat.format(2, it.density)

                    val alertSeverityDensity =
                        if (it.density.higherThenDelta(Gas.MAX_GAS_DENSITY, 0.01)) {
                            AlertSeverity.ERROR
                        } else if (it.density.higherThenDelta(Gas.MAX_RECOMMENDED_GAS_DENSITY, 0.01)) {
                            AlertSeverity.WARNING
                        } else {
                            AlertSeverity.NONE
                        }

                    TextAlert(
                        alertSeverity = alertSeverityDensity,
                        modifier = Modifier.weight(0.3f),
                        text = density,
                    )

                    val ppo2 = DecimalFormat.format(2, it.ppo2)
                    val alertSeverityPPO2 =
                        if (it.ppo2.higherThenDelta(Gas.MAX_PPO2, 0.01)) {
                            AlertSeverity.ERROR
                        } else {
                            AlertSeverity.NONE
                        }

                    TextAlert(
                        alertSeverity = alertSeverityPPO2,
                        modifier = Modifier.weight(0.2f),
                        text = ppo2,
                    )
                }
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
