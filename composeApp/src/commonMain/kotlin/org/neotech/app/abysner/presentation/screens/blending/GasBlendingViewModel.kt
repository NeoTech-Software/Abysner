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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.gasblending.GasBlendingPlanner
import org.neotech.app.abysner.domain.gasblending.model.GasBlendingPlan
import org.neotech.app.abysner.domain.gasblending.toBlendingInput
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import org.neotech.app.abysner.domain.settings.model.SettingsModel

enum class BlendingMode(val humanReadableName: String) {
    NITROX("Nitrox"),
    TRIMIX("Trimix"),
}

/**
 * Which top-up gas slot a [GasBlendingViewModel.setTopUpGas] call refers to. Slots are filled in
 * this order, and [SECOND] is only used when blending trimix.
 */
enum class TopUpSlot {
    FIRST,
    SECOND,
    FINAL,
}

@Inject
class GasBlendingViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val planner = GasBlendingPlanner()

    // Every input lives in settings rather than in this view model, so the screen comes back the
    // way it was left even after the app is closed. updateSettings applies synchronously to the
    // in-memory settings and persists in the background, so the UI still reacts immediately.
    val uiState: StateFlow<UiState> = settingsRepository.settings
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = settingsRepository.settings.value.toUiState(),
        )

    private fun SettingsModel.toUiState() = UiState(
        blending = gasBlending,
        // Solving is two small linear systems plus a handful of Newton iterations, so unlike the
        // dive planner there is no need to move this off the main dispatcher or show a loading
        // state.
        plan = planner.plan(gasBlending.toBlendingInput()),
        unitSystem = unitSystem,
    )

    private fun update(block: (GasBlendingSettings) -> GasBlendingSettings) {
        settingsRepository.updateSettings { it.copy(gasBlending = block(it.gasBlending)) }
    }

    fun setMode(mode: BlendingMode) = update { it.copy(trimixMode = mode == BlendingMode.TRIMIX) }

    fun setCylinderSize(cylinderSize: Cylinder.Size) = update { it.copy(cylinderSize = cylinderSize) }

    fun setStartPressure(pressure: Double) = update { it.copy(startPressure = pressure) }

    fun setStartGas(gas: Gas) = update { it.copy(startGas = gas) }

    fun setTargetPressure(pressure: Double) = update { it.copy(targetPressure = pressure) }

    fun setTargetGas(gas: Gas) = update { it.copy(targetGas = gas) }

    fun setTopUpGas(slot: TopUpSlot, gas: Gas) = update {
        when (slot) {
            TopUpSlot.FIRST -> it.copy(firstTopUpGas = gas)
            TopUpSlot.SECOND -> it.copy(secondTopUpGas = gas)
            TopUpSlot.FINAL -> it.copy(finalTopUpGas = gas)
        }
    }

    /**
     * Stores what a liter of [gas] at 1 bar costs. A null [pricePerLiter] forgets the price again.
     */
    fun setGasPrice(gas: Gas, pricePerLiter: Double?) = update { blending ->
        val prices = blending.gasPrices.toMutableMap()
        if (pricePerLiter == null) {
            prices.remove(gas)
        } else {
            prices[gas] = pricePerLiter
        }
        blending.copy(gasPrices = prices)
    }

    @Immutable
    data class UiState(
        val blending: GasBlendingSettings = GasBlendingSettings(),
        val plan: GasBlendingPlan = GasBlendingPlanner().plan(GasBlendingSettings().toBlendingInput()),
        val unitSystem: UnitSystem = UnitSystem.METRIC,
    ) {
        val mode: BlendingMode
            get() = if (blending.trimixMode) BlendingMode.TRIMIX else BlendingMode.NITROX
    }
}
