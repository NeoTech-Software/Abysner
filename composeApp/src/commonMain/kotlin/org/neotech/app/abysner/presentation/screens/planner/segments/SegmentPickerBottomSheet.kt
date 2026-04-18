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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.CylinderRole
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.bailoutCylinders
import org.neotech.app.abysner.domain.diveplanning.model.ccrDiluentCylinder
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBar
import org.neotech.app.abysner.domain.core.physics.partialPressure
import org.neotech.app.abysner.presentation.component.DropDown
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.BottomSheetButtonRow
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.clearFocusOutside
import org.neotech.app.abysner.presentation.component.recordLayoutCoordinates
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.core.pluralsStringBuilder
import org.neotech.app.abysner.presentation.theme.bodyExtraLarge
import org.neotech.app.abysner.presentation.utilities.ModalTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SegmentPickerBottomSheetHost(
    show: ModalTarget<Int>?,
    configuration: Configuration,
    segments: List<DiveProfileSection>,
    cylinders: ImmutableList<PlannedCylinderModel>,
    diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    onDismiss: () -> Unit,
    onAddSegment: (DiveProfileSection) -> Unit,
    onUpdateSegment: (Int, DiveProfileSection) -> Unit,
) {
    if (show != null) {
        val editIndex = (show as? ModalTarget.Edit)?.value
        val initial = editIndex?.let { segments[it] }
        val previousIndex = (editIndex ?: segments.size) - 1
        SegmentPickerBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            isAdd = initial == null,
            initialValue = initial,
            maxPPO2 = configuration.maxPPO2,
            maxDensity = Gas.MAX_GAS_DENSITY,
            environment = configuration.environment,
            cylinders = cylinders,
            diveMode = diveMode,
            previousDepth = segments.getOrNull(previousIndex)?.depth?.toDouble() ?: 0.0,
            configuration = configuration,
            onAddOrUpdateDiveSegment = {
                if (editIndex != null) {
                    onUpdateSegment(editIndex, it)
                } else {
                    onAddSegment(it)
                }
            },
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentPickerBottomSheet(
    isAdd: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialValue: DiveProfileSection?,
    maxPPO2: Double,
    maxDensity: Double,
    environment: Environment,
    cylinders: ImmutableList<PlannedCylinderModel>,
    diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
    previousDepth: Double,
    configuration: Configuration,
    onAddOrUpdateDiveSegment: (gas: DiveProfileSection) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    require(cylinders.isNotEmpty()) {
        "SegmentPickerBottomSheet was shown with an empty list of cylinders, this is not supported."
    }

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
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

                val availableCylinders = remember(cylinders) {
                    cylinders
                        .filter { !it.isCcrOxygen }
                        .map { it.cylinder }
                        .distinctBy { it.gas }
                        .sortedBy { it.gas.oxygenFraction }
                        .toImmutableList()
                }

                // Match by gas instead of identity: availableCylinders is deduplicated, so the
                // exact cylinder object from initialValue may differ.
                val initialCylinder = initialValue?.cylinder?.gas?.let { gas ->
                    availableCylinders.firstOrNull { it.gas == gas }
                        ?: error("Gas $gas from initialValue not found in availableCylinders.")
                }

                var selectedCylinder: Cylinder by remember { mutableStateOf(initialCylinder ?: availableCylinders.first()) }

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

                if (!diveMode.isCcr) {
                    GasPropertiesComponent(
                        modifier = Modifier.padding(vertical = 16.dp),
                        gas = selectedCylinder.gas,
                        maxDensity = maxDensity,
                        maxPPO2 = maxPPO2,
                        maxPPO2Secondary = null,
                        environment = environment,
                        showTopRow = false,
                    )

                    DropDown(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Gas",
                        selectedValue = selectedCylinder,
                        items = availableCylinders,
                        selectedText = {
                            it?.gas?.buildText() ?: AnnotatedString("")
                        },
                        dropdownRow = { _, cylinder ->
                            Text(
                                style = MaterialTheme.typography.bodyLarge,
                                text = cylinder.gas.buildText()
                            )
                        },
                        onSelectionChanged = { _, cylinder ->
                            selectedCylinder = cylinder
                        }
                    )
                } else {
                    val diluentGas = cylinders.ccrDiluentCylinder()?.cylinder?.gas
                    if (diluentGas != null) {
                        CcrLoopPropertiesComponent(
                            modifier = Modifier.padding(top = 16.dp),
                            depth = depth,
                            setpoint = configuration.ccrHighSetpoint,
                            diluent = diluentGas,
                            environment = environment,
                        )
                    }
                }

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

                val gas = selectedCylinder.gas

                // Collect input error messages (invalid number, no number etc.)
                var anyErrorMessage = errorMessageDepth.value ?: errorMessageTime.value

                // OC-specific warnings: MOD and density limits apply to the breathed gas directly.
                // On a rebreather the setpoint controls O2, so these do not apply.
                if (!diveMode.isCcr) {
                    if (anyErrorMessage == null && depth > gas.oxygenModRounded(maxPPO2, environment)) {
                        anyErrorMessage = "Warning: Depth exceeds oxygen MOD!"
                    } else if (anyErrorMessage == null && depth > gas.densityModRounded(environment = environment)) {
                        anyErrorMessage = "Warning: Depth exceeds density MOD!"
                    }
                } else if (anyErrorMessage == null) {
                    val diluentGas = cylinders.ccrDiluentCylinder()?.cylinder?.gas
                    if (diluentGas != null) {
                        val ambientPressure = depthInMetersToBar(depth.toDouble(), environment).value
                        val diluentPpO2 = partialPressure(ambientPressure, diluentGas.oxygenFraction)
                        if (diluentPpO2 > configuration.ccrHighSetpoint) {
                            anyErrorMessage = "Warning: Diluent PPO2 exceeds setpoint at this depth!"
                        }
                    }

                    if (anyErrorMessage == null) {
                        val hasBailoutAtDepth = cylinders.bailoutCylinders().any {
                            depth <= it.cylinder.gas.oxygenModRounded(maxPPO2, environment)
                        }
                        if (!hasBailoutAtDepth) {
                            anyErrorMessage = "Warning: No bailout gas within MOD at this depth!"
                        }
                    }
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
                    minLines = 2,
                    text = anyErrorMessage ?: travelTimeHint,
                    color = if(anyErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    }
                )

                BottomSheetButtonRow(
                    modifier = Modifier.padding(top = 16.dp),
                    secondaryLabel = "Cancel",
                    primaryLabel = if (isAdd) { "Add" } else { "Update" },
                    primaryEnabled = isTimeValid.value && isDepthValid.value,
                    onSecondary = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    onPrimary = {
                        onAddOrUpdateDiveSegment(
                            DiveProfileSection(
                                duration = time,
                                depth = depth,
                                cylinder = selectedCylinder
                            )
                        )
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Gas.buildText(): AnnotatedString {
    val name = diveIndustryName()
    val mix = toString()
    return buildAnnotatedString {
        withStyle(MaterialTheme.typography.bodyExtraLarge.toSpanStyle()) {
            append(name)
        }
        append(" ($mix)")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun SegmentPickerBottomSheetPreview() {
    SegmentPickerBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        maxDensity = Gas.MAX_GAS_DENSITY,
        maxPPO2 = 1.4,
        previousDepth = 0.0,
        configuration = Configuration(),
        environment = Environment.Default,
        initialValue = DiveProfileSection(
            10,
            15,
            Cylinder.steel12Liter(gas = Gas.Air)
        ),
        cylinders = persistentListOf(
            PlannedCylinderModel(cylinder = Cylinder.steel12Liter(gas = Gas.Air), isChecked = true, isLocked = true),
            PlannedCylinderModel(cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50), isChecked = true, isLocked = false),
            PlannedCylinderModel(cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80), isChecked = false, isLocked = false),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun SegmentPickerBottomSheetCcrPreview() {
    SegmentPickerBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        maxDensity = Gas.MAX_GAS_DENSITY,
        maxPPO2 = 1.4,
        previousDepth = 0.0,
        configuration = Configuration(),
        environment = Environment.Default,
        diveMode = DiveMode.CLOSED_CIRCUIT,
        initialValue = DiveProfileSection(
            30,
            25,
            Cylinder.steel12Liter(gas = Gas.Air)
        ),
        cylinders = persistentListOf(
            PlannedCylinderModel(cylinder = Cylinder.steel3LiterOxygen(), isChecked = true, isLocked = true, role = CylinderRole.CCR_OXYGEN),
            PlannedCylinderModel(cylinder = Cylinder.steel12Liter(gas = Gas.Air), isChecked = true, isLocked = true, role = CylinderRole.CCR_DILUENT_AND_BAILOUT),
            PlannedCylinderModel(cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50), isChecked = true, isLocked = false),
        )
    )
}
