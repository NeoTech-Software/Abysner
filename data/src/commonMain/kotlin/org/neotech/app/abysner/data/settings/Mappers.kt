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

package org.neotech.app.abysner.data.settings

import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.GasBlendingResource
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.GasPriceResource
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.GasResource
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.ThemeModeResource
import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1.UnitSystemResource
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.settings.model.GasBlendingSettings
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.settings.model.ThemeMode

fun SettingsModel.toResource() = SettingsResourceV1(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted,
    themeMode = themeMode.toResource(),
    showDiveEditTooltip = showDiveEditTooltip,
    unitSystem = unitSystem.toResource(),
    gasBlending = gasBlending.toResource(),
)

fun SettingsResourceV1.toModel(defaultUnitSystem: UnitSystem) = SettingsModel(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted,
    themeMode = themeMode.toModel(),
    showDiveEditTooltip = showDiveEditTooltip,
    unitSystem = unitSystem?.toModel() ?: defaultUnitSystem,
    gasBlending = gasBlending.toModel(),
)

private fun GasBlendingSettings.toResource() = GasBlendingResource(
    trimixMode = trimixMode,
    cylinderWaterVolume = cylinderSize.waterVolume,
    cylinderWorkingPressure = cylinderSize.workingPressure,
    startGas = startGas.toResource(),
    startPressure = startPressure,
    targetGas = targetGas.toResource(),
    targetPressure = targetPressure,
    firstTopUpGas = firstTopUpGas.toResource(),
    secondTopUpGas = secondTopUpGas.toResource(),
    finalTopUpGas = finalTopUpGas.toResource(),
    gasPrices = gasPrices.map { (gas, pricePerLiter) ->
        GasPriceResource(gas = gas.toResource(), pricePerLiter = pricePerLiter)
    },
)

private fun GasBlendingResource.toModel(): GasBlendingSettings {
    val defaults = GasBlendingSettings()
    return GasBlendingSettings(
        trimixMode = trimixMode,
        // Resolving against the standard sizes restores the preset name, so the cylinder picker
        // shows the right preset selected rather than falling back to a custom size.
        cylinderSize = Cylinder.Size.findMatching(cylinderWaterVolume, cylinderWorkingPressure)
            ?: Cylinder.Size(
                waterVolume = cylinderWaterVolume,
                workingPressure = cylinderWorkingPressure,
            ),
        startGas = startGas.toModel() ?: defaults.startGas,
        startPressure = startPressure,
        targetGas = targetGas.toModel() ?: defaults.targetGas,
        targetPressure = targetPressure,
        firstTopUpGas = firstTopUpGas.toModel() ?: defaults.firstTopUpGas,
        secondTopUpGas = secondTopUpGas.toModel() ?: defaults.secondTopUpGas,
        finalTopUpGas = finalTopUpGas.toModel() ?: defaults.finalTopUpGas,
        gasPrices = gasPrices.mapNotNull { resource ->
            resource.gas.toModel()?.let { it to resource.pricePerLiter }
        }.toMap(),
    )
}

private fun Gas.toResource() = GasResource(
    oxygenPercentage = oxygenPercentage,
    heliumPercentage = heliumPercentage,
)

/**
 * Returns null for anything that is not a valid gas, so a corrupt or hand-edited preference file
 * cannot stop the app from loading its settings. [Gas] itself rejects such values by throwing.
 */
private fun GasResource.toModel(): Gas? =
    if (oxygenPercentage < 0 || heliumPercentage < 0 || oxygenPercentage + heliumPercentage > 100) {
        null
    } else {
        Gas(
            oxygenFraction = oxygenPercentage / 100.0,
            heliumFraction = heliumPercentage / 100.0,
        )
    }

private fun ThemeMode.toResource() = when (this) {
    ThemeMode.SYSTEM -> ThemeModeResource.SYSTEM
    ThemeMode.LIGHT -> ThemeModeResource.LIGHT
    ThemeMode.DARK -> ThemeModeResource.DARK
}

private fun ThemeModeResource.toModel() = when (this) {
    ThemeModeResource.SYSTEM -> ThemeMode.SYSTEM
    ThemeModeResource.LIGHT -> ThemeMode.LIGHT
    ThemeModeResource.DARK -> ThemeMode.DARK
}

private fun UnitSystem.toResource() = when (this) {
    UnitSystem.METRIC -> UnitSystemResource.METRIC
    UnitSystem.IMPERIAL -> UnitSystemResource.IMPERIAL
}

private fun UnitSystemResource.toModel() = when (this) {
    UnitSystemResource.METRIC -> UnitSystem.METRIC
    UnitSystemResource.IMPERIAL -> UnitSystem.IMPERIAL
}

