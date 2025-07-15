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

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.core.physics.ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlin.math.round

@Composable
fun GasPropertiesComponent(
    modifier: Modifier = Modifier,
    gas: Gas?,
    maxPPO2: Double,
    maxPPO2Secondary: Double?,
    maxDensity: Double,
    environment: Environment,
    showTopRow: Boolean = true,
    onClickMix: (() -> Unit)? = null
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

    val showSecondaryPPO2 = maxPPO2Secondary != null && maxPPO2Secondary != maxPPO2

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if(showTopRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BigNumberDisplay(
                    modifier = Modifier.weight(0.6f),
                    size = BigNumberSize.LARGE,
                    // 4 is widest number found in this character set
                    widestEstimatedValue = "44/44",
                    value = mix,
                    label = "Mix (O2/He)",
                    onClick = onClickMix,
                    showDropDown = onClickMix != null
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
        Row(modifier = Modifier.wrapContentHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            val mod = gas?.let {
                "${round(it.oxygenMod(maxPPO2, environment)).toInt()}m"
            } ?: EMPTY_PLACEHOLDER

            FlipCardComponent(
                modifier = Modifier.weight(0.3f),
                front = { modifier, onClick ->
                    BigNumberDisplay(
                        modifier = modifier,
                        onClick = onClick,
                        size = BigNumberSize.SMALL,
                        value = mod,
                        label = "O2 MOD (${maxPPO2.format(1)})",
                    )
                },
                back = { modifier, onClick ->
                    BigNumberDisplay(
                        modifier = modifier,
                        onClick = onClick,
                        size = BigNumberSize.SMALL,
                        value = maxPPO2.toString(),
                        label = "at PPO2"
                    )
                }
            )

            if(showSecondaryPPO2) {

                val modSecondary = gas?.let {
                    "${round(it.oxygenMod(maxPPO2Secondary!!, environment)).toInt()}m"
                } ?: EMPTY_PLACEHOLDER

                FlipCardComponent(
                    modifier = Modifier.weight(0.3f),
                    front = { modifier, onClick ->
                        BigNumberDisplay(
                            modifier = modifier,
                            onClick = onClick,
                            size = BigNumberSize.SMALL,
                            value = modSecondary,
                            label = "O2 MOD (${maxPPO2Secondary!!.format(1)})",
                        )
                    },
                    back = { modifier, onClick ->
                        BigNumberDisplay(
                            modifier = modifier,
                            onClick = onClick,
                            size = BigNumberSize.SMALL,
                            value = maxPPO2Secondary.toString(),
                            label = "at PPO2",
                        )
                    }
                )
            }

            val densityMod = gas?.let {
                "${round(it.densityMod(maxAllowedDensity = maxDensity, environment = environment)).toInt()}m"
            } ?: EMPTY_PLACEHOLDER

            FlipCardComponent(
                modifier = Modifier.weight(0.3f),
                front = { modifier, onClick ->
                    BigNumberDisplay(
                        modifier = modifier,
                        onClick = onClick,
                        size = BigNumberSize.SMALL,
                        value = densityMod,
                        label = "Density MOD"
                    )
                },
                back = { modifier, onClick ->
                    BigNumberDisplay(
                        modifier = modifier,
                        onClick = onClick,
                        size = BigNumberSize.SMALL,
                        value = maxDensity.toString(),
                        label = "at g/L"
                    )
                }
            )

            if(!showSecondaryPPO2) {
                FlipCardComponent(
                    modifier = Modifier.weight(0.3f),
                    front = { modifier, onClick ->
                        BigNumberDisplay(
                            modifier = modifier,
                            onClick = onClick,
                            size = BigNumberSize.SMALL,
                            value = nitrogenPercentage,
                            label = "Nitrogen"
                        )
                    },
                    back = { modifier, onClick ->
                        BigNumberDisplay(
                            modifier = modifier,
                            onClick = onClick,
                            size = BigNumberSize.SMALL,
                            value = "\uD83E\uDD24",
                            label = "Nitrogen"
                        )
                    }
                )
            }
        }
    }
}

private const val EMPTY_PLACEHOLDER = "…"

@Preview
@Composable
private fun GasPropertiesComponentPreview() {
    AbysnerTheme {
        GasPropertiesComponent(
            modifier = Modifier.wrapContentHeight(),
            gas = Gas.Air,
            maxPPO2 = 1.4,
            maxPPO2Secondary = 1.6,
            maxDensity = 6.8,
            environment = Environment(Salinity.WATER_FRESH, ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL),
            showTopRow = true
        )
    }
}
