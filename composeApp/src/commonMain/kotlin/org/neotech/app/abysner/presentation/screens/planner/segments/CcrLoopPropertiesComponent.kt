/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.Salinity
import org.neotech.app.abysner.domain.core.physics.ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.utilities.format
import org.neotech.app.abysner.presentation.component.BigNumberDisplay
import org.neotech.app.abysner.presentation.component.BigNumberSize
import org.neotech.app.abysner.presentation.component.InfoPill
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.theme.onWarning
import org.neotech.app.abysner.presentation.theme.warning

@Composable
fun CcrLoopPropertiesComponent(
    modifier: Modifier = Modifier,
    depth: Int,
    setpoint: Double,
    diluent: Gas,
    environment: Environment,
) {
    val ambientPressure = depthInMetersToBar(depth.toDouble(), environment).value
    val inspiredGas = diluent.inspiredGas(ambientPressure, setpoint)
    val inspiredDensity = inspiredGas.densityAtDepth(depth.toDouble(), environment)

    val (densityContainerColor, densityValueColor) = when {
        inspiredDensity > Gas.MAX_GAS_DENSITY -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        inspiredDensity > Gas.MAX_RECOMMENDED_GAS_DENSITY -> MaterialTheme.colorScheme.warning to MaterialTheme.colorScheme.onWarning
        else -> Color.Unspecified to Color.Unspecified
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoPill(label = "Diluent", value = diluent.toString())
            InfoPill(label = "Setpoint", value = "${setpoint.format(1)} bar")
            InfoPill(label = "Type", value = diluent.diveIndustryName())
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigNumberDisplay(
                modifier = Modifier.weight(1f),
                size = BigNumberSize.SMALL,
                value = inspiredGas.toString(),
                label = "Inspired mix (O2/He)",
            )
            BigNumberDisplay(
                modifier = Modifier.weight(1f),
                size = BigNumberSize.SMALL,
                value = inspiredDensity.format(2),
                label = "Inspired density (g/L)",
                containerColor = densityContainerColor,
                valueColor = densityValueColor,
            )
        }
    }
}

@Preview
@Composable
private fun CcrLoopPropertiesComponentPreview() {
    AbysnerTheme {
        Surface {
            CcrLoopPropertiesComponent(
                depth = 30,
                setpoint = 1.3,
                diluent = Gas.Air,
                environment = Environment.Default,
            )
        }
    }
}

@Preview
@Composable
private fun CcrLoopPropertiesComponentTrimixPreview() {
    AbysnerTheme {
        Surface {
            CcrLoopPropertiesComponent(
                depth = 60,
                setpoint = 1.3,
                diluent = Gas.Trimix2135,
                environment = Environment(
                    salinity = Salinity.WATER_SALT,
                    atmosphericPressure = ATMOSPHERIC_PRESSURE_AT_SEA_LEVEL
                ),
            )
        }
    }
}
