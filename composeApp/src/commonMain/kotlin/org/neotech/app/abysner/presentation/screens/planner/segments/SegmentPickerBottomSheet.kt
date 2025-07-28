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

package org.neotech.app.abysner.presentation.screens.planner.segments

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.segment_picker_travel_time_hint
import abysner.composeapp.generated.resources.unit_meter
import abysner.composeapp.generated.resources.unit_minute
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.DropDown
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.clearFocusOutside
import org.neotech.app.abysner.presentation.component.recordLayoutCoordinates
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.core.pluralsStringBuilder
import org.neotech.app.abysner.presentation.theme.bodyExtraLarge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentPickerBottomSheet(
    isAdd: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialValue: DiveProfileSection?,
    maxPPO2: Double,
    maxDensity: Double,
    environment: Environment,
    cylinders: List<Cylinder>,
    previousDepth: Double,
    configuration: Configuration,
    onAddOrUpdateDiveSegment: (gas: DiveProfileSection) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    require(cylinders.isNotEmpty()) {
        "SegmentPickerBottomSheet was shown with an empty list of cylinders, this is not supported."
    }

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {

        val textFieldPositions = mutableStateMapOf<String, LayoutCoordinates>()

        ModalBottomSheetScaffold(
            modifier = Modifier
                .clearFocusOutside(textFieldPositions)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    text = "Dive segment"
                )

                var cylinder: Cylinder by remember {
                    mutableStateOf(initialValue?.cylinder ?: cylinders.first())
                }

                var depth by remember {
                    mutableIntStateOf(initialValue?.depth ?: 10)
                }
                var time by remember {
                    mutableIntStateOf(initialValue?.duration ?: 15)
                }

                val errorMessageDepth = remember {
                    mutableStateOf<String?>(null)
                }
                val errorMessageTime = remember {
                    mutableStateOf<String?>(null)
                }

                val isDepthValid = remember { mutableStateOf(true) }
                val isTimeValid = remember { mutableStateOf(true) }

                GasPropertiesComponent(
                    modifier = Modifier.padding(vertical = 16.dp),
                    gas = cylinder.gas,
                    maxDensity = maxDensity,
                    maxPPO2 = maxPPO2,
                    maxPPO2Secondary = null,
                    environment = environment,
                    showTopRow = false,
                )

                DropDown(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Cylinder",
                    selectedValue = cylinder,
                    items = cylinders,
                    selectedText = {
                        it?.buildGasText() ?: AnnotatedString("")
                    },
                    dropdownRow = { _, gas ->
                        Text(
                            style = MaterialTheme.typography.bodyLarge,
                            text = gas.buildGasText()
                        )
                    },
                    onSelectionChanged = { _, gas ->
                        cylinder = gas
                    }
                )

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedNumberInputField(
                        modifier = Modifier.weight(1f)
                            .recordLayoutCoordinates("depth", textFieldPositions),
                        label = "Depth",
                        minValue = 1,
                        maxValue = 150,
                        visualTransformation = SuffixVisualTransformation(" m"),
                        initialValue = initialValue?.depth ?: 10,
                        errorMessage = errorMessageDepth,
                        isValid = isDepthValid,
                        onNumberChanged = {
                            if(it != null) {
                                depth = it
                            }
                        },
                        supportingText = null,
                    )
                    OutlinedNumberInputField(
                        modifier = Modifier.weight(1f)
                            .recordLayoutCoordinates("duration", textFieldPositions),
                        label = "Duration",
                        minValue = 1,
                        maxValue = 999,
                        visualTransformation = SuffixVisualTransformation(" min"),
                        initialValue = initialValue?.duration ?: 15,
                        errorMessage = errorMessageTime,
                        isValid = isTimeValid,
                        onNumberChanged = {
                            if(it != null) {
                                time = it
                            }
                        },
                        supportingText = null,
                    )
                }

                val gas = cylinder.gas

                var anyErrorMessage = errorMessageDepth.value ?: errorMessageTime.value

                if (anyErrorMessage == null && depth > gas.oxygenModRounded(maxPPO2, environment)) {
                    anyErrorMessage = "Warning: Depth exceeds oxygen MOD!"
                } else if (anyErrorMessage == null && depth > gas.densityModRounded(environment = environment)) {
                    anyErrorMessage = "Warning: Depth exceeds density MOD!"
                }

                val distance = (previousDepth - depth)
                val travelTime = configuration.travelTime(distance)
                val bottomTime = time - travelTime

                val travelTimeHint = pluralsStringBuilder(Res.string.segment_picker_travel_time_hint) {
                    pluralInt(Res.plurals.unit_minute, travelTime)
                    pluralInt(Res.plurals.unit_meter, depth)
                    pluralInt(Res.plurals.unit_minute, bottomTime)
                }

                Text(
                    textAlign = TextAlign.Center,
                    minLines = 1,
                    text = anyErrorMessage ?: travelTimeHint,
                    color = if(anyErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
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
                        enabled = isTimeValid.value && isDepthValid.value,
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(top = 16.dp),
                        onClick = {
                            onAddOrUpdateDiveSegment(
                                DiveProfileSection(
                                    duration = time,
                                    depth = depth,
                                    cylinder = cylinder
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
}

@Composable
private fun Cylinder.buildGasText() = buildAnnotatedString {
    withStyle(MaterialTheme.typography.bodyExtraLarge.toSpanStyle()) {
        append(gas.toString())
    }
    val liters = DecimalFormat.format(1, waterVolume)
    val pressure = DecimalFormat.format(0, pressure)
    append(" (${liters}l @ ${pressure}bar)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun SegmentPickerBottomSheetPreview() {
    SegmentPickerBottomSheet(
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded
        ),
        maxDensity = Gas.MAX_GAS_DENSITY,
        maxPPO2 = 1.4,
        previousDepth = 0.0,
        configuration = Configuration(),
        environment = Environment.Default,
        initialValue = DiveProfileSection(10, 15, Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0)),
        cylinders = listOf(
            Cylinder(gas = Gas.Air, pressure = 232.0, waterVolume = 12.0),
            Cylinder(gas = Gas.Nitrox50, pressure = 207.0, waterVolume = Cylinder.AL80_WATER_VOLUME),
            Cylinder(gas =Gas.Nitrox80, pressure = 207.0, waterVolume = Cylinder.AL63_WATER_VOLUME)
        )
    )
}
