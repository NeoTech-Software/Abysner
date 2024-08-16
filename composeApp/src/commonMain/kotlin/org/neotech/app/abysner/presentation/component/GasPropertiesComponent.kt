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

package org.neotech.app.abysner.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import kotlin.math.round

@Composable
fun GasPropertiesComponent(
    modifier: Modifier,
    gas: Gas?,
    maxPPO2: Double,
    maxDensity: Double,
    environment: Environment,
    showTopRow: Boolean = true,
) {

    val mix: String
    val nitrogenPercentage: String
    if(gas != null) {
        val oxygenPercentageInt = round(gas.oxygenFraction * 100).toInt()
        val heliumPercentageInt = round(gas.heliumFraction * 100).toInt()
        mix = "$oxygenPercentageInt/$heliumPercentageInt"
        nitrogenPercentage = "${100 - oxygenPercentageInt - heliumPercentageInt}%"
    } else {
        mix = EMPTY_PLACEHOLDER
        nitrogenPercentage = EMPTY_PLACEHOLDER
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if(showTopRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BigNumberDisplay(
                    modifier = Modifier.weight(0.6f),
                    size = BigNumberSize.LARGE,
                    value = mix,
                    label = "Mix (O2/He)"
                )

                val name = gas?.diveIndustryName() ?: EMPTY_PLACEHOLDER
                BigNumberDisplay(
                    modifier = Modifier.weight(0.4f),
                    size = BigNumberSize.SMALL,
                    value = name,
                    label = "Type"
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            val mod = gas?.let {
                "${round(it.oxygenMod(maxPPO2, environment)).toInt()}m"
            } ?: EMPTY_PLACEHOLDER

            FlipCardComponent(
                modifier = Modifier.weight(0.3f),
                front = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = mod,
                        label = "Oxygen MOD"
                    )
                },
                back = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = maxPPO2.toString(),
                        label = "at PPO2"
                    )
                }
            )

            val densityMod = gas?.let {
                "${round(it.densityMod(maxAllowedDensity = maxDensity, environment = environment)).toInt()}m"
            } ?: EMPTY_PLACEHOLDER

            FlipCardComponent(
                modifier = Modifier.weight(0.3f),
                front = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = densityMod,
                        label = "Density MOD"
                    )
                },
                back = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = maxDensity.toString(),
                        label = "at g/L"
                    )
                }
            )

            FlipCardComponent(
                modifier = Modifier.weight(0.3f),
                front = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = nitrogenPercentage,
                        label = "Nitrogen"
                    )
                },
                back = {
                    BigNumberDisplay(
                        modifier = it,
                        size = BigNumberSize.SMALL,
                        value = "\uD83E\uDD24",
                        label = "Nitrogen"
                    )
                }
            )
        }
    }
}

private const val EMPTY_PLACEHOLDER = "…"
