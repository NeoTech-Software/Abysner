/*
 * Abysner - Dive planner
 * Copyright (C) 2025-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogCustomContent
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.core.physics.GasEquationOfStateModel
import org.neotech.app.abysner.domain.core.physics.asBarToPsi
import org.neotech.app.abysner.domain.core.physics.asCubicFeetToLiters
import org.neotech.app.abysner.domain.core.physics.asLitersToCubicFeet
import org.neotech.app.abysner.domain.core.physics.asPsiToBar
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.list.LazyColumnWithScrollIndicators
import org.neotech.app.abysner.presentation.component.list.SelectionListDefaults
import org.neotech.app.abysner.presentation.component.textfield.number.NumberInputState
import org.neotech.app.abysner.presentation.component.textfield.number.rememberDecimalInputState
import org.neotech.app.abysner.presentation.component.textfield.OutlinedInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.textfield.defaultInputFieldLabel
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.pressureUnitLabel
import org.neotech.app.abysner.presentation.utilities.volumeUnitLabel
import kotlin.math.roundToInt

@Composable
fun CylinderSizeField(
    modifier: Modifier = Modifier,
    cylinderSize: Cylinder.Size,
    unitSystem: UnitSystem,
    onCylinderSizeSelected: (Cylinder.Size) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    val displayText = when (unitSystem) {
        UnitSystem.METRIC -> DecimalFormat.format(1, cylinderSize.waterVolume)
        UnitSystem.IMPERIAL -> cylinderSize.ratedCapacity().asLitersToCubicFeet().roundToInt().toString()
    }

    OutlinedTextField(
        modifier = modifier.clickable { showDialog = true },
        value = TextFieldValue(displayText),
        visualTransformation = SuffixVisualTransformation(" ${unitSystem.volumeUnitLabel}"),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        singleLine = true,
        label = defaultInputFieldLabel("Volume"),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            // This changes the disable to look like enabled, because we abuse this field a little
            // bit, we don't want it to have a cursor or respond to keyboard input, but it must
            // look enabled.
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    if (showDialog) {
        CylinderSizeDialog(
            currentCylinderSize = cylinderSize,
            unitSystem = unitSystem,
            onDismiss = { showDialog = false },
            onCylinderSizeSelected = { selectedSize ->
                showDialog = false
                onCylinderSizeSelected(selectedSize)
            },
        )
    }
}

@Composable
private fun CylinderSizeDialog(
    currentCylinderSize: Cylinder.Size,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onCylinderSizeSelected: (Cylinder.Size) -> Unit,
) {
    val matchedPreset = Cylinder.Size.findMatching(
        currentCylinderSize.waterVolume,
        currentCylinderSize.workingPressure
    )
    val initialTab = if (matchedPreset != null) {
        TAB_PRESETS
    } else {
        TAB_CUSTOM
    }
    val pagerState = rememberPagerState(initialPage = initialTab) { 2 }
    val coroutineScope = rememberCoroutineScope()

    val volumeRange = when (unitSystem) {
        UnitSystem.METRIC -> 0.1 to 50.0
        UnitSystem.IMPERIAL -> 6.0 to 160.0
    }
    val volumeFractionDigits = when (unitSystem) {
        UnitSystem.METRIC -> 1
        UnitSystem.IMPERIAL -> 0
    }
    val pressureRange = when (unitSystem) {
        UnitSystem.METRIC -> 10.0 to 300.0
        UnitSystem.IMPERIAL -> 1800.0 to 4500.0
    }
    val volumeInput = rememberDecimalInputState(
        initialValue = when (unitSystem) {
            UnitSystem.METRIC -> currentCylinderSize.waterVolume
            UnitSystem.IMPERIAL -> currentCylinderSize.ratedCapacity().asLitersToCubicFeet()
        },
        fractionDigits = volumeFractionDigits,
        minValue = volumeRange.first,
        maxValue = volumeRange.second,
    )
    val pressureInput = rememberDecimalInputState(
        initialValue = when (unitSystem) {
            UnitSystem.METRIC -> currentCylinderSize.workingPressure
            UnitSystem.IMPERIAL -> currentCylinderSize.workingPressure.asBarToPsi()
        },
        fractionDigits = 0,
        minValue = pressureRange.first,
        maxValue = pressureRange.second,
    )

    AlertDialogCustomContent(
        onDismissRequest = onDismiss,
        title = { Text("Cylinder size") },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            if (pagerState.currentPage == TAB_CUSTOM) {
                TextButton(
                    enabled = volumeInput.isValid && pressureInput.isValid,
                    onClick = {
                        val customVolume = volumeInput.value
                        val customPressure = pressureInput.value
                        val size = when (unitSystem) {
                            UnitSystem.METRIC -> Cylinder.Size(
                                waterVolume = customVolume,
                                workingPressure = customPressure,
                            )
                            UnitSystem.IMPERIAL -> {
                                val pressureBar = customPressure.asPsiToBar()
                                val targetCapacityLiters = customVolume.asCubicFeetToLiters()
                                val gasVolumePerLiter = GasEquationOfStateModel.Default.getGasVolume(Gas.Air, 1.0, pressureBar)
                                Cylinder.Size(
                                    waterVolume = targetCapacityLiters / gasVolumePerLiter,
                                    workingPressure = pressureBar,
                                )
                            }
                        }
                        onCylinderSizeSelected(size)
                    }
                ) {
                    Text("OK")
                }
            }
        },
        content = {
            Column(modifier = Modifier.heightIn(max = SelectionListDefaults.dialogContentMaxHeight)) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Tab(
                        selected = pagerState.currentPage == TAB_PRESETS,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(TAB_PRESETS)
                            }
                        },
                        text = { Text("Presets") },
                    )
                    Tab(
                        selected = pagerState.currentPage == TAB_CUSTOM,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(TAB_CUSTOM)
                            }
                        },
                        text = { Text("Custom") },
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { page ->
                    when (page) {
                        TAB_PRESETS -> {
                            PresetsTankSizeTab(
                                currentCylinderSize = currentCylinderSize,
                                unitSystem = unitSystem,
                                onPresetSelected = { preset ->
                                    onCylinderSizeSelected(preset)
                                },
                            )
                        }

                        TAB_CUSTOM -> {
                            CustomTankSizeTab(
                                unitSystem = unitSystem,
                                volumeInput = volumeInput,
                                pressureInput = pressureInput,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PresetsTankSizeTab(
    currentCylinderSize: Cylinder.Size,
    unitSystem: UnitSystem,
    onPresetSelected: (Cylinder.Size) -> Unit,
) {
    val presets = when (unitSystem) {
        UnitSystem.METRIC -> Cylinder.StandardMetricSizes + Cylinder.StandardImperialSizes
        UnitSystem.IMPERIAL -> Cylinder.StandardImperialSizes + Cylinder.StandardMetricSizes
    }
    val selectedPreset = Cylinder.Size.findMatching(
        currentCylinderSize.waterVolume,
        currentCylinderSize.workingPressure
    )
    val selectedIndex = selectedPreset?.let { presets.indexOf(it) } ?: -1
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LazyColumnWithScrollIndicators(state = listState) {
        items(presets) { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SelectionListDefaults.rowMinHeight)
                    .clickable { onPresetSelected(preset) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    RadioButton(
                        selected = preset == selectedPreset,
                        onClick = { onPresetSelected(preset) },
                    )
                }
                Text(
                    text = preset.name ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                InfoPill(
                    label = when (unitSystem) {
                        UnitSystem.METRIC -> null
                        UnitSystem.IMPERIAL -> {
                            val pressurePsi = preset.workingPressure.asBarToPsi().roundToInt()
                            "$pressurePsi ${unitSystem.pressureUnitLabel}"
                        }
                    },
                    value = when (unitSystem) {
                        UnitSystem.METRIC -> "${DecimalFormat.format(1, preset.waterVolume)} ${unitSystem.volumeUnitLabel}"
                        UnitSystem.IMPERIAL -> {
                            val capacityCuFt = preset.ratedCapacity().asLitersToCubicFeet().roundToInt()
                            "$capacityCuFt ${unitSystem.volumeUnitLabel}"
                        }
                    },
                    size = InfoPillSize.SMALL,
                )
            }
        }
    }
}

@Composable
private fun CustomTankSizeTab(
    unitSystem: UnitSystem,
    volumeInput: NumberInputState<Double>,
    pressureInput: NumberInputState<Double>,
) {
    val volumeLabel = when (unitSystem) {
        UnitSystem.METRIC -> "Volume"
        UnitSystem.IMPERIAL -> "True capacity"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedInputField(
            modifier = Modifier.fillMaxWidth(),
            state = volumeInput,
            label = volumeLabel,
            visualTransformation = SuffixVisualTransformation(" ${unitSystem.volumeUnitLabel}"),
            supportingText = null,
        )
        OutlinedInputField(
            modifier = Modifier.fillMaxWidth(),
            state = pressureInput,
            label = "Working pressure",
            visualTransformation = SuffixVisualTransformation(" ${unitSystem.pressureUnitLabel}"),
            supportingText = null,
        )
        if (unitSystem == UnitSystem.IMPERIAL) {
            Text(
                text = "Use real-gas capacity from the manufacturer's specifications sheet. This may differ from the marketing name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val TAB_PRESETS = 0
private const val TAB_CUSTOM = 1

@Preview
@Composable
fun CylinderSizeDialogPresetsPreview() {
    AbysnerTheme {
        CylinderSizeDialog(
            currentCylinderSize = Cylinder.STEEL_12L,
            unitSystem = UnitSystem.METRIC,
            onDismiss = {},
            onCylinderSizeSelected = {},
        )
    }
}

@Preview
@Composable
fun CylinderSizeDialogCustomPreview() {
    AbysnerTheme {
        CylinderSizeDialog(
            currentCylinderSize = Cylinder.Size(
                waterVolume = 9.0,
                workingPressure = 232.0,
            ),
            unitSystem = UnitSystem.METRIC,
            onDismiss = {},
            onCylinderSizeSelected = {},
        )
    }
}

@Preview
@Composable
fun CylinderSizeDialogImperialPresetsPreview() {
    AbysnerTheme {
        CylinderSizeDialog(
            currentCylinderSize = Cylinder.AL80,
            unitSystem = UnitSystem.IMPERIAL,
            onDismiss = {},
            onCylinderSizeSelected = {},
        )
    }
}

@Preview
@Composable
fun CylinderSizeDialogImperialCustomPreview() {
    AbysnerTheme {
        CylinderSizeDialog(
            currentCylinderSize = Cylinder.Size(
                waterVolume = 9.0,
                workingPressure = 232.0,
            ),
            unitSystem = UnitSystem.IMPERIAL,
            onDismiss = {},
            onCylinderSizeSelected = {},
        )
    }
}
