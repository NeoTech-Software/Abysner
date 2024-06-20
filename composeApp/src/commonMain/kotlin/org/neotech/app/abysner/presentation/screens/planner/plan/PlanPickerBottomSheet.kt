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

package org.neotech.app.abysner.presentation.screens.planner.plan

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.presentation.component.GasPickerComponent
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.component.clearFocusOutside
import org.neotech.app.abysner.presentation.component.modifier.ifTrue
import org.neotech.app.abysner.presentation.component.modifier.invisible
import org.neotech.app.abysner.presentation.component.recordLayoutCoordinates
import org.neotech.app.abysner.presentation.component.textfield.OutlinedNumberInputField
import org.neotech.app.abysner.presentation.component.textfield.SuffixVisualTransformation
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPickerBottomSheet(
    isAdd: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialValue: DiveProfileSection,
    maxPPO2: Double,
    maxDensity: Double,
    environment: Environment,
    onAddDiveSegment: (gas: DiveProfileSection) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {

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
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    text = "Dive segment"
                )

                var depth by remember {
                    mutableIntStateOf(initialValue.depth)
                }
                var time by remember {
                    mutableIntStateOf(initialValue.duration)
                }

                val errorMessageDepth = remember {
                    mutableStateOf<String?>(null)
                }
                val errorMessageTime = remember {
                    mutableStateOf<String?>(null)
                }

                val isDepthValid = remember { mutableStateOf(true) }
                val isTimeValid = remember { mutableStateOf(true) }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedNumberInputField(
                        modifier = Modifier.weight(1f)
                            .recordLayoutCoordinates("depth", textFieldPositions),
                        label = "Depth",
                        minValue = 1,
                        maxValue = 150,
                        visualTransformation = SuffixVisualTransformation(" m"),
                        initialValue = depth,
                        errorMessage = errorMessageDepth,
                        isValid = isDepthValid,
                        onNumberChanged = {
                            depth = it
                        },
                        supportingText = null,
                    )
                    OutlinedNumberInputField(
                        modifier = Modifier.weight(1f)
                            .recordLayoutCoordinates("time", textFieldPositions),
                        label = "Time",
                        minValue = 1,
                        maxValue = 999,
                        visualTransformation = SuffixVisualTransformation(" min"),
                        initialValue = time,
                        errorMessage = errorMessageTime,
                        isValid = isTimeValid,
                        onNumberChanged = {
                            time = it
                        },
                        supportingText = null,
                    )
                }

                var oxygenPercentage: Int by remember {
                    mutableIntStateOf((initialValue.gas.oxygenFraction * 100).roundToInt())
                }

                var heliumPercentage: Int by remember {
                    mutableIntStateOf((initialValue.gas.heliumFraction * 100).roundToInt())
                }

                val gas = Gas(oxygenPercentage / 100.0, heliumPercentage / 100.0)

                var anyErrorMessage = errorMessageDepth.value ?: errorMessageTime.value

                if(anyErrorMessage == null && depth > gas.oxygenModRounded(maxPPO2, environment)) {
                    anyErrorMessage = "Warning: Depth exceeds oxygen MOD!"
                } else if(anyErrorMessage == null && depth > gas.densityModRounded(environment = environment)) {
                    anyErrorMessage = "Warning: Depth exceeds density MOD!"
                }

                Text(
                    modifier = Modifier.ifTrue(anyErrorMessage == null) {
                        invisible()
                    },
                    textAlign = TextAlign.Center,
                    minLines = 1,
                    maxLines = 1,
                    text = anyErrorMessage ?: "Dummy to avoid jumping",
                    color = MaterialTheme.colorScheme.error
                )

                GasPropertiesComponent(
                    modifier = Modifier.padding(vertical = 16.dp),
                    gas = gas,
                    maxDensity = maxDensity,
                    maxPPO2 = maxPPO2,
                    environment = environment,
                )

                GasPickerComponent(
                    initialHeliumPercentage = heliumPercentage,
                    initialOxygenPercentage = oxygenPercentage,
                    onGasChanged = { oxygen, helium ->
                        oxygenPercentage = oxygen
                        heliumPercentage = helium
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
                            onAddDiveSegment(
                                DiveProfileSection(
                                    duration = time,
                                    depth = depth,
                                    gas = Gas(
                                        oxygenFraction = oxygenPercentage / 100.0,
                                        heliumFraction = heliumPercentage / 100.0
                                    )
                                )
                            )
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        }) {
                        Text(if(isAdd) { "Add" } else { "Save"})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun PlanPickerBottomSheetPreview() {
    PlanPickerBottomSheet(
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded
        ),
        maxDensity = Gas.MAX_GAS_DENSITY,
        maxPPO2 = 1.4,
        environment = Environment.Default,
        initialValue = DiveProfileSection(10, 15, Gas.Air)
    )
}
