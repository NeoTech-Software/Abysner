/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.blending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.gasblending.GasBlendingPlanner
import org.neotech.app.abysner.domain.gasblending.toBlendingInput
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import org.neotech.app.abysner.presentation.component.appbar.DetailScreenTopAppBar
import org.neotech.app.abysner.presentation.theme.AbysnerTheme

/**
 * Callbacks the gas blending screen needs, bundled so the stateless composables stay previewable
 * without a view model.
 */
@Immutable
data class GasBlendingCallbacks(
    val onModeChanged: (BlendingMode) -> Unit = {},
    val onCylinderSizeChanged: (Cylinder.Size) -> Unit = {},
    val onStartPressureChanged: (Double) -> Unit = {},
    val onStartGasChanged: (Gas) -> Unit = {},
    val onTargetPressureChanged: (Double) -> Unit = {},
    val onTargetGasChanged: (Gas) -> Unit = {},
    val onTopUpGasChanged: (TopUpSlot, Gas) -> Unit = { _, _ -> },
    val onGasPriceChanged: (Gas, Double?) -> Unit = { _, _ -> },
)

// Metro supports @Inject on top-level functions, but the generated types are not resolved by the
// IDE, causing "Unresolved reference" errors. This wrapper class avoids those IDE errors.
// See: https://zacsweers.github.io/metro/latest/installation/#ide-support
@Inject
class GasBlendingScreen(
    private val viewModelCreator: () -> GasBlendingViewModel,
) {
    @Composable
    operator fun invoke(navController: NavHostController) {
        GasBlendingScreen(
            navController = navController,
            viewModel = viewModel { viewModelCreator() },
        )
    }
}

@Composable
private fun GasBlendingScreen(
    navController: NavHostController,
    viewModel: GasBlendingViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    GasBlendingScreen(
        navController = navController,
        uiState = uiState,
        callbacks = GasBlendingCallbacks(
            onModeChanged = viewModel::setMode,
            onCylinderSizeChanged = viewModel::setCylinderSize,
            onStartPressureChanged = viewModel::setStartPressure,
            onStartGasChanged = viewModel::setStartGas,
            onTargetPressureChanged = viewModel::setTargetPressure,
            onTargetGasChanged = viewModel::setTargetGas,
            onTopUpGasChanged = viewModel::setTopUpGas,
            onGasPriceChanged = viewModel::setGasPrice,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasBlendingScreen(
    navController: NavHostController = rememberNavController(),
    uiState: GasBlendingViewModel.UiState,
    callbacks: GasBlendingCallbacks = GasBlendingCallbacks(),
) {
    AbysnerTheme {
        Scaffold(
            topBar = {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                DetailScreenTopAppBar(
                    title = "Gas blending",
                    showBackButton = currentBackStackEntry != null || LocalInspectionMode.current,
                    onNavigateUp = { navController.navigateUp() },
                )
            }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GasBlendingInputCard(
                    modifier = Modifier.fillMaxWidth(),
                    blending = uiState.blending,
                    unitSystem = uiState.unitSystem,
                    callbacks = callbacks,
                )

                GasBlendingResultCard(
                    modifier = Modifier.fillMaxWidth(),
                    plan = uiState.plan,
                    unitSystem = uiState.unitSystem,
                )

                GasNeededCard(
                    modifier = Modifier.fillMaxWidth(),
                    plan = uiState.plan,
                    cylinderSize = uiState.blending.cylinderSize,
                    unitSystem = uiState.unitSystem,
                    gasPrices = uiState.blending.gasPrices,
                    onCylinderSizeChanged = callbacks.onCylinderSizeChanged,
                    onGasPriceChanged = callbacks.onGasPriceChanged,
                )

                Text(
                    text = "Amounts account for real gas behaviour, using the same polynomial fit " +
                            "as the rest of the app. The calculation assumes everything happens at " +
                            "room temperature, so fill slowly and let the cylinder cool down before " +
                            "topping up further. It also assumes the mix compresses as a whole, " +
                            "rather than each component separately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun previewState(
    mode: BlendingMode = BlendingMode.NITROX,
    startGas: Gas = Gas.Air,
    startPressure: Double = 50.0,
    targetGas: Gas = Gas.Nitrox32,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    gasPrices: Map<Gas, Double> = mapOf(Gas.Oxygen to 0.008, Gas.Helium to 0.045, Gas.Air to 0.001),
): GasBlendingViewModel.UiState {
    val blending = GasBlendingSettings(
        trimixMode = mode == BlendingMode.TRIMIX,
        startGas = startGas,
        startPressure = startPressure,
        targetGas = targetGas,
        gasPrices = gasPrices,
    )
    return GasBlendingViewModel.UiState(
        blending = blending,
        plan = GasBlendingPlanner().plan(blending.toBlendingInput()),
        unitSystem = unitSystem,
    )
}

@Preview
@Composable
fun GasBlendingScreenPreview() {
    GasBlendingScreen(uiState = previewState())
}

@Preview
@Composable
fun GasBlendingScreenTrimixPreview() {
    GasBlendingScreen(
        uiState = previewState(
            mode = BlendingMode.TRIMIX,
            startPressure = 20.0,
            targetGas = Gas.Trimix1845,
        )
    )
}

@Preview
@Composable
fun GasBlendingScreenDrainPreview() {
    GasBlendingScreen(uiState = previewState(startGas = Gas.Nitrox50, startPressure = 100.0))
}

@Preview
@Composable
fun GasBlendingScreenImperialPreview() {
    GasBlendingScreen(uiState = previewState(unitSystem = UnitSystem.IMPERIAL))
}
