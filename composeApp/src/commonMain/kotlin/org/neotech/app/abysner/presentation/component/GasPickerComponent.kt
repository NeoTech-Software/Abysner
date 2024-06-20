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

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.round

@Composable
fun GasPickerComponent(
    modifier: Modifier = Modifier,
    initialOxygenPercentage: Int = 21,
    initialHeliumPercentage: Int = 0,
    onGasChanged: (oxygenPercentage: Int, heliumPercentage: Int) -> Unit = { _, _ -> }
) {
    Column(modifier = modifier) {

        var oxygenPercentage: Int by remember {
            mutableIntStateOf(initialOxygenPercentage)
        }

        var heliumPercentage: Int by remember {
            mutableIntStateOf(initialHeliumPercentage)
        }

        Text(
            text = "Oxygen",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        SliderWithButtons(
            value = oxygenPercentage.toFloat(),
            onValueChange = {

                val newNitrogenPercentage = (100 - (round(it).toInt() + heliumPercentage))

                if (newNitrogenPercentage < 0) {
                    heliumPercentage += newNitrogenPercentage
                } else if(newNitrogenPercentage > 79) {
                    // Don't allow a nitrogen percentage above 79% (that would not be a normal diving gas)
                    // Instead increase helium percentage
                    heliumPercentage += (newNitrogenPercentage - 79)
                }
                oxygenPercentage = round(it).toInt()
                onGasChanged(oxygenPercentage, heliumPercentage)
            },
            valueRange = 4f..100f
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "Helium",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        SliderWithButtons(
            value = heliumPercentage.toFloat(),
            onValueChange = {
                val newNitrogenPercentage = (100 - (oxygenPercentage + round(it).toInt()))
                if (newNitrogenPercentage < 0) {
                    oxygenPercentage += newNitrogenPercentage
                    // Don't allow oxygen percentage lower then 4%
                    if (oxygenPercentage < 4) {
                        oxygenPercentage = 4
                        return@SliderWithButtons
                    }
                } else if(newNitrogenPercentage > 79) {
                    // Don't allow a nitrogen percentage above 79% (that would not be a normal diving gas)
                    // Instead increase oxygen percentage
                    oxygenPercentage += (newNitrogenPercentage - 79)
                }
                heliumPercentage = round(it).toInt()
                onGasChanged(oxygenPercentage, heliumPercentage)
            },
            valueRange = 0f..99f,
        )
    }
}

@Composable
@Preview
private fun GasPickerComponentPreview() {
    GasPickerComponent()
}
