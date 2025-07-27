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

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
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
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CylinderPickerBottomSheet(
    isAdd: Boolean,
    initialValue: Cylinder?,
    environment: Environment,
    maxPPO2: Double,
    maxPPO2Secondary: Double,
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
            maxPPO2Secondary = maxPPO2Secondary,
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
    maxPPO2Secondary: Double?,
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

    val showStandardGasPickerDialog = remember { mutableStateOf(false) }

    ModalBottomSheetScaffold(
        modifier = Modifier
            .clearFocusOutside(textFieldPositions)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {

            Text(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                text = "Gas & cylinder"
            )

            val errorMessageVolume = remember { mutableStateOf<String?>(null) }
            val errorMessagePressure = remember { mutableStateOf<String?>(null) }

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
                    errorMessage = errorMessageVolume,
                    onNumberChanged = {
                        volume = it
                    },
                    supportingText = null
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
                    errorMessage = errorMessagePressure,
                    onNumberChanged = {
                        startPressure = it
                    },
                    supportingText = null
                )
            }

            val anyErrorMessage = errorMessageVolume.value ?: errorMessagePressure.value
            if(anyErrorMessage != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    minLines = 1,
                    text = anyErrorMessage,
                    color = MaterialTheme.colorScheme.error
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
                maxPPO2Secondary = maxPPO2Secondary,
                maxDensity = Gas.MAX_GAS_DENSITY,
                environment = environment,
                onClickMix = {
                    showStandardGasPickerDialog.value = true
                }
            )

            GasPickerComponent(
                initialOxygenPercentage = oxygenPercentage,
                initialHeliumPercentage = heliumPercentage,
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

    ShowStandardGasPickerDialog(
        show = showStandardGasPickerDialog,
        onGasSelected = {
            oxygenPercentage = it.oxygenPercentage
            heliumPercentage = it.heliumPercentage
        }
    )
}

@Composable
private fun ShowStandardGasPickerDialog(
    show: MutableState<Boolean>,
    onGasSelected: (gas: Gas) -> Unit
) {

    if(show.value) {
        StandardGasPickerDialog(
            onGasSelected = {
                onGasSelected(it)
            },
            onDismissRequest = {
                show.value = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun GasPickerBottomSheetPreview() {
    CylinderPickerBottomSheet(
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded
        ),
        environment = Environment.Default,
        maxPPO2 = 1.4,
        maxPPO2Secondary = 1.6,
        isAdd = true,
        initialValue = Cylinder.steel10Liter(Gas.Air)
    )
}
