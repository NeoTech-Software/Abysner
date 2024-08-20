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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.presentation.screens.planner.decoplan.GasPieChart
import org.neotech.app.abysner.presentation.getUserReadableMessage
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.domain.utilities.higherThenDelta
import org.neotech.app.abysner.presentation.component.AlertSeverity
import org.neotech.app.abysner.presentation.component.Table
import org.neotech.app.abysner.presentation.component.TextAlert
import org.neotech.app.abysner.presentation.component.textfield.ExpandableText
import org.neotech.app.abysner.presentation.screens.planner.decoplan.LoadingBoxWithBlur

@OptIn(ExperimentalTextApi::class)
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
                    text = "Gas plan"
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

                    Table(
                        modifier = Modifier.padding(horizontal = 16.dp),
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

                        gasRequirements.total.forEachIndexed { index, (gas, volume) ->
                            row {
                                Text(
                                    modifier = Modifier.weight(0.1f),
                                    text = (index + 1).toString(),
                                )
                                Text(
                                    modifier = Modifier.weight(0.2f),
                                    text = gas.gas.toString(),
                                )
                                val size = DecimalFormat.format(1, gas.waterVolume)
                                Text(
                                    modifier = Modifier.weight(0.2f),
                                    text = size,
                                )

                                // TODO extract these values to a CylinderUsageModel? That is calculated as part of the gas plan?
                                val endPressureBase =
                                    gas.pressureAfter(volumeUsage = gasRequirements.sortedBase[index].second)
                                val endPressure = gas.pressureAfter(volumeUsage = volume)

                                val startPressure = DecimalFormat.format(0, gas.pressure)

                                val alertSeverity: AlertSeverity
                                val pressureText =
                                    if (endPressureBase == null || endPressure == null) {
                                        alertSeverity = AlertSeverity.ERROR
                                        "$startPressure > empty"
                                    } else {
                                        alertSeverity = AlertSeverity.NONE
                                        "$startPressure > ${
                                            DecimalFormat.format(
                                                0,
                                                endPressureBase
                                            )
                                        } (${DecimalFormat.format(0, endPressure)})"
                                    }

                                TextAlert(
                                    modifier = Modifier.weight(0.3f),
                                    alertSeverity = alertSeverity,
                                    text = pressureText
                                )
                            }
                        }
                    }


                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            .padding(horizontal = 16.dp),
                        text = "Limits",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Table(
                        modifier = Modifier.padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
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


                        (divePlanSet.deeperAndLonger.maximumGasDensities + divePlanSet.base.maximumGasDensities)
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
                    ExpandableText(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = "Note: All gas information is calculated based on the contingency (deeper & longer) plan. 'Baseline' represents the gas requirement for a single diver to normally complete the contingency plan. 'Lost gas extra' represents the extra gas that is needed for a safe ascent (including potential deco) should a team buddy lose one or more gas mixes at the worst-possible time during the dive (calculated using the out-of-air SAC rate). This lost-gas calculation assumes buddy breathing is possible, however with deco gasses this may not always be the case and you may have to take turns using the deco gas. No extra (bottom) gas is accounted for those situations! Also keep in mind that you need to plan your tanks carefully taking into account 'minimum functional pressure' of your regulators.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GasPlanCardComponentPreview() {
    AbysnerTheme {

        val divePlan = DivePlanner().apply {
            configuration = Configuration()
        }.getDecoPlan(
            plan = listOf(
                DiveProfileSection(16, 45, Cylinder.steel12Liter(Gas.Air)),
                DiveProfileSection(16, 35, Cylinder.steel12Liter(Gas(0.28, 0.0))),
            ),
            decoGases = listOf(Cylinder.aluminium80Cuft(Gas.Oxygen50)),
        )

        val gasPlan = GasPlanner().calculateGasPlan(divePlan)

        GasPlanCardComponent(
            divePlanSet = DivePlanSet(divePlan, divePlan, gasPlan),
            planningException = null,
            isLoading = false
        )
    }
}
