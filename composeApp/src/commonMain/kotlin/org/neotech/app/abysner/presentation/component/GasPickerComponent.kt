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
        Text(
            text = "Oxygen",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        SliderWithButtons(
            value = initialOxygenPercentage.toFloat(),
            onValueChange = {

                val newNitrogenPercentage = (100 - (round(it).toInt() + initialHeliumPercentage))

                val newHeliumPercentage: Int
                if (newNitrogenPercentage < 0) {
                    newHeliumPercentage = initialHeliumPercentage + newNitrogenPercentage
                } else if(newNitrogenPercentage > 79) {
                    // Don't allow a nitrogen percentage above 79% (that would not be a normal diving gas)
                    // Instead increase helium percentage
                    newHeliumPercentage = initialHeliumPercentage + (newNitrogenPercentage - 79)
                } else {
                    newHeliumPercentage = initialHeliumPercentage
                }
                val oxygenPercentage = round(it).toInt()
                onGasChanged(oxygenPercentage, newHeliumPercentage)
            },
            valueRange = 4f..100f
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "Helium",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        SliderWithButtons(
            value = initialHeliumPercentage.toFloat(),
            onValueChange = {
                val newNitrogenPercentage = (100 - (initialOxygenPercentage + round(it).toInt()))

                var newOxygenPercentage: Int = initialOxygenPercentage
                if (newNitrogenPercentage < 0) {
                    newOxygenPercentage = initialOxygenPercentage + newNitrogenPercentage
                    // Don't allow oxygen percentage lower then 4%
                    if (newOxygenPercentage < 4) {
                        newOxygenPercentage = 4
                        return@SliderWithButtons
                    }
                } else if(newNitrogenPercentage > 79) {
                    // Don't allow a nitrogen percentage above 79% (that would not be a normal diving gas)
                    // Instead increase oxygen percentage
                    newOxygenPercentage = initialOxygenPercentage + (newNitrogenPercentage - 79)
                }
                val heliumPercentage = round(it).toInt()
                onGasChanged(newOxygenPercentage, heliumPercentage)
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
