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

package org.neotech.app.abysner.presentation.screens.planner.gas

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.presentation.component.GasPickerComponent
import org.neotech.app.abysner.presentation.component.GasPropertiesComponent
import org.neotech.app.abysner.presentation.component.bottomsheet.ModalBottomSheetScaffold
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasPickerBottomSheet(
    environment: Environment,
    maxPPO2: Double,
    sheetState: SheetState = rememberStandardBottomSheetState(),
    onAddGas: (gas: Gas) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        GasPickerBottomSheetContent(
            environment = environment,
            maxPPO2 = maxPPO2,
            sheetState = sheetState,
            onAddGas = onAddGas,
            onDismissRequest = onDismissRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GasPickerBottomSheetContent(
    environment: Environment,
    maxPPO2: Double,
    sheetState: SheetState = rememberStandardBottomSheetState(),
    onAddGas: (gas: Gas) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {

    val scope = rememberCoroutineScope()


    var oxygenPercentage: Int by remember {
        mutableIntStateOf(21)
    }

    var heliumPercentage: Int by remember {
        mutableIntStateOf(0)
    }

    ModalBottomSheetScaffold {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                text = "Gas"
            )

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
                initialOxygenPercentage = 21,
                initialHeliumPercentage = 0,
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
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(top = 16.dp),
                    onClick = {
                        onAddGas(
                            Gas(
                                oxygenFraction = oxygenPercentage / 100.0,
                                heliumFraction = heliumPercentage / 100.0
                            )
                        )
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }) {
                    Text("Add")
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
        GasPickerBottomSheetContent(
            environment = Environment.Default,
            maxPPO2 = 1.4,
            sheetState = rememberStandardBottomSheetState(
                SheetValue.Expanded
            )
        )
    }
}
