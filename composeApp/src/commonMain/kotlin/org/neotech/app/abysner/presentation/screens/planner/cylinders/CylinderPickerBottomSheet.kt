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

package org.neotech.app.abysner.presentation.screens.planner.cylinders

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.presentation.component.GasPickerComponent
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.clearFocusOutside
import org.neotech.app.abysner.presentation.component.recordLayoutCoordinates
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.OutlinedGenericInputField
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.textfield.behavior.DecimalInputBehavior
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CylinderPickerBottomSheet(
    isAdd: Boolean,
    initialValue: Cylinder?,
    environment: Environment,
    maxPPO2: Double,
    sheetState: SheetState = rememberStandardBottomSheetState(),
    onAddOrUpdateCylinder: (cylinder: Cylinder) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        CylinderPickerBottomSheetContent(
            isAdd = isAdd,
            initialValue = initialValue ?: Cylinder(Gas.Air, 232.0, 10.0),
            environment = environment,
            maxPPO2 = maxPPO2,
            sheetState = sheetState,
            onAddOrUpdateCylinder = onAddOrUpdateCylinder,
            onDismissRequest = onDismissRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CylinderPickerBottomSheetContent(
    isAdd: Boolean,
    initialValue: Cylinder = Cylinder(Gas.Air, 232.0, 10.0),
    environment: Environment,
    maxPPO2: Double,
    sheetState: SheetState = rememberStandardBottomSheetState(),
    onAddOrUpdateCylinder: (cylinder: Cylinder) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {

    val scope = rememberCoroutineScope()

    var oxygenPercentage: Int by remember(initialValue) {
        mutableIntStateOf(initialValue.gas.oxygenPercentage)
    }

    var heliumPercentage: Int by remember(initialValue) {
        mutableIntStateOf(initialValue.gas.heliumPercentage)
    }

    var volume: Double? by remember(initialValue) {
        mutableStateOf(initialValue.waterVolume)
    }
    val isVolumeValid = remember { mutableStateOf(false) }

    var startPressure: Double? by remember(initialValue) {
        mutableStateOf(initialValue.pressure)
    }
    val isStartPressureValid = remember { mutableStateOf(false) }

    val textFieldPositions = mutableStateMapOf<String, LayoutCoordinates>()

    ModalBottomSheetScaffold(
        modifier = Modifier
            .clearFocusOutside(textFieldPositions)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                text = "Gas & cylinder"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                OutlinedDecimalInputField(
                    modifier = Modifier.weight(1f)
                        .recordLayoutCoordinates("volume", textFieldPositions),
                    initialValue = initialValue.waterVolume,
                    isValid = isVolumeValid,
                    fractionDigits = 1,
                    visualTransformation = SuffixVisualTransformation(" ℓ"),
                    label = "Volume",
                    minValue = 0.1,
                    maxValue = 50.0,
                    onNumberChanged = {
                        volume = it
                    }
                )

                OutlinedDecimalInputField(
                    modifier = Modifier.weight(1f)
                        .recordLayoutCoordinates("pressure", textFieldPositions),
                    initialValue = initialValue.pressure,
                    label = "Start pressure",
                    minValue = 10.0,
                    maxValue = 300.0,
                    fractionDigits = 0,
                    isValid = isStartPressureValid,
                    visualTransformation = SuffixVisualTransformation(" bar"),
                    onNumberChanged = {
                        startPressure = it
                    }
                )
            }

            val gas = Gas(
                oxygenFraction = oxygenPercentage / 100.0,
                heliumFraction = heliumPercentage / 100.0
            )

            GasPropertiesComponent(
                modifier = Modifier.padding(vertical = 16.dp),
                gas = gas,
                maxPPO2 = maxPPO2,
                maxDensity = Gas.MAX_GAS_DENSITY,
                environment = environment
            )

            GasPickerComponent(
                initialOxygenPercentage = initialValue.gas.oxygenPercentage,
                initialHeliumPercentage = initialValue.gas.heliumPercentage,
                onGasChanged = { oxygenFraction, heliumFraction ->
                    oxygenPercentage = oxygenFraction
                    heliumPercentage = heliumFraction
                }
            )
            Row {
                TextButton(

                    modifier = Modifier
                        .weight(0.5f)
                        .padding(top = 16.dp),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }) {
                    Text("Cancel")
                }
                Button(
                    enabled = isVolumeValid.value,
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(top = 16.dp),
                    onClick = {
                        onAddOrUpdateCylinder(
                            // Copy since we want to maintain the uniqueIdentifier
                            initialValue.copy(
                                gas = Gas(
                                    oxygenFraction = oxygenPercentage / 100.0,
                                    heliumFraction = heliumPercentage / 100.0
                                ),
                                pressure = startPressure!!,
                                waterVolume = volume!!,
                            )

                        )
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }) {
                    Text(if(isAdd) { "Add" } else { "Update" })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun GasPickerBottomSheetPreview() {
    AbysnerTheme {
        CylinderPickerBottomSheetContent(
            environment = Environment.Default,
            maxPPO2 = 1.4,
            isAdd = true,
            sheetState = rememberStandardBottomSheetState(
                SheetValue.Expanded
            )
        )
    }
}
