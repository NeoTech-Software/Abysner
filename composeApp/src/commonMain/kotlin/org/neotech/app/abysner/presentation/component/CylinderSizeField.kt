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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.MutableState
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
import org.neotech.app.abysner.domain.utilities.DecimalFormat
import org.neotech.app.abysner.presentation.component.list.LazyColumnWithScrollIndicators
import org.neotech.app.abysner.presentation.component.textfield.OutlinedDecimalInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import org.neotech.app.abysner.presentation.component.textfield.defaultInputFieldLabel
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@Composable
fun CylinderSizeField(
    modifier: Modifier = Modifier,
    cylinderSize: Cylinder.Size,
    onCylinderSizeSelected: (Cylinder.Size) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    val displayText = DecimalFormat.format(1, cylinderSize.waterVolume)

    OutlinedTextField(
        modifier = modifier.clickable { showDialog = true },
        value = TextFieldValue(displayText),
        visualTransformation = SuffixVisualTransformation(" L"),
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

    var customVolume: Double? by remember { mutableStateOf(currentCylinderSize.waterVolume) }
    var customPressure: Double? by remember { mutableStateOf(currentCylinderSize.workingPressure) }
    val isVolumeValid = remember { mutableStateOf(true) }
    val isPressureValid = remember { mutableStateOf(true) }

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
                    enabled = isVolumeValid.value && isPressureValid.value,
                    onClick = {
                        onCylinderSizeSelected(
                            Cylinder.Size(
                                waterVolume = customVolume!!,
                                workingPressure = customPressure!!,
                            )
                        )
                    }
                ) {
                    Text("OK")
                }
            }
        },
        content = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Tab(
                        selected = pagerState.currentPage == TAB_PRESETS,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    TAB_PRESETS
                                )
                            }
                        },
                        text = { Text("Presets") },
                    )
                    Tab(
                        selected = pagerState.currentPage == TAB_CUSTOM,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    TAB_CUSTOM
                                )
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
                                onPresetSelected = { preset ->
                                    onCylinderSizeSelected(preset)
                                },
                            )
                        }

                        TAB_CUSTOM -> {
                            CustomTankSizeTab(
                                volume = customVolume,
                                pressure = customPressure,
                                isVolumeValid = isVolumeValid,
                                isPressureValid = isPressureValid,
                                onVolumeChanged = { customVolume = it },
                                onPressureChanged = { customPressure = it },
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
    onPresetSelected: (Cylinder.Size) -> Unit,
) {
    val selectedPreset = Cylinder.Size.findMatching(
        currentCylinderSize.waterVolume,
        currentCylinderSize.workingPressure
    )
    val selectedIndex = selectedPreset?.let { Cylinder.StandardSizes.indexOf(it) } ?: -1
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LazyColumnWithScrollIndicators(state = listState) {
        items(Cylinder.StandardSizes) { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable { onPresetSelected(preset) }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
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
                    label = null,
                    value = "${DecimalFormat.format(1, preset.waterVolume)} L",
                    size = InfoPillSize.SMALL,
                )
            }
        }
    }
}

@Composable
private fun CustomTankSizeTab(
    volume: Double?,
    pressure: Double?,
    isVolumeValid: MutableState<Boolean>,
    isPressureValid: MutableState<Boolean>,
    onVolumeChanged: (Double?) -> Unit,
    onPressureChanged: (Double?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedDecimalInputField(
            modifier = Modifier.fillMaxWidth(),
            initialValue = volume,
            label = "Volume",
            fractionDigits = 1,
            minValue = 0.1,
            maxValue = 50.0,
            isValid = isVolumeValid,
            visualTransformation = SuffixVisualTransformation(" L"),
            onNumberChanged = onVolumeChanged,
            supportingText = null,
        )
        OutlinedDecimalInputField(
            modifier = Modifier.fillMaxWidth(),
            initialValue = pressure,
            label = "Working pressure",
            fractionDigits = 0,
            minValue = 10.0,
            maxValue = 300.0,
            isValid = isPressureValid,
            visualTransformation = SuffixVisualTransformation(" bar"),
            onNumberChanged = onPressureChanged,
            supportingText = null,
        )
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
                waterVolume = 11.1,
                workingPressure = 232.0,
            ),
            onDismiss = {},
            onCylinderSizeSelected = {},
        )
    }
}
