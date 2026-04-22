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

package org.neotech.app.abysner.presentation.screens.planner.cylinders

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.presentation.utilities.ModalTarget
import org.neotech.app.abysner.presentation.component.GasPickerComponent
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.BottomSheetHeader
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.clearFocusOutside
import org.neotech.app.abysner.presentation.component.recordLayoutCoordinates
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CylinderPickerBottomSheetHost(
    show: ModalTarget<Cylinder>?,
    configuration: Configuration,
    diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    cylinders: List<PlannedCylinderModel> = emptyList(),
    segments: List<DiveProfileSection> = emptyList(),
    onDismiss: () -> Unit,
    onAddCylinder: (Cylinder) -> Unit,
    onUpdateCylinder: (Cylinder) -> Unit,
    onAvailableForBailoutChanged: (Cylinder, Boolean) -> Unit = { _, _ -> },
) {
    if (show != null) {
        val cylinderBeingEdited = (show as? ModalTarget.Edit)?.value
        val plannedCylinder = cylinderBeingEdited?.let { edited ->
            cylinders.firstOrNull { it.cylinder.uniqueIdentifier == edited.uniqueIdentifier }
        }
        val lockGas = plannedCylinder?.isCcrOxygen == true

        val gasesInSegments = segments.mapTo(mutableSetOf()) { it.cylinder.gas }
        val showBailoutToggle = diveMode.isCcr
                && plannedCylinder != null
                && !plannedCylinder.isCcrOxygen
                && plannedCylinder.cylinder.gas in gasesInSegments

        CylinderPickerBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            isAdd = cylinderBeingEdited == null,
            initialValue = cylinderBeingEdited,
            environment = configuration.environment,
            maxPPO2 = configuration.maxPPO2,
            maxPPO2Secondary = configuration.maxPPO2Deco,
            lockGas = lockGas,
            showBailoutToggle = showBailoutToggle,
            initialBailoutValue = plannedCylinder?.isAvailableForBailout ?: true,
            onBailoutToggled = { value ->
                cylinderBeingEdited?.let { onAvailableForBailoutChanged(it, value) }
            },
            onAddOrUpdateCylinder = {
                if (cylinderBeingEdited != null) {
                    onUpdateCylinder(it)
                } else {
                    onAddCylinder(it)
                }
            },
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CylinderPickerBottomSheet(
    isAdd: Boolean,
    initialValue: Cylinder?,
    environment: Environment,
    maxPPO2: Double,
    maxPPO2Secondary: Double,
    lockGas: Boolean = false,
    showBailoutToggle: Boolean = false,
    initialBailoutValue: Boolean = true,
    onBailoutToggled: (Boolean) -> Unit = {},
    sheetState: SheetState = rememberStandardBottomSheetState(),
    onAddOrUpdateCylinder: (cylinder: Cylinder) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ModalBottomSheet(
        dragHandle = {},
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        CylinderPickerBottomSheetContent(
            isAdd = isAdd,
            initialValue = initialValue ?: Cylinder(Gas.Air, 232.0, 10.0),
            environment = environment,
            maxPPO2 = maxPPO2,
            maxPPO2Secondary = maxPPO2Secondary,
            lockGas = lockGas,
            showBailoutToggle = showBailoutToggle,
            initialBailoutValue = initialBailoutValue,
            onBailoutToggled = onBailoutToggled,
            sheetState = sheetState,
            onAddOrUpdateCylinder = onAddOrUpdateCylinder,
            onDismissRequest = onDismiss
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
    lockGas: Boolean = false,
    showBailoutToggle: Boolean = false,
    initialBailoutValue: Boolean = true,
    onBailoutToggled: (Boolean) -> Unit = {},
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
    val isVolumeValid = remember { mutableStateOf(true) }

    var startPressure: Double? by remember(initialValue) {
        mutableStateOf(initialValue.pressure)
    }
    val isStartPressureValid = remember { mutableStateOf(true) }

    val textFieldPositions = mutableStateMapOf<String, LayoutCoordinates>()

    val showStandardGasPickerDialog = remember { mutableStateOf(false) }

    val dismiss: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheetScaffold(
        modifier = Modifier.clearFocusOutside(textFieldPositions),
        header = {
            BottomSheetHeader(
                title = if (lockGas) { "Cylinder" } else { "Gas & cylinder" },
                primaryLabel = if (isAdd) { "Add" } else { "Update" },
                primaryEnabled = isVolumeValid.value && isStartPressureValid.value,
                onClose = dismiss,
                onPrimary = {
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
                    dismiss()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {            val errorMessageVolume = remember { mutableStateOf<String?>(null) }
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

            if (showBailoutToggle) {
                var bailoutChecked by remember { mutableStateOf(initialBailoutValue) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Available for bail-out",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Switch(
                            checked = bailoutChecked,
                            onCheckedChange = {
                                bailoutChecked = it
                                onBailoutToggled(it)
                            },
                        )
                    }
                }
            }

            GasPropertiesComponent(
                modifier = Modifier.padding(vertical = 16.dp),
                gas = gas,
                maxPPO2 = maxPPO2,
                maxPPO2Secondary = maxPPO2Secondary,
                maxDensity = Gas.MAX_GAS_DENSITY,
                environment = environment,
                onClickMix = if (lockGas) { null } else {
                    { showStandardGasPickerDialog.value = true }
                },
            )

            if (!lockGas) {
                GasPickerComponent(
                    initialOxygenPercentage = oxygenPercentage,
                    initialHeliumPercentage = heliumPercentage,
                    onGasChanged = { oxygenFraction, heliumFraction ->
                        oxygenPercentage = oxygenFraction
                        heliumPercentage = heliumFraction
                    }
                )
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
    AbysnerTheme {
        CylinderPickerBottomSheet(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            environment = Environment.Default,
            maxPPO2 = 1.4,
            maxPPO2Secondary = 1.6,
            isAdd = true,
            initialValue = Cylinder.steel10Liter(Gas.Air)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun GasPickerBottomSheetBailoutPreview() {
    AbysnerTheme {
        CylinderPickerBottomSheet(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            environment = Environment.Default,
            maxPPO2 = 1.4,
            maxPPO2Secondary = 1.6,
            isAdd = false,
            showBailoutToggle = true,
            initialBailoutValue = true,
            initialValue = Cylinder.aluminium80Cuft(Gas.Trimix1555)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun GasPickerBottomSheetLockedGasPreview() {
    AbysnerTheme {
        CylinderPickerBottomSheet(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            environment = Environment.Default,
            maxPPO2 = 1.4,
            maxPPO2Secondary = 1.6,
            isAdd = false,
            lockGas = true,
            initialValue = Cylinder.steel3LiterOxygen()
        )
    }
}

